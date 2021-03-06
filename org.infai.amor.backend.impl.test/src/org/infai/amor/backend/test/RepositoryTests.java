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

import static org.junit.Assert.*;

import java.io.IOException;

import org.infai.amor.backend.*;
import org.infai.amor.backend.impl.RepositoryImpl;
import org.infai.amor.backend.internal.*;
import org.infai.amor.backend.storage.Storage;
import org.infai.amor.backend.storage.StorageFactory;
import org.infai.amor.test.TestUtils;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.*;

/**
 * @author sdienst
 * 
 */
public class RepositoryTests {

    private Mockery context;
    private RepositoryImpl repo;
    private Storage storage;
    private BranchFactory branchFactory;
    private UriHandler uriHandler;
    private TransactionManager transactionManager;
    private StorageFactory storageFactory;

    @Test
    public void delegatesMainBranchCreation() {
        final Branch mockedBranch = context.mock(Branch.class);
        context.checking(new Expectations() {
            {
                one(branchFactory).createBranch(null, null);
                will(returnValue(mockedBranch));
            }
        });
        final Branch branch = repo.createBranch(null, null);

        assertNotNull(branch);
        assertTrue(mockedBranch == branch);
    }

    @Test
    public void delegatesSubBranchCreation() {
        final Branch mockedBranch = context.mock(Branch.class, "mainBranch");
        final Branch subBranch = context.mock(Branch.class, "subBranch");
        final Revision revision = context.mock(Revision.class);

        context.checking(new Expectations() {
            {
                one(branchFactory).createBranch(revision, "branch1");
                will(returnValue(subBranch));

                allowing(subBranch).getName();
                will(returnValue("branch1"));
            }
        });
        final Branch branch = repo.createBranch(revision, "branch1");

        assertNotNull(branch);
        assertEquals("branch1", branch.getName());
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        context = new Mockery();
        storage = context.mock(Storage.class);
        branchFactory = context.mock(BranchFactory.class);
        uriHandler = context.mock(UriHandler.class);
        transactionManager = context.mock(TransactionManager.class);
        storageFactory = context.mock(StorageFactory.class);
        repo = new RepositoryImpl(storageFactory, branchFactory, uriHandler, transactionManager);
    }
    @After
    public void tearDown() {
        context.assertIsSatisfied();
    }
    @Test
    public void testSavesChangedModelIntoStorage() throws IOException {
        final ChangedModel model = context.mock(ChangedModel.class);
        final Branch branch = context.mock(Branch.class);
        final InternalCommitTransaction tr = context.mock(InternalCommitTransaction.class);
        final InternalRevision revision = TestUtils.createRevision(1);
        context.checking(new Expectations() {
            {
                allowing(tr).getBranch();
                will(returnValue(branch));
                one(storageFactory).getStorage(tr);
                will(returnValue(storage));
                // needs to checkin into our given storage
                one(storage).checkin(model, null, revision);
                allowing(model);
                allowing(uriHandler);
                allowing(branchFactory);
                allowing(tr).getRevision();
                will(returnValue(revision));
            }
        });

        repo.checkin(model, tr);
    }

    @Test
    public void testSavesIntoStorage() throws IOException {
        final Model model = context.mock(Model.class);
        final InternalCommitTransaction tr = context.mock(InternalCommitTransaction.class);
        final Branch branch = context.mock(Branch.class);
        final InternalRevision revision = TestUtils.createRevision(1);

        context.checking(new Expectations() {
            {
                allowing(tr).getBranch();
                will(returnValue(branch));
                one(storageFactory).getStorage(tr);
                will(returnValue(storage));
                one(storage).checkin(model, null, revision);
                allowing(model);
                allowing(uriHandler);
                allowing(branchFactory);
                allowing(tr).getRevision();
                will(returnValue(revision));
            }
        });

        repo.checkin(model, tr);
    }
}
