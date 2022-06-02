/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.doctor.fixer;

import org.fcrepo.doctor.problem.ObjectProblems;
import org.fcrepo.doctor.problem.writer.ProblemWriter;
import org.fcrepo.doctor.util.Stoppable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reads from a queue of object problems and fixes them
 *
 * @author winckles
 */
public class ObjectFixerWorker implements Stoppable {

    private static final Logger LOG = LoggerFactory.getLogger(ObjectFixerWorker.class);

    private final BlockingQueue<ObjectProblems> problemQueue;
    private final ObjectFixer objectFixer;
    private final ProblemWriter problemWriter;

    private final AtomicInteger complete;
    private final AtomicInteger failed;

    private boolean stop = false;
    private boolean stopped = false;

    /**
     * @param problemQueue  queue of problems to fix
     * @param objectFixer   fixer for fixing object problems
     * @param problemWriter writes problems that fail to process
     * @param complete count of how many objects successfully fixed
     * @param failed count of how many objects failed to fix
     */
    public ObjectFixerWorker(final BlockingQueue<ObjectProblems> problemQueue,
                             final ObjectFixer objectFixer,
                             final ProblemWriter problemWriter,
                             final AtomicInteger complete,
                             final AtomicInteger failed) {
        this.problemQueue = problemQueue;
        this.objectFixer = objectFixer;
        this.problemWriter = problemWriter;
        this.complete = complete;
        this.failed = failed;
    }

    @Override
    public void run() {
        while (!stop) {
            final var problems = getProblems();
            if (problems == null) {
                continue;
            }

            try {
                objectFixer.fix(problems);
                complete.incrementAndGet();
            } catch (RuntimeException e) {
                LOG.error("Failed to fix problems: {}", problems, e);
                writeProblem(problems);
                failed.incrementAndGet();
            }
        }

        stopped = true;
    }

    private void writeProblem(final ObjectProblems problems) {
        try {
            problemWriter.write(problems);
        } catch (RuntimeException e) {
            LOG.error("Failed to write problems to file: {}", problems, e);
        }
    }

    @Override
    public void terminate() {
        stop = true;
    }

    @Override
    public boolean hasStopped() {
        return stopped;
    }

    private ObjectProblems getProblems() {
        try {
            return problemQueue.poll(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            stop = true;
            return null;
        }
    }

}
