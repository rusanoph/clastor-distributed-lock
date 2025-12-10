package io.sagittarius.simplelock.zookeeper;

import java.time.Duration;

public record ZookeeperLockConfig(
        String connectionString,
        Duration connectionTimeout,
        Duration sessionTimeout,
        String rootPath,
        Duration acquireRetryDelay
) {}
