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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;

import org.infai.amor.backend.Branch;
import org.infai.amor.backend.Repository;
import org.infai.amor.backend.impl.CommitTransactionImpl;
import org.infai.amor.backend.impl.RepositoryImpl;
import org.infai.amor.backend.internal.BranchFactory;
import org.infai.amor.backend.internal.NeoProvider;
import org.infai.amor.backend.internal.TransactionManager;
import org.infai.amor.backend.internal.UriHandler;
import org.infai.amor.backend.internal.impl.DumbStorageFactory;
import org.infai.amor.backend.internal.impl.NeoBranchFactory;
import org.infai.amor.backend.internal.impl.TransactionManagerImpl;
import org.infai.amor.backend.internal.impl.UriHandlerImpl;
import org.infai.amor.backend.storage.StorageFactory;
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
    private CommitTransactionImpl commitTransaction;

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
    public void setup() {
        final UriHandler uh = new UriHandlerImpl();
        final TransactionManager tm = new TransactionManagerImpl(uh, neoprovider);
        final BranchFactory bf = new NeoBranchFactory(neoprovider);
        final StorageFactory sf = new DumbStorageFactory();
        repository = new RepositoryImpl(sf, bf, uh, tm);

        commitTransaction = (CommitTransactionImpl) repository.startCommitTransaction(null); // FIXME should not be necessary
    }

    @Test
    public void shouldBeAbleToCreateBranch() throws Exception {
        // given
        // when
        final Branch branch = repository.createBranch(null, "trunk");
        // then
        assertNotNull(branch);
        assertEquals("trunk", branch.getName());
    }

    @After
    public void teardown() {
        commitTransaction.getNeoTransaction().failure();
        commitTransaction.getNeoTransaction().finish();
    }
}
