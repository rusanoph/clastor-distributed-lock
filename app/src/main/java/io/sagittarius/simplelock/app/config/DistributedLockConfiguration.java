package io.sagittarius.simplelock.app.config;

import io.sagittarius.simplelock.curator.CuratorLockConfig;
import io.sagittarius.simplelock.curator.CuratorLockManager;
import io.sagittarius.simplelock.domain.DistributedLockManager;
import io.sagittarius.simplelock.zookeeper.ZookeeperLockConfig;
import io.sagittarius.simplelock.zookeeper.ZookeeperLockManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@EnableConfigurationProperties(DistributedLockProperties.class)
@RequiredArgsConstructor
public class DistributedLockConfiguration {

    private final DistributedLockProperties properties;

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public DistributedLockManager distributedLockManager() throws IOException, InterruptedException {
        return switch (properties.provider()) {
            case ZOOKEEPER -> zookeeperLockManager();
            case CURATOR -> curatorLockManager();
        };
    }

    private DistributedLockManager zookeeperLockManager() throws IOException, InterruptedException {
        var zookeeperProperties = properties.zookeeper();
        ZookeeperLockConfig config = new ZookeeperLockConfig(
                zookeeperProperties.connectionString(),
                zookeeperProperties.connectionTimeout(),
                zookeeperProperties.sessionTimeout(),
                zookeeperProperties.rootPath(),
                zookeeperProperties.acquireRetryDelay()
        );
        return new ZookeeperLockManager(config);
    }

    private DistributedLockManager curatorLockManager() {
        var curatorProperties = properties.curator();
        CuratorLockConfig config = new CuratorLockConfig(
                curatorProperties.connectionString(),
                curatorProperties.connectionTimeout(),
                curatorProperties.sessionTimeout(),
                curatorProperties.rootPath()
        );
        return new CuratorLockManager(config);
    }
}
