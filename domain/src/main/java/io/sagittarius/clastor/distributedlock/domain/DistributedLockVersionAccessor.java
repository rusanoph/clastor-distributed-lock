package io.sagittarius.clastor.distributedlock.domain;

/**
 * Interface for accessing the current version of the distributed lock system.
 */
public interface DistributedLockVersionAccessor {

    /**
     * Returns the path to the version node for a given resource kind.
     * @param resourceKind the kind of the resource
     * @return the path to the version node for the given resource kind
     */
    String versionPath(String resourceKind);

    /**
     * Returns the current version of the distributed lock system for a given resource kind.
     * Example:
     *   resourceKind = "transaction" -> "v3"
     *   resourceKind = "user"        -> "v1"
     * @param resourceKind the kind of the resource
     * @return the current version of the distributed lock system for the given resource kind
     */
    String currentVersion(String resourceKind);
}