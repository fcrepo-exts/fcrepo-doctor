/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.doctor.problem.fixer;

import org.apache.commons.io.IOUtils;
import org.fcrepo.doctor.util.ResourceHeadersUtil;
import org.fcrepo.storage.ocfl.OcflObjectSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Fixes binary descriptions with RDF that contain subjects that end in "/fcr:metadata". The binary itself,
 * rather than its description, should be used as the subject.
 *
 * @author winckles
 */
public class BinaryDescSubjectProblemFixer implements ProblemFixer {

    private static final Logger LOG = LoggerFactory.getLogger(BinaryDescSubjectProblemFixer.class);

    private static final Pattern PATTERN = Pattern.compile("^<([^>]+)/fcr:metadata>", Pattern.MULTILINE);

    @Override
    public void fix(final String resourceId, final OcflObjectSession objectSession) {
        LOG.debug("Applying invalid binary description subject fix to resource {}", resourceId);

        try (final var resource = objectSession.readContent(resourceId)) {
            if (resource.getContentStream().isPresent()) {
                final var original = IOUtils.toString(resource.getContentStream().get(), UTF_8);
                final var fixed = PATTERN.matcher(original).replaceAll("<$1>");
                final var updatedHeaders = ResourceHeadersUtil.touchForUpdate(resource.getHeaders());
                objectSession.writeResource(updatedHeaders, IOUtils.toInputStream(fixed, UTF_8));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed apply invalid binary description subject fix to resource " + resourceId, e);
        }
    }

}
