package io.sagittarius.simplelock.app.model;

public record DistributedLockResponse(
        String resource,
        String resourceId,
        DistributedLockStatus status
) {
}
