/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.infai.amor.backend.Branch;
import org.infai.amor.backend.Revision;
import org.infai.amor.backend.Revision.ChangeType;
import org.infai.amor.backend.internal.*;
import org.infai.amor.backend.internal.impl.UriHandlerImpl;
import org.infai.amor.backend.storage.Storage;
import org.infai.amor.backend.storage.StorageFactory;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.*;

import com.google.common.collect.Iterables;

/**
 * @author sdienst
 * 
 */
public class QueryTests {
    private Mockery context;
    private RepositoryImpl repo;
    private Storage storage;
    private BranchFactory branchFactory;
    private UriHandler uriHandler;
    private TransactionManager transactionManager;
    private StorageFactory storageFactory;
    private Branch branch1;

    @Before
    public void setUp() throws Exception {
        context = new Mockery();
        storage = context.mock(Storage.class);
        branchFactory = context.mock(BranchFactory.class);
        uriHandler = new UriHandlerImpl("localhost", "repo");
        transactionManager = context.mock(TransactionManager.class);
        storageFactory = context.mock(StorageFactory.class);
        repo = new RepositoryImpl(storageFactory, branchFactory, uriHandler, transactionManager);
        branch1 = context.mock(Branch.class, "b1");
        final Branch br2 = context.mock(Branch.class, "b2");
        final Revision rev1 = context.mock(Revision.class, "rev1");
        final Revision rev2 = context.mock(Revision.class, "rev2");
        final Revision rev3 = context.mock(Revision.class, "rev3");
        final List<Revision> revisions = Arrays.asList(rev1, rev2, rev3);
        final List<Branch> branches = Arrays.asList(branch1, br2);
        /*
         * Two branches (branch1, branch2) where branch1 contains the revisions 1 and 2. Rev1 refers to path1/model1, rev2 refers
         * to path1/model2
         */
        context.checking(new Expectations() {
            {
                allowing(branchFactory).getBranches();
                will(returnValue(branches));
                allowing(branch1).getName();
                will(returnValue("branch1"));
                allowing(br2).getName();
                will(returnValue("branch2"));
                allowing(branchFactory).getBranch("branch1");
                will(returnValue(branch1));
                allowing(branch1).getRevision(1L);
                will(returnValue(rev1));
                allowing(branch1).getRevisions();
                will(returnValue(revisions));
                allowing(branch1).getRevision(2L);
                will(returnValue(rev2));
                allowing(branch1).getRevision(3L);
                will(returnValue(rev3));
                allowing(branch1).getHeadRevision();
                will(returnValue(rev3));
                allowing(rev3).getPreviousRevision();
                will(returnValue(rev2));
                allowing(rev2).getPreviousRevision();
                will(returnValue(rev1));
                allowing(rev1).getPreviousRevision();
                will(returnValue(null));
                allowing(rev1).getRevisionId();
                will(returnValue(1L));
                allowing(rev2).getRevisionId();
                will(returnValue(2L));
                allowing(rev3).getRevisionId();
                will(returnValue(3L));
                final URI uri1 = URI.createURI("amor://localhost/repo/branch1/1/path1/model1");
                allowing(rev1).getModelReferences(ChangeType.ADDED);
                will(returnValue(Arrays.asList(uri1)));
                final URI uri2 = URI.createURI("amor://localhost/repo/branch1/2/path1/model2");
                allowing(rev2).getModelReferences(ChangeType.ADDED);
                will(returnValue(Arrays.asList(uri2)));
                final URI uri3 = URI.createURI("amor://localhost/repo/branch1/3/path1/model3");
                allowing(rev3).getModelReferences(ChangeType.ADDED);
                will(returnValue(Arrays.asList(uri3)));
                allowing(rev1).getModelReferences(with(any(Revision.ChangeType.class)));
                allowing(rev2).getModelReferences(with(any(Revision.ChangeType.class)));
                allowing(rev3).getModelReferences(with(any(Revision.ChangeType.class)));
                allowing(transactionManager).startReadTransaction();
                allowing(transactionManager).closeReadTransaction();
            }
        });
    }

    @Test
    @Ignore(value = "unclear, tests mocked stuff only?")
    public void shouldReturnAllBranches() throws Exception {
        // given

        // when
        final Iterable<URI> uris = repo.getContents(URI.createURI("amor://localhost/repo/"));
        // then
        assertEquals(2, Iterables.size(uris));
    }

