package io.sagittarius.clastor.distributedlock.curator;

import io.sagittarius.clastor.distributedlock.domain.DistributedLock;
import io.sagittarius.clastor.distributedlock.domain.DistributedLockManager;
import io.sagittarius.clastor.distributedlock.domain.DistributedLockVersionAccessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

@Slf4j
public class CuratorLockManager implements DistributedLockManager, AutoCloseable {

    private final CuratorLockConfig config;
    private final CuratorFramework client;
    private final DistributedLockVersionAccessor versionAccessor;

    public CuratorLockManager(CuratorLockConfig config) {
        this.config = config;
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);

        this.client = CuratorFrameworkFactory.builder()
                .connectString(config.connectString())
                .sessionTimeoutMs(Math.toIntExact(config.sessionTimeout().toMillis()))
                .connectionTimeoutMs(Math.toIntExact(config.connectionTimeout().toMillis()))
                .retryPolicy(retryPolicy)
                .build();

        this.client.start();

        this.versionAccessor = new CuratorLockVersionAccessor(
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

        return new CuratorDistributedLock(client, path);
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
