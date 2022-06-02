/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.doctor.problem.fixer;

import org.fcrepo.storage.ocfl.OcflObjectSession;

/**
 * Fixes problems in resources
 *
 * @author winckles
 */
public interface ProblemFixer {

    /**
     * Fixes a problem with a resource
     *
     * @param resourceId the id of the resource to fix
     * @param objectSession the OCFL session for the object that contains the resource
     */
    void fix(final String resourceId, final OcflObjectSession objectSession);

}
