/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.doctor.problem.reader;

import org.fcrepo.doctor.problem.ObjectProblems;

import java.nio.file.Path;
import java.util.List;

/**
 * Reads ObjectProblems from a json file. The file must contain an array of ObjectProblems.
 *
 * @author winckles
 */
public interface ProblemReader {

    /**
     * Read a list of ObjectProblems from a json file
     *
     * @param jsonFile the file containing the problems to read
     * @return problems
     */
    List<ObjectProblems> read(final Path jsonFile);

}
