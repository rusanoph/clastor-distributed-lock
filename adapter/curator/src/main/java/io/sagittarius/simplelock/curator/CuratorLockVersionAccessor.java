package io.sagittarius.simplelock.curator;

import io.sagittarius.simplelock.domain.DistributedLockVersionAccessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;

import java.nio.charset.StandardCharsets;

@Slf4j
@RequiredArgsConstructor
public class CuratorLockVersionAccessor implements DistributedLockVersionAccessor {

    private final CuratorFramework client;
    private final String rootPath;
    private final String versionNodeName;
    private final String defaultVersion;

    @Override
    public String versionPath(String resourceKind) {
        return rootPath + "/" + resourceKind + "/" + versionNodeName;
    }

    @Override
    public String currentVersion(String resourceKind) {
        try {
            String resourceRoot = rootPath + "/" + resourceKind;
            client.create().creatingParentsIfNeeded().forPath(resourceRoot);

            String vp = versionPath(resourceKind);

            byte[] data;
            try {
                data = client.getData().forPath(vp);
            } catch (KeeperException.NoNodeException e) {
                log.info("Version node {} not found. Initializing with default version={}", vp, defaultVersion);
                client.create()
                        .creatingParentsIfNeeded()
                        .forPath(vp, defaultVersion.getBytes(StandardCharsets.UTF_8));
                return defaultVersion;
            }

            if (data == null || data.length == 0) {
                log.warn("Version node {} has empty data. Resetting to default version={}", vp, defaultVersion);
                client.setData().forPath(vp, defaultVersion.getBytes(StandardCharsets.UTF_8));
                return defaultVersion;
            }

            return new String(data, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Failed to read version from {}. Falling back to default version={}", versionPath(resourceKind), defaultVersion, e);
            return defaultVersion;
        }
    }
}
