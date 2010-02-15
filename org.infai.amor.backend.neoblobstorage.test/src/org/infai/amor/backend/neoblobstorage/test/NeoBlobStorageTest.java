/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.neoblobstorage.test;

import static org.infai.amor.test.ModelUtil.readInputModels;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.infai.amor.backend.*;
import org.infai.amor.backend.Revision.ChangeType;
import org.infai.amor.backend.impl.RepositoryImpl;
import org.infai.amor.backend.internal.*;
import org.infai.amor.backend.internal.impl.*;
import org.infai.amor.backend.neostorage.NeoBlobStorageFactory;
import org.infai.amor.backend.responses.CheckinResponse;
import org.infai.amor.backend.util.EcoreModelHelper;
import org.infai.amor.test.AbstractNeo4JPerformanceTest;
import org.infai.amor.test.ModelUtil;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.neo4j.api.core.NeoService;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * @author sdienst
 * 
 */
@RunWith(Parameterized.class)
public class NeoBlobStorageTest extends AbstractNeo4JPerformanceTest {
    private static final Logger logger = Logger.getLogger(NeoBlobStorageTest.class.getName());
    private Repository repository;
    private CountingNeoProxy proxy;
    private final String[] modelLocations;

    /**
     * Run one roundtrip for every pair of m2/m1 model.
     * 
     * @return
     * @throws InterruptedException
     */
    @Parameters
    public static Collection<Object[]> getTestParameters() {
        final List<String[]> testdata = Lists.newArrayList();
        // testdata.add(new String[] { "testmodels/filesystem.ecore", "testmodels/simplefilesystem.xmi" });
        // testdata.add(new String[] { "testmodels/filesystem.ecore", "testmodels/fs/simplefilesystem_v1.filesystem" });
        // CAUTION: takes some time, huge model!
        // testdata.add(new String[] { "testmodels/aris.ecore", "testmodels/model_partial.xmi" });
        testdata.add(new String[] { "testmodels/02/primitive_types.ecore", "testmodels/02/java.ecore", "testmodels/02/Hello.java.xmi" });
        // testdata.add(new String[] { "testmodels/multi/B.ecore", "testmodels/multi/A.ecore", "testmodels/multi/a.xmi" });

        final Collection<Object[]> params = Lists.newArrayList();
        for(final String[] data: testdata) {
            params.add(new Object[] {data});
        }
        return params;
    }

    /**
     * @param modelLocations
     */
    public NeoBlobStorageTest(final String... m) {
        this.modelLocations = m;
    }

    @Override
    protected boolean isRollbackAfterTest() {
        return false;
    }

    @After
    public void printStatistics() {
        logger.info(String.format("# nodes created: %d, # relationships created: %d, # properties set: %d", proxy.getNumNodes(), proxy.getNumRels(), proxy.getNumProps()));
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
        split(String.format("Before loading %d models ", modelLocations.length, Arrays.asList(modelLocations)));
        // given
        final ResourceSet rs = new ResourceSetImpl();
        final Map<String, List<EObject>> models = Maps.newLinkedHashMap();
        models.put("testmodels/Ecore.ecore", readInputModels("testmodels/Ecore.ecore", rs, true));
        for (final String location : modelLocations) {
            final List<EObject> currentModelContents = readInputModels(location, rs, true);
            for (final EObject eo : currentModelContents) {
                final Set<URI> proxyUrls = EcoreModelHelper.findReferencedModels(eo, eo.eResource().getURI());
                if (!proxyUrls.isEmpty()) {
                    System.out.println("Unresolved proxies: " + proxyUrls);
                    // TODO restore from repo or ask for these dependencies
                }
            }
            models.put(location, currentModelContents);
        }
        split("After loading models");

        final Branch branch = repository.createBranch(null, "trunk");
        final CommitTransaction ct = repository.startCommitTransaction(branch);
        ct.setCommitMessage("test");
        ct.setUser("mustermann");
        // when
        // model checked in successfully
        final List<URI> repoUris = Lists.newArrayList();
        for (final String loc : models.keySet()) {
            final Response response = repository.checkin(new ModelImpl(models.get(loc), loc), ct);
            assertTrue("Class is " + response.getClass(), response instanceof CheckinResponse);

            repoUris.add(response.getURI());
            split("Neo4j - Added "+loc);
        }
        final Response commitResponse = repository.commitTransaction(ct);
        // then
        for (final Resource res : rs.getResources()) {
            res.unload();
        }
        // all models should be known to the corresponding revision
        final Revision revision = repository.getRevision(commitResponse.getURI());
        final Collection<ModelLocation> modelReferences = revision.getModelReferences(ChangeType.ADDED);

        assertEquals(models.size(), modelReferences.size());
        // for(final URI uri: repoUris) {
        // assertTrue(modelReferences.contains(uri));
        // }

        split("Accessing model references of the last revision");
        // storeViaXml(input, input2, input3);

        final Model checkedoutmodel = repository.checkout(repoUris.get(repoUris.size() - 1));
        split("Restoring last checked in model");
        assertNotNull(checkedoutmodel.getContent());
        ModelUtil.storeViaXml(checkedoutmodel.getContent(), checkedoutmodel.getPersistencePath().toString());

        split("Writing XML");

        // ModelUtil.assertModelEqual(models.get(this.modelLocations[modelLocations.length - 1]).get(0),
        // checkedoutmodel.getContent().get(0));
    }
}
