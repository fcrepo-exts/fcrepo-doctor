/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.doctor.problem.fixer;

import org.apache.commons.io.IOUtils;
import org.fcrepo.storage.ocfl.OcflObjectSession;
import org.fcrepo.storage.ocfl.ResourceContent;
import org.fcrepo.storage.ocfl.ResourceHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

/**
 * @author pwinckles
 */
@ExtendWith(MockitoExtension.class)
public class BinaryDescSubjectProblemFixerTest {

    @Mock
    OcflObjectSession objectSession;

    private BinaryDescSubjectProblemFixer fixer;

    private String resourceId;

    @BeforeEach
    public void setup() {
        fixer = new BinaryDescSubjectProblemFixer();
        resourceId = "info:fedora/abc";
    }

    @Test
    public void fixSubjects() throws Exception {
        resourceContent(
                "<info:fedora/ag-01/child-01/fcr:metadata> <http://purl.org/dc/elements/1.1/title> \"My title 1\" .\n" +
                "<info:fedora/ag-01/child-01> <http://purl.org/dc/elements/1.1/title> \"My title 2\" .\n" +
                "<info:fedora/ag-01/child-01/fcr:metadata> <http://purl.org/dc/elements/1.1/title> <info:fedora/ag-01/child-02/fcr:metadata> .\n"
        );

        final var fixedContent = captureFixedContent();

        fixer.fix(resourceId, objectSession);

        assertEquals("<info:fedora/ag-01/child-01> <http://purl.org/dc/elements/1.1/title> \"My title 1\" .\n" +
                        "<info:fedora/ag-01/child-01> <http://purl.org/dc/elements/1.1/title> \"My title 2\" .\n" +
                        "<info:fedora/ag-01/child-01> <http://purl.org/dc/elements/1.1/title> <info:fedora/ag-01/child-02/fcr:metadata> .\n",
                fixedContent.get());
    }

    private void resourceContent(final String content) {
        final var resourceContent = new ResourceContent(IOUtils.toInputStream(content, StandardCharsets.UTF_8),
                new ResourceHeaders.Builder().build());
        doReturn(resourceContent)
                .when(objectSession).readContent(resourceId);
    }

    private Future<String> captureFixedContent() {
        final var content = new CompletableFuture<String>();
        doAnswer(args -> {
            content.complete(IOUtils.toString(args.getArgument(1, InputStream.class), StandardCharsets.UTF_8));
            return null;
        }).when(objectSession).writeResource(any(), any());
        return content;
    }

}
