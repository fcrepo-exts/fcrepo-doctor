/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.doctor.util;

/**
 * Thread implementation that gracefully stops its Runnable
 *
 * @author winckles
 */
public class StoppableThread extends Thread implements Stoppable {

    private final Stoppable stoppable;

    /**
     * @param stoppable the inner Stoppable to execute
     * @param name the name of the thread
     */
    public StoppableThread(final Stoppable stoppable, final String name) {
        super(stoppable, name);
        this.stoppable = stoppable;
    }

    @Override
    public void terminate() {
        stoppable.terminate();
    }

    @Override
    public boolean hasStopped() {
        return stoppable.hasStopped();
    }

}
