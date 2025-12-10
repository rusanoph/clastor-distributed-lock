package io.sagittarius.simplelock.zookeeper;

import io.sagittarius.simplelock.domain.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * The algorithm:
 * 1. A directory is created for the resource: /locks/<name>
 * 2. The client creates an ephemeral sequential node .../lock-0000001.
 * 3. Look at all children and sort.
 * 4. If we are first → we took a lock.
 * 5. Otherwise:
 *      - find the previous neighbor
 *      - set a watch on it
 *      - wait for it to be deleted
 *      - then check again to see if we are first.
 */
@Slf4j
@RequiredArgsConstructor
public class ZookeeperDistributedLock implements DistributedLock {

    private final ZkClient client;
    private final String locksRootPath;
    private final Duration retryDelay;

    private volatile String currentNodePath;
    private volatile boolean locked = false;

    @Override
    public boolean tryLock(Duration timeout) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeout.toMillis();

        try {
            ensureLockRoot();

            String nodePrefix = locksRootPath + "/lock-";
            currentNodePath = client.createEphemeralSequential(nodePrefix);

            while (true) {
                if (System.currentTimeMillis() > deadline) {
                    cleanupNodeQuietly();
                    return false;
                }

                List<String> children = client.getSortedChildren(locksRootPath);
                if (children.isEmpty()) {
                    // it shouldn't be like this, but just in case
                    log.error("Expected at least one child under path {}, but got empty list", locksRootPath);
                    throw new IllegalStateException("Empty children under " + locksRootPath);
                }

                String nodeName = currentNodePath.substring(locksRootPath.length() + 1);
                int index = children.indexOf(nodeName);
                if (index == -1) {
                    cleanupNodeQuietly();
                    currentNodePath = client.createEphemeralSequential(nodePrefix);
                    continue;
                }

                if (index == 0) {
                    locked = true;
                    return true;
                }

                String prevNode = children.get(index - 1);
                String prevPath = locksRootPath + "/" + prevNode;

                CountDownLatch latch = new CountDownLatch(1);
                Watcher watcher = event -> {
                    if (event.getType() == Watcher.Event.EventType.NodeDeleted) {
                        latch.countDown();
                    }
                };

                if (client.getRaw().exists(prevPath, watcher) == null) {
                    continue;
                }

                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    cleanupNodeQuietly();
                    return false;
                }

                latch.await(Math.min(remaining, retryDelay.toMillis()), TimeUnit.MILLISECONDS);
            }
        } catch (KeeperException e) {
            cleanupNodeQuietly();
            throw new RuntimeException("Error while acquiring ZooKeeper lock", e);
        }
    }

    @Override
    public void unlock() {
        if (!locked) {
            return;
        }

        try {
            if (currentNodePath != null) {
                client.getRaw().delete(currentNodePath, -1);
            }
        }  catch (KeeperException | InterruptedException e){
            Thread.currentThread().interrupt();
            log.error("Failed to release ZooKeeper lock", e);
        } finally {
            locked = false;
            currentNodePath = null;
        }
    }

    private void ensureLockRoot() throws KeeperException, InterruptedException {
        client.ensurePath(locksRootPath);
    }

    private void cleanupNodeQuietly() {
        try {
            if (currentNodePath != null) {
                client.getRaw().delete(currentNodePath, -1);
            }
        } catch (Exception ignored) {
        } finally {
            currentNodePath = null;
        }
    }
}
