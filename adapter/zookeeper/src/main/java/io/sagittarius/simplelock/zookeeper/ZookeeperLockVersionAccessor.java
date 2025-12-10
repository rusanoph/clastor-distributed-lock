package io.sagittarius.simplelock.zookeeper;

import io.sagittarius.simplelock.domain.DistributedLockVersionAccessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.nio.charset.StandardCharsets;

@Slf4j
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
        try {
            String resourceRoot = rootPath + "/" + resourceKind;
            client.ensurePath(resourceRoot);

            String vp = versionPath(resourceKind);
            ZooKeeper zk = client.getRaw();
            Stat stat = zk.exists(vp, false);

            if (stat == null) {
                log.info("Version node {} not found. Initializing with default version={}", vp, defaultVersion);
                zk.create(
                        vp,
                        defaultVersion.getBytes(StandardCharsets.UTF_8),
                        ZooDefs.Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT
                );
                return defaultVersion;
            }

            byte[] data = zk.getData(vp, false, stat);
            if (data == null || data.length == 0) {
                log.warn("Version node {} has empty data. Resetting to default version={}", vp, defaultVersion);
                zk.setData(vp, defaultVersion.getBytes(StandardCharsets.UTF_8), stat.getVersion());
                return defaultVersion;
            }

            return new String(data, StandardCharsets.UTF_8);
        } catch (KeeperException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Failed to read version from {}. Falling back to default version={}", versionPath(resourceKind), defaultVersion, e);
            return defaultVersion;
        }
    }
}