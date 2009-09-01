/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.internal.storage.neo;

import static org.infai.amor.ModelUtil.readInputModel;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.infai.amor.backend.Branch;
import org.infai.amor.backend.CommitTransaction;
import org.infai.amor.backend.Model;
import org.infai.amor.backend.Repository;
import org.infai.amor.backend.Response;
import org.infai.amor.backend.impl.RepositoryImpl;
import org.infai.amor.backend.internal.NeoProvider;
import org.infai.amor.backend.internal.TransactionManager;
import org.infai.amor.backend.internal.UriHandler;
import org.infai.amor.backend.internal.impl.AbstractNeo4JPerformanceTest;
import org.infai.amor.backend.internal.impl.ModelImpl;
import org.infai.amor.backend.internal.impl.NeoBranchFactory;
import org.infai.amor.backend.internal.impl.TransactionManagerImpl;
import org.infai.amor.backend.internal.impl.UriHandlerImpl;
import org.infai.amor.backend.internal.responses.CheckinResponse;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.api.core.NeoService;

/**
 * @author sdienst
 * 
 */
public class NeoBlobStorageTest extends AbstractNeo4JPerformanceTest {
    private Repository repository;

    @Before
    public void setup() throws IOException {
        final UriHandler uh = new UriHandlerImpl("localhost", "repo");
        final NeoProvider np = new NeoProvider() {
            @Override
            public NeoService getNeo() {
                return neoservice;
            }
        };
        final TransactionManager tm = new TransactionManagerImpl(uh, np);
        final NeoBranchFactory bf = new NeoBranchFactory(np);
        final NeoBlobStorageFactory sf = new NeoBlobStorageFactory(np);
        tm.addTransactionListener(sf);

        repository = new RepositoryImpl(sf, bf, uh, tm);
    }

    @Test
    public void shouldSaveModelIntoNeo() throws Exception {
        split("Before loading models");
        // given
        final ResourceSet rs = new ResourceSetImpl();
        final EObject input = readInputModel("testmodels/Ecore.ecore", rs);
        final EObject input2 = readInputModel("testmodels/filesystem.ecore", rs);
        final EObject input3 = readInputModel("testmodels/simplefilesystem.xmi", rs);
        split("After loading models");

        final Branch branch = repository.createBranch(null, "trunk");
        final CommitTransaction ct = repository.startCommitTransaction(branch);
        ct.setCommitMessage("test");
        ct.setUser("mustermann");
        // when
        // model checked in successfully
        final Response checkin = repository.checkin(new ModelImpl(input, "testmodels/Ecore.ecore"), ct);
        split("Neo4j - Ecore");
        final Response checkin2 = repository.checkin(new ModelImpl(input2, "testmodels/filesystem.ecore"), ct);
        split("Neo4j - M2");
        final Response checkin3 = repository.checkin(new ModelImpl(input3, "testmodels/simplefilesystem.xmi"), ct);
        split("Neo4j - M1");
        repository.commitTransaction(ct);
        split("Neo4j - Commit");
        // then
        assertTrue(checkin instanceof CheckinResponse);
        // storeViaXml(input, input2, input3);

        final Model checkedoutmodel = repository.checkout(URI.createURI("amor://localhost/repo/trunk/1/testmodels/filesystem.ecore"));
        split("Restoring M1");
        storeViaXml(checkedoutmodel.getContent());
    }

    /**
     * @param input
     * @param input2
     * @param input3
     * @throws IOException
     */
    private void storeViaXml(final EObject... input) throws IOException {
        final ResourceSetImpl rs = new ResourceSetImpl();
        for (final EObject eo : input) {
            final Resource res = rs.createResource(URI.createFileURI("foo/" + eo.hashCode() + ".xml"));
            res.getContents().add(eo);
            res.save(null);
        }
        split("Writing XML");
    }
}
