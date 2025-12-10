package io.sagittarius.clastor.distributedlock.app.model;

public record DistributedLockResponse(
        String resource,
        String resourceId,
        DistributedLockStatus status
) {
}
