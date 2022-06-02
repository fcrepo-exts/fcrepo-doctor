/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.doctor.fixer;

import org.fcrepo.doctor.problem.ObjectProblems;
import org.fcrepo.doctor.problem.ProblemType;
import org.fcrepo.doctor.problem.fixer.ProblemFixer;
import org.fcrepo.storage.ocfl.OcflObjectSession;
import org.fcrepo.storage.ocfl.OcflObjectSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Fixes previously identified problems in an object
 *
 * @author winckles
 */
public class ObjectFixer {

    private static final Logger LOG = LoggerFactory.getLogger(ObjectFixer.class);

    private final OcflObjectSessionFactory objectSessionFactory;
    private final Map<ProblemType, ProblemFixer> problemFixers;

    /**
     * @param objectSessionFactory OCFL object session factory
     * @param problemFixers map problem types to their fixers
     */
    public ObjectFixer(final OcflObjectSessionFactory objectSessionFactory,
                       final Map<ProblemType, ProblemFixer> problemFixers) {
        this.objectSessionFactory = objectSessionFactory;
        this.problemFixers = problemFixers;
    }

    /**
     * Fixes a set of problems in an object. All of the problems are fixed within a single OCFL version.
     * If any of the problems fails to be fixed, then all of the fixes are rolled back.
     *
     * @param problems the problems to fix.
     */
    public void fix(final ObjectProblems problems) {
        LOG.debug("Fixing problems: {}", problems);

        try (final var session = objectSessionFactory.newSession(problems.getOcflObjectId())) {
            try {
                problems.getResourceProblems().forEach((resourceId, resourceProblems) -> {
                    resourceProblems.forEach(problem -> {
                        LOG.debug("Fixing {} in resource {}", problem, resourceId);
                        problemFixers.get(problem).fix(resourceId, session);
                    });
                });
                session.commit();
            } catch (RuntimeException e) {
                rollback(session);
                throw e;
            }
        }
    }

    private void rollback(final OcflObjectSession session) {
        try {
            session.rollback();
        } catch (RuntimeException e) {
            LOG.error("Failed to rollback session for object {}", session.ocflObjectId(), e);
        }
    }

}