    @Test
    @Ignore(value = "unclear, tests mocked stuff only?")
    public void shouldReturnAllPathsForRevision1() throws Exception {
        // given

        // when
        final Iterable<URI> uris = repo.getActiveContents(URI.createURI("amor://localhost/repo/branch1/1"));
        // then
        assertEquals(1, Iterables.size(uris));
        assertEquals(URI.createURI("amor://localhost/repo/branch1/1/path1"), Iterables.get(uris, 0));
    }

    @Test
    @Ignore(value = "unclear, tests mocked stuff only?")
    public void shouldReturnAllPathsForRevision1Path1() throws Exception {
        // given

        // when
        final Iterable<URI> uris = repo.getContents(URI.createURI("amor://localhost/repo/branch1/1/path1"));
        // then
        assertEquals(1, Iterables.size(uris));
        assertEquals(URI.createURI("amor://localhost/repo/branch1/1/path1/model1"), Iterables.get(uris, 0));
    }

    @Test
    @Ignore(value = "unclear, tests mocked stuff only?")
    public void shouldReturnAllPathsForRevision2Path1() throws Exception {
        // given

        // when
        final Iterable<URI> uris = repo.getActiveContents(URI.createURI("amor://localhost/repo/branch1/2/path1"));
        // then
        // finds all models, ordered backwards
        assertEquals(2, Iterables.size(uris));
        assertTrue(Iterables.contains(uris, URI.createURI("amor://localhost/repo/branch1/2/path1/model2")));
        assertTrue(Iterables.contains(uris, URI.createURI("amor://localhost/repo/branch1/1/path1/model1")));
    }

    @Test
    @Ignore(value = "unclear, tests mocked stuff only?")
    public void shouldReturnAllPathsForRevision3Path1() throws Exception {
        // given

        // when
        final Iterable<URI> uris = repo.getActiveContents(URI.createURI("amor://localhost/repo/branch1/3/path1"));
        // then
        // finds all models, ordered backwards
        assertEquals(3, Iterables.size(uris));
        assertTrue(Iterables.contains(uris, URI.createURI("amor://localhost/repo/branch1/3/path1/model3")));
        assertTrue(Iterables.contains(uris, URI.createURI("amor://localhost/repo/branch1/2/path1/model2")));
        assertTrue(Iterables.contains(uris, URI.createURI("amor://localhost/repo/branch1/1/path1/model1") ));
    }

    @Test
    @Ignore(value = "unclear, tests mocked stuff only?")
    public void shouldReturnAllRevisionsOfBranch1() throws Exception {
        // given

        // when
        final Iterable<URI> uris = repo.getContents(URI.createURI("amor://localhost/repo/branch1"));
        // then
        assertEquals(3, Iterables.size(uris));
        assertEquals(URI.createURI("amor://localhost/repo/branch1/1"), Iterables.get(uris, 0));
        assertEquals(URI.createURI("amor://localhost/repo/branch1/2"), Iterables.get(uris, 1));
        assertEquals(URI.createURI("amor://localhost/repo/branch1/3"), Iterables.get(uris, 2));
    }

    @Test
    @Ignore(value = "unclear, tests mocked stuff only?")
    public void shouldReturnNoPathsForRevision2Path1() throws Exception {
        // given

        // when
        final Iterable<URI> uris = repo.getContents(URI.createURI("amor://localhost/repo/branch1/2/path1"));
        // then
        assertEquals(1, Iterables.size(uris));
        assertEquals(URI.createURI("amor://localhost/repo/branch1/2/path1/model2"), Iterables.get(uris, 0));
    }

    @Test
    @Ignore(value = "unclear, tests mocked stuff only?")
    public void shouldReturnPathsForRevision2() throws Exception {
        // given

        // when
        final Iterable<URI> uris = repo.getContents(URI.createURI("amor://localhost/repo/branch1/2"));
        // then
        assertEquals(1, Iterables.size(uris));
        assertEquals(URI.createURI("amor://localhost/repo/branch1/2/path1"), Iterables.get(uris, 0));
    }

    @After
    public void tearDown() {
        context.assertIsSatisfied();
    }
}
