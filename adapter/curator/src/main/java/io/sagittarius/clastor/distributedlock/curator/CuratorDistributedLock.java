package io.sagittarius.clastor.distributedlock.curator;

import io.sagittarius.clastor.distributedlock.domain.DistributedLock;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class CuratorDistributedLock implements DistributedLock {

    private final InterProcessMutex mutex;

    public CuratorDistributedLock(CuratorFramework client, String path) {
        this.mutex = new InterProcessMutex(client, path);
    }

    @Override
    public boolean tryLock(Duration timeout) throws InterruptedException {
        try {
            return mutex.acquire(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to acquire Curator lock", e);
        }
    }

    @Override
    public void unlock() {
        try {
            if (mutex.isAcquiredInThisProcess()) {
                mutex.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while releasing Curator lock", e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to release Curator lock", e);
        }
    }
}
