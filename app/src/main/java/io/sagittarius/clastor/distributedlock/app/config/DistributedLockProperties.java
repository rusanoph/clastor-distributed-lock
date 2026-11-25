package io.sagittarius.clastor.distributedlock.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "distributed-lock")
public record DistributedLockProperties(
        DistributedLockProvider provider,
        ZookeeperDistributedLockProperties zookeeper,
        CuratorDistributedLockProperties curator
) {
    enum DistributedLockProvider {
        ZOOKEEPER, CURATOR
    }

    public record ZookeeperDistributedLockProperties(
            String connectionString,
            Duration connectionTimeout,
            Duration sessionTimeout,
            String rootPath,
            Duration acquireRetryDelay
    ) {}

    public record CuratorDistributedLockProperties(
            String connectionString,
            Duration sessionTimeout,
            Duration connectionTimeout,
            String rootPath
    ) {}
}
