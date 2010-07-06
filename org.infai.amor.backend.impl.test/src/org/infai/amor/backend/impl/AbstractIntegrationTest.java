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

import java.io.File;
import java.io.IOException;

import org.infai.amor.backend.Repository;
import org.infai.amor.backend.internal.*;
import org.infai.amor.backend.internal.impl.*;
import org.infai.amor.backend.storage.StorageFactory;
import org.junit.*;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.EmbeddedGraphDatabase;

/**
 * @author sdienst
 *
 */
public abstract class AbstractIntegrationTest {

    protected Repository repository;
    private static GraphDatabaseService neoservice;
    protected static NeoProvider neoprovider;

    @BeforeClass
    public static void beforeClass() throws IOException {
        final File tempFile = File.createTempFile("integration", "test");
        tempFile.delete();
        tempFile.mkdirs();

        neoservice = new EmbeddedGraphDatabase(tempFile.getAbsolutePath());
        neoprovider = new NeoProvider() {
            @Override
            public GraphDatabaseService getNeo() {
                return neoservice;
            }
        };
    }

    @AfterClass
    public static void tearDown() {
        neoservice.shutdown();
    }

    /**
     * 
     */
    public AbstractIntegrationTest() {
        super();
    }

    /**
     * @param tempDir
     * @return
     */
    abstract protected StorageFactory createStorageFactory(File tempDir);

    @Before
    public void setup() throws IOException {
        final UriHandler uh = new UriHandlerImpl("localhost", "repo");
        final NeoBranchFactory bf = new NeoBranchFactory(neoprovider);
        final TransactionManager tm = new TransactionManagerImpl(uh, neoprovider, bf);
        // create storage directory
        final File tempDir = File.createTempFile("storage", "temp");
        tempDir.delete();
        tempDir.mkdirs();
        final StorageFactory sf = createStorageFactory(tempDir);
        tm.addTransactionListener(sf);

        repository = new RepositoryImpl(sf, bf, uh, tm);

    }

    @After
    public void teardown() {

    }

}