/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.doctor.analyzer;

import org.fcrepo.doctor.problem.writer.ProblemWriter;
import org.fcrepo.doctor.util.Stoppable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Analyzes Fedora OCFL objects for problems and records any problems found. Each ObjectAnalyzerWorker is intended to
 * run in its own thread, and pulls objects to analyze off a queue. It will run indefinitely until explicitly stopped.
 *
 * @author winckles
 */
public class ObjectAnalyzerWorker implements Stoppable {

    private static final Logger LOG = LoggerFactory.getLogger(ObjectAnalyzerWorker.class);

    private final BlockingQueue<String> objectIdQueue;
    private final ObjectAnalyzer objectAnalyzer;
    private final ProblemWriter problemWriter;

    private boolean stop = false;
    private boolean stopped = false;

    /**
     * @param objectIdQueue queue of object ids to process
     * @param objectAnalyzer analyzer for identifying problems within objects
     * @param problemWriter writer for recording any problems found
     */
    public ObjectAnalyzerWorker(final BlockingQueue<String> objectIdQueue,
                                final ObjectAnalyzer objectAnalyzer,
                                final ProblemWriter problemWriter) {
        this.objectIdQueue = objectIdQueue;
        this.objectAnalyzer = objectAnalyzer;
        this.problemWriter = problemWriter;
    }

    @Override
    public void run() {
        while (!stop) {
            final var objectId = getObjectId();
            if (objectId == null) {
                continue;
            }

            try {
                final var objectProblems = objectAnalyzer.analyze(objectId);

                if (objectProblems.hasProblems()) {
                    problemWriter.write(objectProblems);
                }
            } catch (RuntimeException e) {
                LOG.error("Failed to analyze object {}", objectId, e);
            }
        }

        stopped = true;
    }

    @Override
    public void terminate() {
        stop = true;
    }

    @Override
    public boolean hasStopped() {
        return stopped;
    }

    private String getObjectId() {
        try {
            return objectIdQueue.poll(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            stop = true;
            return null;
        }
    }

}
