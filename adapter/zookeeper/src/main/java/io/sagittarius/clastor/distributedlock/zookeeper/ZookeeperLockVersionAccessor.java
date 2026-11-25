package io.sagittarius.clastor.distributedlock.zookeeper;

import io.sagittarius.clastor.distributedlock.domain.DistributedLockVersionAccessor;
import lombok.RequiredArgsConstructor;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.common.PathUtils;

import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
public class ZookeeperLockVersionAccessor implements DistributedLockVersionAccessor {

    private final ZkClient client;
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
        var zk = client.getRaw();
        try {
            byte[] data;
            try {
                data = zk.getData(path, false, null);
            } catch (KeeperException.NoNodeException missingVersion) {
                client.ensurePath(rootPath + "/" + resourceKind);
                try {
                    zk.create(path, defaultVersion.getBytes(StandardCharsets.UTF_8),
                            ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                } catch (KeeperException.NodeExistsException alreadyInitialized) {
                    // Another client initialized the version.
                }
                data = zk.getData(path, false, null);
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
        } catch (KeeperException | IllegalArgumentException e) {
            throw new IllegalStateException("Failed to read lock version at " + path, e);
        }
    }
}
