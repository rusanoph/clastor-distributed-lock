package io.sagittarius.clastor.distributedlock.curator;

import io.sagittarius.clastor.distributedlock.domain.DistributedLockVersionAccessor;
import lombok.RequiredArgsConstructor;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.common.PathUtils;

import java.nio.charset.StandardCharsets;

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
        String path = versionPath(resourceKind);
        try {
            byte[] data;
            try {
                data = client.getData().forPath(path);
            } catch (KeeperException.NoNodeException missingVersion) {
                try {
                    client.create().creatingParentsIfNeeded()
                            .forPath(path, defaultVersion.getBytes(StandardCharsets.UTF_8));
                } catch (KeeperException.NodeExistsException alreadyInitialized) {
                    // Another client initialized the version.
                }
                data = client.getData().forPath(path);
            }

            String version = data == null ? "" : new String(data, StandardCharsets.UTF_8);
            if (version.isBlank() || version.contains("/") || version.equals(versionNodeName)) {
                throw new IllegalArgumentException("Invalid lock version");
            }
            PathUtils.validatePath("/" + version);
            return version;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while reading lock version at " + path, e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read lock version at " + path, e);
        }
    }
}
