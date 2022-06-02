/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.doctor.problem.reader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.fcrepo.doctor.problem.ObjectProblems;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Reads ObjectProblems from a json file. The file must contain an array of ObjectProblems.
 *
 * @author winckles
 */
public class DefaultProblemReader implements ProblemReader {

    private final ObjectReader reader;

    public DefaultProblemReader(final ObjectMapper objectMapper) {
        this.reader = objectMapper.readerFor(new TypeReference<List<ObjectProblems>>() {});
    }

    @Override
    public List<ObjectProblems> read(final Path jsonFile) {
        try {
            return reader.readValue(jsonFile.toFile());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
