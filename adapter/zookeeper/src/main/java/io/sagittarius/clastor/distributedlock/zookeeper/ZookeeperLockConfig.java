package io.sagittarius.clastor.distributedlock.zookeeper;

import java.time.Duration;

public record ZookeeperLockConfig(
        String connectionString,
        Duration connectionTimeout,
        Duration sessionTimeout,
        String rootPath,
        Duration acquireRetryDelay
) {}
