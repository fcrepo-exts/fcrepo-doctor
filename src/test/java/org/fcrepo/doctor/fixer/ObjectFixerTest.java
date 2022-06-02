/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.doctor.fixer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.wisc.library.ocfl.core.OcflRepositoryBuilder;
import edu.wisc.library.ocfl.core.path.mapper.LogicalPathMappers;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.fcrepo.doctor.problem.ObjectProblems;
import org.fcrepo.doctor.problem.ProblemType;
import org.fcrepo.doctor.problem.fixer.BinaryDescSubjectProblemFixer;
import org.fcrepo.storage.ocfl.CommitType;
import org.fcrepo.storage.ocfl.DefaultOcflObjectSessionFactory;
import org.fcrepo.storage.ocfl.OcflObjectSession;
import org.fcrepo.storage.ocfl.OcflObjectSessionFactory;
import org.fcrepo.storage.ocfl.cache.NoOpCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author pwinckles
 */
public class ObjectFixerTest {

    @TempDir
    Path tempDir;
    private Path ocflRoot;
    private Path ocflTemp;
    private OcflObjectSessionFactory objectSessionFactory;

    private ObjectFixer fixer;

    @BeforeEach
    public void setup() throws IOException {
        ocflTemp = Files.createDirectories(tempDir.resolve("temp"));
        ocflRoot = tempDir.resolve("root");

        FileUtils.copyDirectory(
                Paths.get("src/test/resources/repos/invalid-binary-desc-subjects").toFile(),
                ocflRoot.toFile()
        );

        objectSessionFactory = createObjectSessionFactory();

        fixer = new ObjectFixer(
                objectSessionFactory,
                Map.of(
                        ProblemType.INVALID_BIN_DESC_SUBJ, new BinaryDescSubjectProblemFixer()
                )
        );
    }

    @Test
    public void fixObjectWithInvalidBinaryDescSubj() throws IOException {
        final var objectId = "info:fedora/ag";
        final var problems = problems(objectId,
                Map.of(
                        objectId + "/child-1/fcr:metadata",
                        Set.of(ProblemType.INVALID_BIN_DESC_SUBJ),
                        objectId + "/child-2/grandchild-2/fcr:metadata",
                        Set.of(ProblemType.INVALID_BIN_DESC_SUBJ)
                )
        );

        fixer.fix(problems);

        assertContent(objectId, "/child-1/fcr:metadata",
                "<info:fedora/ag/child-1> <http://purl.org/dc/elements/1.1/title> \"Child 1\" .\n");
        assertContent(objectId, "/child-2/grandchild-2/fcr:metadata",
                "<info:fedora/ag/child-2/grandchild-2> <http://purl.org/dc/elements/1.1/title> \"Grandchild 2\" .\n");
    }

    private void assertContent(final String objectId,
                               final String resourceSuffix,
                               final String expected) throws IOException {
        try (final var session = objectSessionFactory.newSession(objectId)) {
            final var actual = readContent(objectId + resourceSuffix, session);
            assertEquals(expected, actual);
        }
    }

    private String readContent(final String resourceId, final OcflObjectSession session) throws IOException {
        try (final var content = session.readContent(resourceId)) {
            return IOUtils.toString(content.getContentStream().get(), StandardCharsets.UTF_8);
        }
    }

    private ObjectProblems problems(final String objectId,
                                    final Map<String, Set<ProblemType>> resourceProblems) {
        final var problems = new ObjectProblems(objectId);
        problems.setResourceProblems(resourceProblems);
        return problems;
    }

    private OcflObjectSessionFactory createObjectSessionFactory() {
        final var logicalPathMapper = SystemUtils.IS_OS_WINDOWS ?
                LogicalPathMappers.percentEncodingWindowsMapper() : LogicalPathMappers.percentEncodingLinuxMapper();

        final var ocflRepo = new OcflRepositoryBuilder()
                .logicalPathMapper(logicalPathMapper)
                .storage(builder -> builder.fileSystem(ocflRoot))
                .workDir(ocflTemp)
                .buildMutable();

        final var objectMapper = new ObjectMapper()
                .configure(WRITE_DATES_AS_TIMESTAMPS, false)
                .registerModule(new JavaTimeModule())
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);

        return new DefaultOcflObjectSessionFactory(ocflRepo,
                ocflTemp,
                objectMapper,
                new NoOpCache<>(),
                new NoOpCache<>(),
                CommitType.NEW_VERSION,
                "message",
                "fedoraAdmin",
                "info:fedora/fedoraAdmin");
    }

}
