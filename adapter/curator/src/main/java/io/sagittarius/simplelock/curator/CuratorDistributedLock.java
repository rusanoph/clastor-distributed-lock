package io.sagittarius.simplelock.curator;

import io.sagittarius.simplelock.domain.DistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CuratorDistributedLock implements DistributedLock {

    private final InterProcessMutex mutex;

    public CuratorDistributedLock(CuratorFramework client, String path) {
        this.mutex = new InterProcessMutex(client, path);
    }

    @Override
    public boolean tryLock(Duration timeout) throws InterruptedException {
        try {
            return mutex.acquire(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to acquire Curator lock", e);
        }
    }

    @Override
    public void unlock() {
        try {
            if (mutex.isAcquiredInThisProcess()) {
                mutex.release();
            }
        } catch (Exception e) {
            log.error("Failed to release Curator lock", e);
        }
    }
}
