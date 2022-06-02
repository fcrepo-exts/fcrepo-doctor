/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.doctor.fixer;

import org.fcrepo.doctor.problem.ObjectProblems;
import org.fcrepo.doctor.problem.writer.ProblemWriter;
import org.fcrepo.doctor.util.Stoppable;
import org.fcrepo.doctor.util.StoppableThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixes problems in objects in a repository
 *
 * @author pwinckles
 */
public class RepoFixer {

    private static final Logger LOG = LoggerFactory.getLogger(RepoFixer.class);

    private static final Duration REPORTING_INTERVAL = Duration.ofMinutes(5);

    private final BlockingQueue<ObjectProblems> problemQueue;
    private final List<StoppableThread> fixers;
    private final ProblemWriter incompleteWriter;

    private int total;
    private final AtomicInteger failed;
    private final AtomicInteger complete;

    private boolean stopped = false;

    /**
     * @param parallelism number of worker threads to use; must be greater than 0
     * @param objectFixer fixer for fixing object problems
     * @param failedWriter writer for problems that fail to be processed
     * @param incompleteWriter writer for problems that are not processed
     */
    public RepoFixer(final int parallelism,
                     final ObjectFixer objectFixer,
                     final ProblemWriter failedWriter,
                     final ProblemWriter incompleteWriter) {
        this.incompleteWriter = incompleteWriter;
        this.failed = new AtomicInteger(0);
        this.complete = new AtomicInteger(0);

        if (parallelism < 1) {
            throw new IllegalArgumentException("Parallelism must be greater than 0");
        }

        this.problemQueue = new LinkedBlockingQueue<>();
        this.fixers = new ArrayList<>(parallelism);
        for (int i = 0; i < parallelism; i++) {
            fixers.add(new StoppableThread(
                    new ObjectFixerWorker(problemQueue, objectFixer, failedWriter, complete, failed),
                    "ObjectFixer-" + i));
        }
    }

    /**
     * Fixes all of the specified problems in a repository. This should only be called once.
     *
     * @param problems the list of ObjectProblems to fix
     */
    public void fixProblems(final List<ObjectProblems> problems) {
        LOG.info("Fixing problems in {} objects", problems.size());

        this.total = problems.size();

        // Intercepts ctrl+c for a safe shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        problemQueue.addAll(problems);

        startFixers();
        waitForEmptyQueue();
        stopFixers();
        stopped = true;

        final var incomplete = total - complete.get() - failed.get();
        LOG.info("Report:\n  - Fixed: {} objects\n  - Failed: {} objects\n  - Unprocessed: {} objects",
                complete.get(), failed.get(), incomplete);
    }

    private void stop() {
        if (!stopped) {
            LOG.info("Stopping...");
            drainQueue();
            stopFixers();
            stopped = true;
        }
    }

    private void startFixers() {
        fixers.forEach(StoppableThread::start);
    }

    private void stopFixers() {
        fixers.forEach(Stoppable::terminate);
        for (var fixer : fixers) {
            while (!fixer.hasStopped()) {
                Thread.onSpinWait();
            }
        }
    }

    private void waitForEmptyQueue() {
        var lastReport = Instant.now();
        while (!problemQueue.isEmpty()) {
            Thread.onSpinWait();
            if (Duration.between(lastReport, Instant.now()).compareTo(REPORTING_INTERVAL) > 0) {
                LOG.info("Processed {} objects out of {}", total - problemQueue.size(), total);
                lastReport = Instant.now();
            }
        }
    }

    private void drainQueue() {
        final var remaining = new LinkedList<ObjectProblems>();
        problemQueue.drainTo(remaining);
        for (var problems : remaining) {
            try {
                incompleteWriter.write(problems);
            } catch (RuntimeException e) {
                LOG.error("Failed to write unprocessed problems to file: {}", problems, e);
            }
        }
    }

}
