package io.sagittarius.clastor.distributedlock.zookeeper;

import io.sagittarius.clastor.distributedlock.domain.DistributedLock;
import lombok.RequiredArgsConstructor;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Queues contenders as ephemeral sequential nodes and watches each contender's predecessor.
 */
@RequiredArgsConstructor
public class ZookeeperDistributedLock implements DistributedLock {

    private final ZkClient client;
    private final String locksRootPath;
    private final Duration retryDelay;

    private String currentNodePath;

    @Override
    public synchronized boolean tryLock(Duration timeout) throws InterruptedException {
        if (timeout.isNegative()) {
            throw new IllegalArgumentException("Timeout must be non-negative");
        }
        if (currentNodePath != null) {
            throw new IllegalStateException("Release the current lock attempt before acquiring again");
        }

        long timeoutNanos = TimeUnit.MILLISECONDS.toNanos(timeout.toMillis());
        long start = System.nanoTime();
        boolean acquired;
        try {
            client.ensurePath(locksRootPath);
            currentNodePath = client.createEphemeralSequential(locksRootPath + "/lock-");
            acquired = awaitTurn(start, timeoutNanos);
        } catch (KeeperException e) {
            var failure = new IllegalStateException("Failed to acquire ZooKeeper lock", e);
            cleanupAfterFailure(failure);
            throw failure;
        } catch (InterruptedException | RuntimeException e) {
            cleanupAfterFailure(e);
            throw e;
        }

        if (!acquired) {
            unlock();
        }
        return acquired;
    }

    private boolean awaitTurn(long start, long timeoutNanos) throws KeeperException, InterruptedException {
        String nodeName = currentNodePath.substring(locksRootPath.length() + 1);
        // A zero timeout allows one check without waiting for a predecessor.
        do {
            var children = client.getSortedChildren(locksRootPath);
            int index = children.indexOf(nodeName);
            if (index < 0) {
                throw new IllegalStateException("ZooKeeper lock node disappeared: " + currentNodePath);
            }

            long remaining = timeoutNanos - (System.nanoTime() - start);
            if (index == 0) {
                return timeoutNanos == 0 || remaining > 0;
            }
            if (remaining <= 0) {
                return false;
            }

            String predecessor = locksRootPath + "/" + children.get(index - 1);
            CountDownLatch deleted = new CountDownLatch(1);
            Watcher watcher = event -> {
                if (event.getType() == Watcher.Event.EventType.NodeDeleted) {
                    deleted.countDown();
                }
            };
            if (client.getRaw().exists(predecessor, watcher) != null) {
                remaining = timeoutNanos - (System.nanoTime() - start);
                deleted.await(Math.min(remaining, retryDelay.toNanos()), TimeUnit.NANOSECONDS);
            }
        } while (System.nanoTime() - start < timeoutNanos);
        return false;
    }

    @Override
    public synchronized void unlock() {
        if (currentNodePath == null) {
            return;
        }
        try {
            client.getRaw().delete(currentNodePath, -1);
        } catch (KeeperException.NoNodeException alreadyGone) {
            // A previous deletion may have succeeded before its response was lost.
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while releasing ZooKeeper lock", e);
        } catch (KeeperException e) {
            throw new IllegalStateException("Failed to release ZooKeeper lock", e);
        }
        currentNodePath = null;
    }

    private void cleanupAfterFailure(Exception failure) {
        boolean interrupted = Thread.interrupted();
        try {
            unlock();
        } catch (RuntimeException cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
