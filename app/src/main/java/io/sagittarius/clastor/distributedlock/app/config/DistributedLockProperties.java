package io.sagittarius.clastor.distributedlock.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "distributed-lock")
@EnableConfigurationProperties(DistributedLockProperties.class)
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
    ) {
        public ZookeeperDistributedLockProperties {
            connectionTimeout = connectionTimeout != null
                    ? connectionTimeout
                    : Duration.ofSeconds(10);

            sessionTimeout = sessionTimeout != null
                    ? sessionTimeout
                    : Duration.ofSeconds(15);

            rootPath = rootPath != null
                    ? rootPath
                    : "/locks";

            acquireRetryDelay = acquireRetryDelay != null
                    ? acquireRetryDelay
                    : Duration.ofMillis(200);
        }
    }

    public record CuratorDistributedLockProperties(
            String connectionString,
            Duration sessionTimeout,
            Duration connectionTimeout,
            String rootPath
    ) {
        public CuratorDistributedLockProperties {
            connectionTimeout = connectionTimeout != null
                    ? connectionTimeout
                    : Duration.ofSeconds(5);

            sessionTimeout = sessionTimeout != null
                    ? sessionTimeout
                    : Duration.ofSeconds(15);

            rootPath = rootPath != null
                    ? rootPath
                    : "/locks";
        }
    }
}
