package io.sagittarius.simplelock.app.controller;

import io.sagittarius.simplelock.app.model.DistributedLockResponse;
import io.sagittarius.simplelock.app.model.DistributedLockStatus;
import io.sagittarius.simplelock.app.model.DistributedLockVersionInfo;
import io.sagittarius.simplelock.domain.DistributedLock;
import io.sagittarius.simplelock.domain.DistributedLockManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@RestController
@RequestMapping("api/v1/locks")
@RequiredArgsConstructor
public class SimpleLockController {

    private final DistributedLockManager distributedLockManager;
    private final ConcurrentMap<String, DistributedLock> heldLocks = new ConcurrentHashMap<>();

    @GetMapping("/held")
    public ResponseEntity<Set<String>> heldLocks() {
        return ResponseEntity.ok(heldLocks.keySet());
    }

    @GetMapping("/{resource}/version")
    public ResponseEntity<DistributedLockVersionInfo> getVersion(@PathVariable("resource") String resource) {
        var versionAccessor = distributedLockManager.versionAccessor();

        return ResponseEntity.ok(new DistributedLockVersionInfo(
                versionAccessor.versionPath(resource),
                versionAccessor.currentVersion(resource)
        ));
    }

    @PostMapping("/{resource}/acquire")
    public ResponseEntity<DistributedLockResponse> acquire(
            @PathVariable("resource") String resource,
            @RequestParam(name = "resourceId") String resourceId,
            @RequestParam(name = "timeoutMs", defaultValue = "5000") long timeoutMs
    ) throws InterruptedException {

        DistributedLock lock = distributedLockManager.lock(resource, resourceId);
        boolean ok = lock.tryLock(Duration.ofMillis(timeoutMs));
        if (!ok) {
            return ResponseEntity.ok(new DistributedLockResponse(resource, resourceId, DistributedLockStatus.FAILED));
        }

        String lockKey = resource + "/" + resourceId;
        heldLocks.put(lockKey, lock);
        return ResponseEntity.ok(new DistributedLockResponse(resource, resourceId, DistributedLockStatus.ACQUIRED));
    }

    @PostMapping("/{resource}/release")
    public ResponseEntity<DistributedLockResponse> release(
            @PathVariable("resource") String resource,
            @RequestParam(name = "resourceId") String resourceId
    ) {
        String lockKey = resource + "/" + resourceId;
        try (DistributedLock lock = heldLocks.remove(lockKey)) {
            if (lock == null) {
                return ResponseEntity.ok(new DistributedLockResponse(resource, resourceId, DistributedLockStatus.NOT_HELD));
            }
            lock.unlock();
            return ResponseEntity.ok(new DistributedLockResponse(resource, resourceId, DistributedLockStatus.RELEASED));
        }
    }

}
