/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.*;

import org.eclipse.emf.common.util.URI;
import org.infai.amor.backend.*;
import org.infai.amor.backend.impl.RepositoryImpl;
import org.infai.amor.backend.internal.*;
import org.infai.amor.backend.internal.impl.UriHandlerImpl;
import org.infai.amor.backend.storage.Storage;
import org.infai.amor.backend.storage.StorageFactory;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.*;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

/**
 * @author sdienst
 * 
 */
public class QueryTests {
    private static class MockLocation implements ModelLocation {

        private ChangeType ct;
        private String path;
        private URI uri;

        public MockLocation(ChangeType ct, String path, String branch, int id) {
            this.ct = ct;
            this.path = path;
            this.uri = URI.createURI("amor://localhost/repo/" + branch + "/" + id + "/" + path);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.infai.amor.backend.ModelLocation#getChangeType()
         */
        @Override
        public ChangeType getChangeType() {
            return ct;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.infai.amor.backend.ModelLocation#getExternalUri()
         */
        @Override
        public URI getExternalUri() {
            return uri;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.infai.amor.backend.ModelLocation#getMetaData()
         */
        @Override
        public Map<String, Object> getMetaData() {
            return Collections.EMPTY_MAP;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.infai.amor.backend.ModelLocation#getNamespaceUris()
         */
        @Override
        public Collection<String> getNamespaceUris() {
            return Collections.EMPTY_LIST;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.infai.amor.backend.ModelLocation#getRelativePath()
         */
        @Override
        public String getRelativePath() {
            return path;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.infai.amor.backend.ModelLocation#isMetaModel()
         */
        @Override
        public boolean isMetaModel() {
            return false;
        }

    }
    private static class MockRev implements Revision {
        private int id;
        private Revision prev;
        private Collection<String> added;
        private Collection<String> deleted;
        private String branch;

        public MockRev(int id, Revision prev, String branch, Collection<String> added, Collection<String> deleted) {
            this.id = id;
            this.prev = prev;
            this.branch = branch;
            this.added = added != null ? added : Collections.EMPTY_LIST;
            this.deleted = deleted != null ? deleted : Collections.EMPTY_LIST;
        }
        @Override
        public String getCommitMessage() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Date getCommitTimestamp() {
            throw new UnsupportedOperationException();
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.infai.amor.backend.Revision#getModelReferences(org.infai.amor.backend.Revision.ChangeType[])
         */
        @Override
        public Collection<ModelLocation> getModelReferences(ChangeType... ct) {
            Set<ModelLocation> locs = Sets.newHashSet();
            for (ChangeType changetype : ct) {
                if (changetype == ChangeType.ADDED) {
                    for (String path : added) {
                        locs.add(new MockLocation(ChangeType.ADDED, path, branch, id));
                    }
                }
                else if (changetype == ChangeType.DELETED) {
                    for (String path : deleted) {
                        locs.add(new MockLocation(ChangeType.DELETED, path, branch, id));
                    }
                }
            }
            return locs;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.infai.amor.backend.Revision#getPreviousRevision()
         */
        @Override
        public Revision getPreviousRevision() {
            return prev;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.infai.amor.backend.Revision#getRevisionId()
         */
        @Override
        public long getRevisionId() {
            return id;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.infai.amor.backend.Revision#getUser()
         */
        @Override
        public String getUser() {
            throw new UnsupportedOperationException();
        }

    }

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
        final List<Branch> branches = Arrays.asList(branch1, br2);
        final Revision rev1 = new MockRev(1, null, "branch1", Arrays.asList("path1/model1"), null);
        final Revision rev2 = new MockRev(2, rev1, "branch1", Arrays.asList("path1/model2"), null);
        final Revision rev3 = new MockRev(3, rev2, "branch1", Arrays.asList("path1/model3"), null);
        //delete model1
        final Revision rev4 = new MockRev(4, rev3, "branch1", null, Arrays.asList("path1/model1"));
        //re-add model1
        final Revision rev5 = new MockRev(5, rev4, "branch1", Arrays.asList("path1/model1"),null);

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
                allowing(branch1).getRevisions();
                will(returnValue(Arrays.asList(rev3, rev2, rev1)));
                allowing(branch1).getRevision(1L);  will(returnValue(rev1));
                allowing(branch1).getRevision(2L);  will(returnValue(rev2));
                allowing(branch1).getRevision(3L);  will(returnValue(rev3));
                allowing(branch1).getRevision(4L);  will(returnValue(rev4));
                allowing(branch1).getRevision(5L);  will(returnValue(rev5));

                allowing(transactionManager).startReadTransaction();
                allowing(transactionManager).closeReadTransaction();
            }
        });
    }

    @Test
    public void shouldRespectDeletedModels() throws Exception {
        // given

        // when
        final Iterable<URI> uris = repo.getActiveContents(URI.createURI("amor://localhost/repo/branch1/4/path1"));
        // then
        System.out.println(Iterables.toString(uris));
        assertEquals(2, Iterables.size(uris));
        assertTrue(Iterables.contains(uris, URI.createURI("amor://localhost/repo/branch1/2/path1/model2")));
        assertTrue(Iterables.contains(uris, URI.createURI("amor://localhost/repo/branch1/3/path1/model3")));
    }

    @Test
    public void shouldRespectReAddedModels() throws Exception {
        // given

        // when
        final Iterable<URI> uris = repo.getActiveContents(URI.createURI("amor://localhost/repo/branch1/5/path1"));
        // then
        System.out.println(Iterables.toString(uris));
        assertEquals(3, Iterables.size(uris));
        assertTrue(Iterables.contains(uris, URI.createURI("amor://localhost/repo/branch1/2/path1/model2")));
        assertTrue(Iterables.contains(uris, URI.createURI("amor://localhost/repo/branch1/3/path1/model3")));
        assertTrue(Iterables.contains(uris, URI.createURI("amor://localhost/repo/branch1/5/path1/model1")));
    }

    @Test
    public void shouldReturnAllBranches() throws Exception {
        // given

        // when
        final Iterable<URI> uris = repo.getActiveContents(URI.createURI("amor://localhost/repo/"));
        // then
        assertEquals(2, Iterables.size(uris));
    }

    @Test
    public void shouldReturnAllPathsForRevision1() throws Exception {
        // given

        // when
        final Iterable<URI> uris = repo.getActiveContents(URI.createURI("amor://localhost/repo/branch1/1"));
        // then
        assertEquals(1, Iterables.size(uris));
        assertEquals(URI.createURI("amor://localhost/repo/branch1/1/path1"), Iterables.get(uris, 0));
    }

    @Test
    public void shouldReturnAllPathsForRevision1Path1() throws Exception {
        // given

        // when
        final Iterable<URI> uris = repo.getActiveContents(URI.createURI("amor://localhost/repo/branch1/1/path1"));
        // then
        assertEquals(1, Iterables.size(uris));
        assertEquals(URI.createURI("amor://localhost/repo/branch1/1/path1/model1"), Iterables.get(uris, 0));
    }

    @Test
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
    public void shouldReturnAllRevisionsOfBranch1() throws Exception {
        // given

        // when
        final Iterable<URI> uris = repo.getActiveContents(URI.createURI("amor://localhost/repo/branch1"));
        // then
        assertEquals(3, Iterables.size(uris));
        assertTrue(Iterables.contains(uris, URI.createURI("amor://localhost/repo/branch1/1")));
        assertTrue(Iterables.contains(uris, URI.createURI("amor://localhost/repo/branch1/2")));
        assertTrue(Iterables.contains(uris, URI.createURI("amor://localhost/repo/branch1/3")));
    }

    @Test
    public void shouldReturnPathsForRevision2() throws Exception {
        // given

        // when
        final Iterable<URI> uris = repo.getActiveContents(URI.createURI("amor://localhost/repo/branch1/2"));
        // then
        assertEquals(2, Iterables.size(uris));
        assertTrue(Iterables.contains(uris, URI.createURI("amor://localhost/repo/branch1/2/path1")));
    }

    @After
    public void tearDown() {
        context.assertIsSatisfied();
    }
}
