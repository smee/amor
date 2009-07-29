/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.internal.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.infai.amor.backend.Branch;
import org.infai.amor.backend.internal.NeoProvider;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.api.core.EmbeddedNeo;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Transaction;

/**
 * @author sdienst
 * 
 */
public class NeoBranchFactoryImplTest {

    private NeoBranchFactory factory;
    private Transaction tx;
    private static NeoService neoservice;
    private static MockedTransactionNeoWrapper neoWithMockTransaction;

    @BeforeClass
    public static void createNeo() throws IOException {
        final File tempFile = File.createTempFile("unit", "test");
        tempFile.delete();
        tempFile.mkdirs();

        neoservice = new EmbeddedNeo(tempFile.getAbsolutePath());
        neoWithMockTransaction = new MockedTransactionNeoWrapper(neoservice);
    }

    @Before
    public void setUp() {
        factory = new NeoBranchFactory(new NeoProvider() {
            @Override
            public NeoService getNeo() {
                return neoWithMockTransaction;
            }
        });
        tx = neoservice.beginTx();
    }

    @Test
    public void testCreatesNewMainBranch() {
        final long startTime = System.currentTimeMillis();

        final Branch branch = factory.createBranch(null, "main");

        assertNotNull(branch);
        assertEquals("main", branch.getName());
        assertTrue(startTime <= branch.getCreationTime().getTime());
    }

    @Test
    public void testReturnsBranchIfExists() {
        final Branch branch = factory.createBranch(null, "main");
        final Branch branch2 = factory.createBranch(null, "main");

        assertTrue(((NeoBranch) branch).getNode().equals(((NeoBranch) branch2).getNode()));
    }

    @Test
    public void testSubBranchCreationWorks() {
        final Branch branch = factory.createBranch(null, "main");
        final Branch subBranch1 = factory.createBranch(branch, "sub1");
        final Branch subBranch2 = factory.createBranch(branch, "sub2");
        final Branch subsubBranch1 = factory.createBranch(subBranch1, "subsub1");

        for (final Branch b : factory.getBranches()) {
            System.out.println(b.getName());
        }
    }

    @After
    public void tearDown() {
        tx.failure();
        tx.finish();
    }

    @AfterClass
    public static void shutDownNeo() {
        neoservice.shutdown();
    }
}
