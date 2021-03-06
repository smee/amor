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

import static org.infai.amor.test.TestUtils.createLocation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.*;

import org.eclipse.emf.common.util.URI;
import org.infai.amor.backend.*;
import org.infai.amor.backend.impl.CommitTransactionImpl;
import org.infai.amor.backend.neo.NeoProvider;
import org.infai.amor.test.*;
import org.junit.*;
import org.neo4j.graphdb.GraphDatabaseService;

import com.google.common.collect.Iterables;

/**
 * @author sdienst
 * 
 */
public class NeoBranchFactoryImplTest extends AbstractNeo4JTest {

    private NeoBranchFactory factory;

    private NeoBranch mainbranch;

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
        return Iterables.contains(items, lookFor);
    }

    /**
     * @param i
     * @param string
     * @param string2
     * @return
     */
    private CommitTransaction createCommit(final long id, final Branch b, final String message, final String user) {
        final CommitTransaction tr = new CommitTransactionImpl(b, TestUtils.createRevision(id), tx);
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
        final CommitTransaction tr = createCommit(1, mainbranch, "test", "max");
        // with one revision
        factory.createRevision(mainbranch, 1);
        // create a subbranch
        final Revision branchingRev = factory.createRevision(mainbranch, 2);

        // add 3 revisions to the subbranch
        final Branch subbranch = factory.createBranch(branchingRev, "sub1");
        factory.createRevision(subbranch, 3);
        factory.createRevision(subbranch, 4);
        factory.createRevision(subbranch, 5);
        // and one more to the mainbranch
        factory.createRevision(mainbranch, 6);
        return subbranch;
    }

    @Before
    public void setUp() {
        factory = new NeoBranchFactory(new NeoProvider() {
            @Override
            public GraphDatabaseService getNeo() {
                return neoWithMockTransaction;
            }
        });
        mainbranch = factory.createBranch(null, "main");
    }

    @Test
    public void shouldIgnoreDeletedModels() throws Exception {
        // given
        final String model1path = "deleteme.model";
        final URI model1uri = URI.createURI("amor://localhost/repo/main/1/" + model1path);
        final String model2path = "donottouchme.model";
        final URI model2uri = URI.createURI("amor://localhost/repo/main/1/" + model2path);
        // added two models
        final NeoRevision rev1 = factory.createRevision(mainbranch, 1);
        rev1.touchedModel(createLocation(model1uri, model1path, ChangeType.ADDED));
        rev1.touchedModel(createLocation(model2uri, model2path, ChangeType.ADDED));
        // deleted one model
        final NeoRevision rev2 = factory.createRevision(mainbranch, 2);
        final URI model3uri = URI.createURI("amor://localhost/repo/main/2/" + model2path);
        rev2.touchedModel(createLocation(model3uri, model1path, ChangeType.DELETED));
        // when
        // ask for contents
        final Collection<ModelLocation> addedModelReferencesOnRev1 = rev1.getModelReferences(ChangeType.ADDED);
        final Collection<ModelLocation> addedModelReferencesOnRev2 = rev2.getModelReferences(ChangeType.ADDED);
        final Collection<ModelLocation> deletedModelReferencesOnRev2 = rev2.getModelReferences(ChangeType.DELETED);
        // then
        assertEquals(2, addedModelReferencesOnRev1.size());
        assertEquals(0, addedModelReferencesOnRev2.size());
        assertEquals(1, deletedModelReferencesOnRev2.size());
    }

    @Test
    public void testFindRevisionById() {
        final Branch subbranch = createComplexRevisionTree();
        assertTrue(subbranch.getRevision(3) != null);
    }

    @Test
    public void testNewBranchOriginIsHead() {
        final Revision rootRevision = factory.createRevision(mainbranch, 1);

        final Branch subBranch1 = factory.createBranch(rootRevision, "sub1");

        assertEquals(subBranch1.getHeadRevision().getRevisionId(), subBranch1.getOriginRevision().getRevisionId());
    }

    @Test
    public void testReturnsBranchIfExists() {
        final Branch branch2 = factory.createBranch(null, "main");

        assertTrue((mainbranch).getNode().equals(((NeoBranch) branch2).getNode()));
    }

    @Test
    public void testRevisionChainingWorks() {
        final Revision rootRevision = factory.createRevision(mainbranch, 1);
        final Revision nextRevision = factory.createRevision(mainbranch, 2);

        // a main branch has no origin
        assertTrue(null == mainbranch.getOriginRevision());
        assertEquals(2, mainbranch.getHeadRevision().getRevisionId());
        assertEquals(1, mainbranch.getHeadRevision().getPreviousRevision().getRevisionId());
    }

    @Test
    public void testSubbranchHasRevisionOrigin() {
        final Revision rootRevision = factory.createRevision(mainbranch, 1);

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
