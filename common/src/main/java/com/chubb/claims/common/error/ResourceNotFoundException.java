package com.chubb.claims.common.error;

/**
 * Thrown when a resource is missing or not visible to the caller. Claimants get this
 * for claims they don't own, so claim IDs can't be probed. Mapped to HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " not found: " + id);
    }
}
