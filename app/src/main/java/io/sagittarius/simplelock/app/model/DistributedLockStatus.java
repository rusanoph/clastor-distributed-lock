package io.sagittarius.simplelock.app.model;

public enum DistributedLockStatus {
    ACQUIRED,
    RELEASED,
    NOT_HELD,
    FAILED
}
