/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.integration;

import static org.infai.amor.ModelUtil.readInputModel;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.infai.amor.ModelUtil;
import org.infai.amor.backend.Branch;
import org.infai.amor.backend.CommitTransaction;
import org.infai.amor.backend.Model;
import org.infai.amor.backend.Repository;
import org.infai.amor.backend.Response;
import org.infai.amor.backend.Revision;
import org.infai.amor.backend.Revision.ChangeType;
import org.infai.amor.backend.impl.RepositoryImpl;
import org.infai.amor.backend.internal.NeoProvider;
import org.infai.amor.backend.internal.TransactionManager;
import org.infai.amor.backend.internal.UriHandler;
import org.infai.amor.backend.internal.impl.ModelImpl;
import org.infai.amor.backend.internal.impl.NeoBranchFactory;
import org.infai.amor.backend.internal.impl.TransactionManagerImpl;
import org.infai.amor.backend.internal.impl.UriHandlerImpl;
import org.infai.amor.backend.internal.responses.CommitSuccessResponse;
import org.infai.amor.backend.internal.storage.FileStorageFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.api.core.EmbeddedNeo;
import org.neo4j.api.core.NeoService;

import com.google.common.collect.Iterables;

/**
 * @author sdienst
 * 
 */
public class IntegrationTests {
    protected Repository repository;
    private static NeoService neoservice;
    private static NeoProvider neoprovider;

    @BeforeClass
    public static void beforeClass() throws IOException {
        final File tempFile = File.createTempFile("integration", "test");
        tempFile.delete();
        tempFile.mkdirs();

        neoservice = new EmbeddedNeo(tempFile.getAbsolutePath());
        neoprovider = new NeoProvider() {
            @Override
            public NeoService getNeo() {
                return neoservice;
            }
        };
    }

    @AfterClass
    public static void tearDown() {
        neoservice.shutdown();
    }

    @Before
    public void setup() throws IOException {
        final UriHandler uh = new UriHandlerImpl("localhost", "repo");
        final TransactionManager tm = new TransactionManagerImpl(uh, neoprovider);
        final NeoBranchFactory bf = new NeoBranchFactory(neoprovider);
        // create storage directory
        final File tempDir = File.createTempFile("storage", "temp");
        tempDir.delete();
        tempDir.mkdirs();
        final FileStorageFactory sf = new FileStorageFactory();
        sf.setStorageDir(tempDir.getAbsolutePath());
        tm.addTransactionListener(sf);

        repository = new RepositoryImpl(sf, bf, uh, tm);

    }

    @Test
    public void shouldBeAbleToCreateBranch() throws Exception {
        // given
        final String branchName = "branch" + Math.random();
        // when
        final Branch branch = repository.createBranch(null, branchName);
        // then
        assertNotNull(branch);
        assertEquals(branchName, branch.getName());

        // tear down
        // commitTransaction.getNeoTransaction().failure();
        // commitTransaction.getNeoTransaction().finish();
    }

    @Test(expected = IOException.class)
    public void shouldNotLoadAfterRollback() throws Exception {
        // given
        final EObject input = readInputModel("testmodels/base.ecore");

        final Branch branch = repository.createBranch(null, "trunk");
        final CommitTransaction ct = repository.startCommitTransaction(branch);
        ct.setCommitMessage("test");
        ct.setUser("mustermann");
        // when
        // model checked in
        final Response checkin = repository.checkin(new ModelImpl(input, "testmodels/base.ecore"), ct);
        // but rolled back
        repository.rollbackTransaction(ct);
        // then
        repository.checkout(checkin.getURI());
    }

    @Test
    public void shouldSaveAndLoadModel() throws Exception {
        // given
        final EObject input = readInputModel("testmodels/base.ecore");

        final Branch branch = repository.createBranch(null, "trunk");
        final CommitTransaction ct = repository.startCommitTransaction(branch);
        ct.setCommitMessage("test");
        ct.setUser("mustermann");
        // when
        // model checked in successfully
        final Response checkin = repository.checkin(new ModelImpl(input, "testmodels/base.ecore"), ct);
        final CommitSuccessResponse commitResponse = (CommitSuccessResponse) repository.commitTransaction(ct);
        /*
         * // then the revision should know about this model final Revision revision =
         * repository.getRevision(commitResponse.getURI()); assertEquals(1, revision.getModelReferences().size());
         */
        // and reloaded from repository successfully
        final Model output = repository.checkout(checkin.getURI());
        // then
        ModelUtil.assertModelEqual(input, output.getContent());
    }

    @Test
    public void shouldShowRepositoryContentsPerRevision() throws Exception {
        // given
        final EObject input = readInputModel("testmodels/base.ecore");

        final Branch branch = repository.createBranch(null, "trunk");
        // when
        // start new checkin transaction
        final CommitTransaction ct = repository.startCommitTransaction(branch);
        ct.setCommitMessage("test");
        ct.setUser("mustermann");
        // add a model
        final Response checkin = repository.checkin(new ModelImpl(input, "testmodels/base.ecore"), ct);
        final CommitSuccessResponse commitResponse = (CommitSuccessResponse) repository.commitTransaction(ct);
        // then the revision should know about this model
        final Revision revision = repository.getRevision(commitResponse.getURI());
        assertEquals(1, revision.getModelReferences(ChangeType.ADDED).size());

        // now delete the model
        final CommitTransaction ct2 = repository.startCommitTransaction(branch);
        ct2.setCommitMessage("delete model");
        ct2.setUser("mustermann");
        repository.deleteModel(new Path("testmodels/base.ecore"), ct2);
        final CommitSuccessResponse commitResponse2 = (CommitSuccessResponse) repository.commitTransaction(ct2);

        final Iterable<URI> activeContents = repository.getActiveContents(commitResponse2.getURI());
        // no models should be visible at revision 3
        assertTrue(Iterables.isEmpty(activeContents));

    }

    @After
    public void teardown() {

    }
}
