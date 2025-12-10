package io.sagittarius.simplelock.zookeeper;

import io.sagittarius.simplelock.domain.DistributedLock;
import io.sagittarius.simplelock.domain.DistributedLockManager;
import io.sagittarius.simplelock.domain.DistributedLockVersionAccessor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;

@Slf4j
public class ZookeeperLockManager implements DistributedLockManager, AutoCloseable {

    private final ZookeeperLockConfig config;
    private final ZkClient client;
    private final DistributedLockVersionAccessor versionAccessor;

    public ZookeeperLockManager(ZookeeperLockConfig config) throws IOException, InterruptedException {
        this.config = config;
        this.client = new ZkClient(config.connectionString(), config.connectionTimeout(), config.sessionTimeout());

        this.versionAccessor = new ZookeeperLockVersionAccessor(
                client,
                config.rootPath(),
                "version",
                "v1"
        );
    }

    @Override
    public DistributedLock lock(String resourceKind, String resourceId) {
        String version = versionAccessor.currentVersion(resourceKind);
        String path = config.rootPath()
                + "/" + resourceKind
                + "/" + version
                + "/" + resourceId;

        Duration retryDelay = config.acquireRetryDelay();
        return new ZookeeperDistributedLock(client, path, retryDelay);
    }

    @Override
    public DistributedLockVersionAccessor versionAccessor() {
        return versionAccessor;
    }

    @Override
    public void close() throws Exception {
        client.close();
    }
}
