/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.doctor.analyzer;

import edu.wisc.library.ocfl.api.OcflRepository;
import org.fcrepo.doctor.problem.writer.ProblemWriter;
import org.fcrepo.doctor.util.Stoppable;
import org.fcrepo.doctor.util.StoppableThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Iterates over every OCFL object in a repository. The objects are passed off to worker threads to analyze and
 * report any problems found.
 *
 * @author winckles
 */
public class RepoAnalyzer {

    private static final Logger LOG = LoggerFactory.getLogger(RepoAnalyzer.class);

    private final BlockingQueue<String> objectIdQueue;
    private final OcflRepository ocflRepo;
    private final List<StoppableThread> analyzers;

    private boolean stop = false;

    /**
     * @param parallelism number of worker threads to use; must be greater than 0
     * @param ocflRepo the OCFL repository to analyze
     * @param objectAnalyzer analyzer for identifying problems within objects
     * @param problemWriter writer for recording any problems found
     */
    public RepoAnalyzer(final int parallelism,
                        final OcflRepository ocflRepo,
                        final ObjectAnalyzer objectAnalyzer,
                        final ProblemWriter problemWriter) {
        if (parallelism < 1) {
            throw new IllegalArgumentException("Parallelism must be greater than 0");
        }

        this.ocflRepo = ocflRepo;
        this.objectIdQueue = new LinkedBlockingQueue<>();
        this.analyzers = new ArrayList<>(parallelism);
        for (int i = 0; i < parallelism; i++) {
            analyzers.add(new StoppableThread(
                    new ObjectAnalyzerWorker(objectIdQueue, objectAnalyzer, problemWriter),
                    "ObjectAnalyzer-" + i));
        }
    }

    /**
     * Analyzes all of the object in a repository of problems. This should only be called once.
     */
    public void analyze() {
        LOG.info("Analyzing repository");

        // Intercepts ctrl+c for a safe shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        startAnalyzers();

        try (final var objectIds = ocflRepo.listObjectIds()) {
            final var objectIter = objectIds.iterator();
            while (!stop && objectIter.hasNext()) {
                try {
                    objectIdQueue.put(objectIter.next());
                } catch (RuntimeException e) {
                    LOG.error("Failed to queue object for analysis", e);
                } catch (InterruptedException e) {
                    stop();
                }
            }
        }

        waitForEmptyQueue();
        stopAnalyzers();
        stop = true;

        LOG.info("Analysis complete");
    }

    private void stop() {
        if (!stop) {
            LOG.info("Stopping...");
            stop = true;
            stopAnalyzers();
            drainQueue();
        }
    }

    private void startAnalyzers() {
        analyzers.forEach(StoppableThread::start);
    }

    private void stopAnalyzers() {
        analyzers.forEach(Stoppable::terminate);
        for (var analyzer : analyzers) {
            while (!analyzer.hasStopped()) {
                Thread.onSpinWait();
            }
        }
    }

    private void waitForEmptyQueue() {
        while (!objectIdQueue.isEmpty()) {
            Thread.onSpinWait();
        }
    }

    private void drainQueue() {
        objectIdQueue.clear();
    }

}
