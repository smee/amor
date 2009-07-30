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
import java.util.LinkedList;
import java.util.List;

import org.infai.amor.backend.Branch;
import org.infai.amor.backend.CommitTransaction;
import org.infai.amor.backend.Revision;
import org.infai.amor.backend.impl.CommitTransactionImpl;
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

    @AfterClass
    public static void shutDownNeo() {
        neoservice.shutdown();
    }

    /**
     * @param revisions
     * @param originRevision
     * @return
     */
    private <T> boolean contains(final Iterable<T> items, final T lookFor) {
        for (final T item : items) {
            if (item.equals(lookFor)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param i
     * @param string
     * @param string2
     * @return
     */
    private CommitTransaction createCommit(final long id, final Branch b, final String message, final String user) {
        final CommitTransaction tr = new CommitTransactionImpl(b, id);
        tr.setCommitMessage(message);
        tr.setUser(user);
        return tr;
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

    @After
    public void tearDown() {
        tx.failure();
        tx.finish();
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
    public void testNewBranchOriginIsHead() {
        final Branch branch = factory.createBranch(null, "main");
        final CommitTransaction tr = createCommit(1, branch, "test", "user");
        final Revision rootRevision = factory.createRevision(branch, tr);

        final Branch subBranch1 = factory.createBranch(rootRevision, "sub1");

        assertEquals(subBranch1.getHeadRevision().getRevisionId(), subBranch1.getOriginRevision().getRevisionId());
    }

    @Test
    public void testReturnsBranchIfExists() {
        final Branch branch = factory.createBranch(null, "main");
        final Branch branch2 = factory.createBranch(null, "main");

        assertTrue(((NeoBranch) branch).getNode().equals(((NeoBranch) branch2).getNode()));
    }

    @Test
    public void testRevisionChainingWorks() {
        final Branch branch = factory.createBranch(null, "main");
        final CommitTransaction tr = createCommit(1, branch, "test", "max");
        final CommitTransaction tr2 = createCommit(2, branch, "noch etwas vergessen", "user");

        final Revision rootRevision = factory.createRevision(branch, tr);
        final Revision nextRevision = factory.createRevision(branch, tr2);

        // a main branch has no origin
        assertTrue(null == branch.getOriginRevision());
        assertEquals(2, branch.getHeadRevision().getRevisionId());
        assertEquals(1, branch.getHeadRevision().getPreviousRevision().getRevisionId());
    }

    @Test
    public void testSubbranchHasRevisionOrigin() {
        final Branch branch = factory.createBranch(null, "main");
        final CommitTransaction tr = createCommit(1, branch, "test", "max");
        final Revision rootRevision = factory.createRevision(branch, tr);

        final Branch subBranch1 = factory.createBranch(rootRevision, "sub1");

        assertEquals(1, subBranch1.getOriginRevision().getRevisionId());
    }

    @Test
    public void testSubBranching() {
        // create main branch
        final Branch mainbranch = factory.createBranch(null, "main");
        final CommitTransaction tr = createCommit(1, mainbranch, "test", "max");
        // with one revision
        factory.createRevision(mainbranch, tr);
        // create a subbranch
        final Revision branchingRev = factory.createRevision(mainbranch, createCommit(2, mainbranch, "test2", "max"));

        // add 3 revisions to the subbranch
        final Branch subbranch = factory.createBranch(branchingRev, "sub1");
        factory.createRevision(subbranch, createCommit(3, subbranch, "test3", "max"));
        factory.createRevision(subbranch, createCommit(4, subbranch, "test4", "max"));
        final Revision rev5 = factory.createRevision(subbranch, createCommit(5, subbranch, "test5", "max"));

        // is branching revision == subbranch orgin revision?
        assertEquals(branchingRev.getRevisionId(), subbranch.getOriginRevision().getRevisionId());

        // get all revisions of the subbranch
        final List<Revision> subBranchRevisions = new LinkedList<Revision>();
        for (final Revision rev : subbranch.getRevisions()) {
            subBranchRevisions.add(rev);
        }
        // rootRevision + 3 unique revisions
        assertEquals(4, subBranchRevisions.size());
        assertTrue(contains(subBranchRevisions, rev5));
    }
}
