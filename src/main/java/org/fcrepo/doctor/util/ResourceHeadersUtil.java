/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.doctor.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.fcrepo.storage.ocfl.ResourceHeaders;

import java.time.Instant;
import java.util.ArrayList;

/**
 * Utils for working with resource headers
 *
 * @author pwinckles
 */
public final class ResourceHeadersUtil {

    private ResourceHeadersUtil() {
        // noop
    }

    /**
     * Updates the minimal set of resource headers than must be updated when a resource is modified
     *
     * @param original the original resource headers
     * @return the updated resource headers
     */
    public static ResourceHeaders touchForUpdate(final ResourceHeaders original) {
        final var now = Instant.now();
        return new ResourceHeaders.Builder(original)
                .withDigests(new ArrayList<>())
                .withContentSize(-1L)
                .withLastModifiedDate(now)
                .withLastModifiedBy("fedoraAdmin")
                .withMementoCreatedDate(now)
                .withStateToken(DigestUtils.md5Hex(String.valueOf(now)).toUpperCase())
                .build();
    }

}
