package io.sagittarius.clastor.distributedlock.curator;

import java.time.Duration;

public record CuratorLockConfig(
        String connectString,
        Duration connectionTimeout,
        Duration sessionTimeout,
        String rootPath
) {
}
