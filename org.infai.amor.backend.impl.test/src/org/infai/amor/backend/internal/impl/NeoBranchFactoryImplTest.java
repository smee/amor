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

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.infai.amor.backend.Branch;
import org.infai.amor.backend.CommitTransaction;
import org.infai.amor.backend.Revision;
import org.infai.amor.backend.impl.CommitTransactionImpl;
import org.infai.amor.backend.internal.NeoProvider;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.api.core.NeoService;

/**
 * @author sdienst
 * 
 */
public class NeoBranchFactoryImplTest extends AbstractNeo4JTest {

    private NeoBranchFactory factory;

    private static MockedTransactionNeoWrapper neoWithMockTransaction;

    @BeforeClass
    public static void beforeClass() throws IOException {
        neoWithMockTransaction = new MockedTransactionNeoWrapper(neoservice);
    }

    /**
     * @param branches
     * @return
     */
    private <T> List<T> collect(final Iterable<T> items) {
        final List<T> result = new LinkedList<T>();
        for (final T revision : items) {
            result.add(revision);
        }
        return result;
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
        final CommitTransaction tr = new CommitTransactionImpl(b, id, tx);
        tr.setCommitMessage(message);
        tr.setUser(user);
        return tr;
    }

    /**
     * <pre>
     * Branch
     *   |
     *   1 
     *   |
     *   2 -- subbranch
     *   |  |
     *   6  3
     *      |
     *      4
     *      |
     *      5
     * </pre>
     * 
     * @return
     */
    private Branch createComplexRevisionTree() {
        // create main branch
        final Branch mainbranch = factory.createBranch(null, "main");
        final CommitTransaction tr = createCommit(1, mainbranch, "test", "max");
        // with one revision
        factory.createRevision(tr);
        // create a subbranch
        final Revision branchingRev = factory.createRevision(createCommit(2, mainbranch, "test2", "max"));

        // add 3 revisions to the subbranch
        final Branch subbranch = factory.createBranch(branchingRev, "sub1");
        factory.createRevision(createCommit(3, subbranch, "test3", "max"));
        factory.createRevision(createCommit(4, subbranch, "test4", "max"));
        factory.createRevision(createCommit(5, subbranch, "test5", "max"));

        factory.createRevision(createCommit(6, mainbranch, "test6", "max"));
        return subbranch;
    }

    @Before
    public void setUp() {
        factory = new NeoBranchFactory(new NeoProvider() {
            @Override
            public NeoService getNeo() {
                return neoWithMockTransaction;
            }
        });
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
    public void testFindRevisionById() {
        final Branch subbranch = createComplexRevisionTree();
        assertTrue(subbranch.getRevision(3) != null);
    }

    @Test
    public void testNewBranchOriginIsHead() {
        final Branch branch = factory.createBranch(null, "main");
        final CommitTransaction tr = createCommit(1, branch, "test", "user");
        final Revision rootRevision = factory.createRevision(tr);

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

        final Revision rootRevision = factory.createRevision(tr);
        final Revision nextRevision = factory.createRevision(tr2);

        // a main branch has no origin
        assertTrue(null == branch.getOriginRevision());
        assertEquals(2, branch.getHeadRevision().getRevisionId());
        assertEquals(1, branch.getHeadRevision().getPreviousRevision().getRevisionId());
    }

    @Test
    public void testSubbranchHasRevisionOrigin() {
        final Branch branch = factory.createBranch(null, "main");
        final CommitTransaction tr = createCommit(1, branch, "test", "max");
        final Revision rootRevision = factory.createRevision(tr);

        final Branch subBranch1 = factory.createBranch(rootRevision, "sub1");

        assertEquals(1, subBranch1.getOriginRevision().getRevisionId());
    }

    @Test
    public void testSubBranching() {
        final Branch subbranch = createComplexRevisionTree();

        // is branching revision == subbranch orgin revision?
        assertEquals(2, subbranch.getOriginRevision().getRevisionId());

        // get all revisions of the subbranch
        final List<Revision> subBranchRevisions = new LinkedList<Revision>();
        for (final Revision rev : subbranch.getRevisions()) {
            subBranchRevisions.add(rev);
        }
        // rootRevision + 3 unique revisions
        assertEquals(4, subBranchRevisions.size());
        assertTrue(contains(subBranchRevisions, subbranch.getHeadRevision()));
        assertTrue(contains(subBranchRevisions, subbranch.getOriginRevision()));
    }

    @Test
    public void testSubBranching2() {
        final Branch subbranch = createComplexRevisionTree();
        assertEquals(2, collect(factory.getBranches()).size());
    }
}
