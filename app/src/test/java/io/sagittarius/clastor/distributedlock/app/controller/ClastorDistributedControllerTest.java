package io.sagittarius.clastor.distributedlock.app.controller;

import io.sagittarius.clastor.distributedlock.app.model.DistributedLockStatus;
import io.sagittarius.clastor.distributedlock.domain.DistributedLock;
import io.sagittarius.clastor.distributedlock.domain.DistributedLockManager;
import io.sagittarius.clastor.distributedlock.domain.DistributedLockVersionAccessor;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClastorDistributedControllerTest {

    @Test
    void duplicateAcquireDoesNotReplaceOriginalHandle() throws InterruptedException {
        AtomicInteger created = new AtomicInteger();
        AtomicInteger unlocked = new AtomicInteger();
        var controller = controller(() -> {
            created.incrementAndGet();
            return lock(unlocked::incrementAndGet);
        });

        assertEquals(DistributedLockStatus.ACQUIRED, controller.acquire("jobs", "one", 5000).getBody().status());
        assertEquals(DistributedLockStatus.FAILED, controller.acquire("jobs", "one", 5000).getBody().status());
        assertEquals(1, created.get());
        assertEquals(DistributedLockStatus.RELEASED, controller.release("jobs", "one").getBody().status());
        assertEquals(1, unlocked.get());
    }

    @Test
    void releaseFailureKeepsHandleForRetry() throws InterruptedException {
        AtomicInteger attempts = new AtomicInteger();
        var controller = controller(() -> lock(() -> {
            if (attempts.incrementAndGet() == 1) {
                throw new IllegalStateException("Backend unavailable");
            }
        }));

        controller.acquire("jobs", "one", 5000);
        assertThrows(IllegalStateException.class, () -> controller.release("jobs", "one"));
        assertEquals(Set.of("jobs/one"), controller.heldLocks().getBody());
        assertEquals(DistributedLockStatus.RELEASED, controller.release("jobs", "one").getBody().status());
        assertEquals(Set.of(), controller.heldLocks().getBody());
        assertEquals(2, attempts.get());
    }

    private static ClastorDistributedController controller(Supplier<DistributedLock> locks) {
        return new ClastorDistributedController(new DistributedLockManager() {
            @Override
            public DistributedLock lock(String resourceKind, String resourceId) {
                return locks.get();
            }

            @Override
            public DistributedLockVersionAccessor versionAccessor() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void close() {}
        });
    }

    private static DistributedLock lock(Runnable unlock) {
        return new DistributedLock() {
            @Override
            public boolean tryLock(Duration timeout) {
                return true;
            }

            @Override
            public void unlock() {
                unlock.run();
            }
        };
    }
}
