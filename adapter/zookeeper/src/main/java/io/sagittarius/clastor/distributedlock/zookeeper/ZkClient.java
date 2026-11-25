package io.sagittarius.clastor.distributedlock.zookeeper;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ZkClient implements AutoCloseable {

    private final ZooKeeper zooKeeper;

    public ZkClient(String connectionString, Duration connectionTimeout, Duration sessionTimeout) throws IOException, InterruptedException {
        CountDownLatch connectedLatch = new CountDownLatch(1);
        int sessionTimeoutMs = Math.toIntExact(sessionTimeout.toMillis());
        int connectionTimeoutMs = Math.toIntExact(connectionTimeout.toMillis());
        this.zooKeeper = new ZooKeeper(connectionString, sessionTimeoutMs, watchedEvent -> {
            if (watchedEvent.getState() == Watcher.Event.KeeperState.SyncConnected) {
                connectedLatch.countDown();
            }
        });

        try {
            if (!connectedLatch.await(connectionTimeoutMs, TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException("ZooKeeper connection timed out after " + connectionTimeoutMs + " ms");
            }
        } catch (InterruptedException | RuntimeException failure) {
            try {
                zooKeeper.close();
            } catch (InterruptedException cleanupFailure) {
                Thread.currentThread().interrupt();
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    public ZooKeeper getRaw() {
        return zooKeeper;
    }

    public List<String> getSortedChildren(String path) throws InterruptedException, KeeperException {
        List<String> children = zooKeeper.getChildren(path, false);
        if (children.isEmpty()) {
            return List.of();
        }
        children.sort(Comparator.naturalOrder());
        return children;
    }

    public String createEphemeralSequential(String path) throws KeeperException, InterruptedException {
        return zooKeeper.create(path, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
    }

    public void ensurePath(String path) throws InterruptedException, KeeperException {
        String[] parts = path.split("/");
        StringBuilder current = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }

            current.append('/').append(part);
            String currentPath = current.toString();
            Stat stat = zooKeeper.exists(currentPath, false);

            if (stat == null) {
                try {
                    zooKeeper.create(currentPath,
                            new byte[0],
                            ZooDefs.Ids.OPEN_ACL_UNSAFE,
                            CreateMode.PERSISTENT
                    );
                } catch (KeeperException.NodeExistsException ignored) {}
            }
        }
    }

    @Override
    public void close() throws Exception {
        zooKeeper.close();
    }
}
