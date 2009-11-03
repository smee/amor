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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.neo4j.api.core.EmbeddedNeo;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Transaction;

/**
 * Base test class that provides a neo4j database with a running transaction per test. The transaction gets rolled back after each
 * test.
 * 
 * @author sdienst
 * 
 */
public abstract class AbstractNeo4JTest {
    protected Transaction tx;
    protected static NeoService neoservice;

    @BeforeClass
    public static void createNeo() throws IOException {
        final File tempFile = File.createTempFile("unit", "test");
        tempFile.delete();
        tempFile.mkdirs();

        neoservice = new EmbeddedNeo(tempFile.getAbsolutePath(), new HashMap<String, String>() {
            {
                // put("min_relationship_cache_size", "15000");
                // put("min_node_cache_size", "10000");
            }
        });
    }

    @AfterClass
    public static void shutDownNeo() {
        neoservice.shutdown();
    }

    @After
    final public void afterTest() {
        if (isRollbackAfterTest()) {
            tx.failure();
        } else {
            tx.success();
        }
        tx.finish();
    }

    @Before
    final public void beforeTest() {
        tx = neoservice.beginTx();
    }

    /**
     * Overwrite this method to specify, whether changes to the neo4j db should persist after a test or not. Default is true;
     * 
     * @return
     */
    protected boolean isRollbackAfterTest() {
        return true;
    }
}
