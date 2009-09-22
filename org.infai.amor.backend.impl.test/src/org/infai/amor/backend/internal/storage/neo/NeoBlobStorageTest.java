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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collection;
import java.util.logging.Logger;

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
import org.infai.amor.backend.Revision;
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.neo4j.api.core.NeoService;

import com.google.common.collect.Lists;

/**
 * @author sdienst
 * 
 */
@RunWith(Parameterized.class)
public class NeoBlobStorageTest extends AbstractNeo4JPerformanceTest {
    private static final Logger logger = Logger.getLogger(NeoBlobStorageTest.class.getName());
    private Repository repository;
    private final String m1Location, m2Location;
    private CountingNeoProxy proxy;

    /**
     * Run one roundtrip for every pair of m2/m1 model.
     * 
     * @return
     */
    @Parameters
    public static Collection<String[]> getTestParameters() {
        final Collection<String[]> params = Lists.newArrayList();
        params.add(new String[] { "testmodels/filesystem.ecore", "testmodels/simplefilesystem.xmi" });
        params.add(new String[] { "testmodels/filesystem.ecore", "testmodels/fs/simplefilesystem_v1.filesystem" });
        params.add(new String[] { "testmodels/aris.ecore", "testmodels/model_partial.xmi" });
        return params;
    }

    /**
     * @param modelLocations
     */
    public NeoBlobStorageTest(final String m2, final String m1) {
        this.m1Location = m1;
        this.m2Location = m2;
    }

    // /*
    // * (non-Javadoc)
    // *
    // * @see org.infai.amor.backend.internal.impl.AbstractNeo4JTest#isRollbackAfterTest()
    // */
    // @Override
    // protected boolean isRollbackAfterTest() {
    // return false;
    // }

    @After
    public void printStatistics() {
        logger.info(String.format("# nodes created: %d, # relationships created: %d", proxy.getNumNodes(), proxy.getNumRels()));
    }

    @Before
    public void setup() throws IOException {
        final UriHandler uh = new UriHandlerImpl("localhost", "repo");
        proxy = new CountingNeoProxy(neoservice);
        final NeoProvider np = new NeoProvider() {

            @Override
            public NeoService getNeo() {
                return proxy;
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
        split("Before loading models " + m2Location + " and " + m1Location);
        // given
        final ResourceSet rs = new ResourceSetImpl();
        final EObject input = readInputModel("testmodels/Ecore.ecore", rs);
        final EObject input2 = readInputModel(m2Location, rs);
        final EObject input3 = readInputModel(m1Location, rs);
        split("After loading models");

        final Branch branch = repository.createBranch(null, "trunk");
        final CommitTransaction ct = repository.startCommitTransaction(branch);
        ct.setCommitMessage("test");
        ct.setUser("mustermann");
        // when
        // model checked in successfully
        final Response checkin = repository.checkin(new ModelImpl(input, "testmodels/Ecore.ecore"), ct);
        split("Neo4j - Ecore");
        final Response checkin2 = repository.checkin(new ModelImpl(input2, m2Location), ct);
        split("Neo4j - M2");
        final Response checkin3 = repository.checkin(new ModelImpl(input3, m1Location), ct);
        split("Neo4j - M1");
        final Response commitResponse = repository.commitTransaction(ct);
        split("Neo4j - Commit");
        // then
        assertTrue(checkin instanceof CheckinResponse);
        // all models should be known to the corresponding revision
        final Revision revision = repository.getRevision(commitResponse.getURI());
        final Collection<URI> modelReferences = revision.getModelReferences();

        assertEquals(3, modelReferences.size());
        assertTrue(modelReferences.contains(checkin.getURI()));
        assertTrue(modelReferences.contains(checkin2.getURI()));
        assertTrue(modelReferences.contains(checkin3.getURI()));

        split("Accessing model references of the last revision");
        // storeViaXml(input, input2, input3);
        final Model checkedoutmodel = repository.checkout(checkin3.getURI());
        split("Restoring M1");
        assertNotNull(checkedoutmodel.getContent());
        storeViaXml(checkedoutmodel.getContent());
        split("Writing XML");
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
    }
}
