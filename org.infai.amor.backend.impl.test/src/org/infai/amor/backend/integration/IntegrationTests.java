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

import java.io.File;
import java.io.IOException;

import org.eclipse.emf.ecore.EObject;
import org.infai.amor.ModelUtil;
import org.infai.amor.backend.Branch;
import org.infai.amor.backend.CommitTransaction;
import org.infai.amor.backend.Model;
import org.infai.amor.backend.Repository;
import org.infai.amor.backend.Response;
import org.infai.amor.backend.impl.RepositoryImpl;
import org.infai.amor.backend.internal.NeoProvider;
import org.infai.amor.backend.internal.TransactionManager;
import org.infai.amor.backend.internal.UriHandler;
import org.infai.amor.backend.internal.impl.ModelImpl;
import org.infai.amor.backend.internal.impl.NeoBranchFactory;
import org.infai.amor.backend.internal.impl.TransactionManagerImpl;
import org.infai.amor.backend.internal.impl.UriHandlerImpl;
import org.infai.amor.backend.internal.storage.DumbStorageFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.api.core.EmbeddedNeo;
import org.neo4j.api.core.NeoService;

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
        final DumbStorageFactory sf = new DumbStorageFactory();
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
        repository.commitTransaction(ct);
        // and reloaded from repository successfully
        final Model output = repository.checkout(checkin.getURI());
        // then
        ModelUtil.assertModelEqual(input, output.getContent());
    }

    @After
    public void teardown() {

    }
}
