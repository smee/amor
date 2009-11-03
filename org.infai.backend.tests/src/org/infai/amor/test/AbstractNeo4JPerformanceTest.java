/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.test;

import java.util.logging.Logger;

import org.apache.commons.lang.time.StopWatch;
import org.junit.After;
import org.junit.Before;

/**
 * @author sdienst
 * 
 */
public class AbstractNeo4JPerformanceTest extends AbstractNeo4JTest {
    private static final Logger logger = Logger.getLogger(AbstractNeo4JPerformanceTest.class.getName());

    private StopWatch stopwatch;
    private long lastSplit = 0;

    @Before
    public void setupWatch() {
        stopwatch = new StopWatch();
        stopwatch.start();
        lastSplit = 0;
    }

    /**
     * @param msg
     */
    protected void split(final String msg) {
        stopwatch.split();
        final long splitTime = stopwatch.getSplitTime();
        final long timediff = splitTime - lastSplit;
        lastSplit = splitTime;
        logger.info(String.format("%s: %dmsec", msg, timediff));
        stopwatch.unsplit();
    }

    @After
    public void teardownWatch() {
        stopwatch.stop();
        logger.info(String.format("Test took %s", stopwatch.toString()));
    }
}
