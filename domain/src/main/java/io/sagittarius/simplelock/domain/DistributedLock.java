package io.sagittarius.simplelock.domain;

import java.time.Duration;

/**
 * A distributed lock that can be used to synchronize access to resources
 * in a distributed system.
 */
public interface DistributedLock extends AutoCloseable {

    /**
     * Tries to acquire the lock within the given timeout.
     *
     * @param timeout the timeout
     * @return whether the lock was successfully acquired
     * @throws InterruptedException if the thread was interrupted
     */
    boolean tryLock(Duration timeout) throws InterruptedException;

    /**
     * Releases the lock.
     */
    void unlock();

    /**
     * Releases the lock when the lock is closed.
     */
    @Override
    default void close() {
        unlock();
    }
}