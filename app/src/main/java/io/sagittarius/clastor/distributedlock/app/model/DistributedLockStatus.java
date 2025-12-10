package io.sagittarius.clastor.distributedlock.app.model;

public enum DistributedLockStatus {
    ACQUIRED,
    RELEASED,
    NOT_HELD,
    FAILED
}
