package io.sagittarius.clastor.distributedlock.app.controller;

import io.sagittarius.clastor.distributedlock.app.model.DistributedLockResponse;
import io.sagittarius.clastor.distributedlock.app.model.DistributedLockStatus;
import io.sagittarius.clastor.distributedlock.app.model.DistributedLockVersionInfo;
import io.sagittarius.clastor.distributedlock.domain.DistributedLock;
import io.sagittarius.clastor.distributedlock.domain.DistributedLockManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/v1/locks")
@RequiredArgsConstructor
public class ClastorDistributedController {

    private static final Pattern PATH_SEGMENT = Pattern.compile("[A-Za-z0-9_-][A-Za-z0-9._-]*");

    private final DistributedLockManager distributedLockManager;
    private final ConcurrentMap<String, LockEntry> locks = new ConcurrentHashMap<>();

    @GetMapping("/held")
    public ResponseEntity<Set<String>> heldLocks() {
        return ResponseEntity.ok(locks.entrySet().stream()
                .filter(entry -> entry.getValue().lock != null)
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet()));
    }

    @GetMapping("/{resource}/version")
    public ResponseEntity<DistributedLockVersionInfo> getVersion(@PathVariable("resource") String resource) {
        validateSegment("resource", resource);
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

        validateSegment("resource", resource);
        validateSegment("resourceId", resourceId);
        if (timeoutMs < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "timeoutMs must be non-negative");
        }

        String lockKey = resource + "/" + resourceId;
        LockEntry entry = new LockEntry();
        if (locks.putIfAbsent(lockKey, entry) != null) {
            return ResponseEntity.ok(new DistributedLockResponse(resource, resourceId, DistributedLockStatus.FAILED));
        }

        boolean acquired = false;
        try {
            DistributedLock lock = distributedLockManager.lock(resource, resourceId);
            acquired = lock.tryLock(Duration.ofMillis(timeoutMs));
            if (acquired) {
                entry.lock = lock;
            }
            var status = acquired ? DistributedLockStatus.ACQUIRED : DistributedLockStatus.FAILED;
            return ResponseEntity.ok(new DistributedLockResponse(resource, resourceId, status));
        } finally {
            if (!acquired) {
                locks.remove(lockKey, entry);
            }
        }
    }

    @PostMapping("/{resource}/release")
    public ResponseEntity<DistributedLockResponse> release(
            @PathVariable("resource") String resource,
            @RequestParam(name = "resourceId") String resourceId
    ) {
        validateSegment("resource", resource);
        validateSegment("resourceId", resourceId);
        String lockKey = resource + "/" + resourceId;
        LockEntry entry = locks.get(lockKey);
        if (entry != null) {
            synchronized (entry) {
                if (entry.lock != null) {
                    entry.lock.unlock();
                    entry.lock = null;
                    locks.remove(lockKey, entry);
                    return ResponseEntity.ok(new DistributedLockResponse(resource, resourceId, DistributedLockStatus.RELEASED));
                }
            }
        }
        return ResponseEntity.ok(new DistributedLockResponse(resource, resourceId, DistributedLockStatus.NOT_HELD));
    }

    private static void validateSegment(String name, String value) {
        if (!PATH_SEGMENT.matcher(value).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid " + name);
        }
    }

    private static final class LockEntry {
        private volatile DistributedLock lock;
    }
}
