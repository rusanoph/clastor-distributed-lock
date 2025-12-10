package io.sagittarius.clastor.distributedlock.domain;

/**
 * A manager for distributed locks.
 */
public interface DistributedLockManager {
    // TODO: Add resource mode like: normal, draining for correct rotation support

    /**
     * Acquires a distributed lock for a given resource kind and resource id.
     *
     * @param resourceKind the kind of the resource to lock
     * @param resourceId the id of the resource to lock
     * @return a distributed lock that can be used to lock the resource
     */
    DistributedLock lock(String resourceKind, String resourceId);

    /**
     * Returns the current version of the distributed lock system.
     *
     * @return the current version of the distributed lock system
     */
    DistributedLockVersionAccessor versionAccessor();

    /**
     * Closes the manager and releases all resources.
     *
     * @throws Exception if an exception occurs while closing the manager
     */
    void close() throws Exception;
}
