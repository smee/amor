/*******************************************************************************
 * Copyright (c) 2010 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.neoblobstorage.test;

import static org.junit.Assert.*;

import java.util.List;

import org.infai.amor.backend.Repository;
import org.infai.amor.backend.SimpleRepository;
import org.infai.amor.backend.impl.RepositoryImpl;
import org.infai.amor.backend.impl.SimpleRepositoryImpl;
import org.infai.amor.backend.internal.*;
import org.infai.amor.backend.internal.impl.*;
import org.infai.amor.backend.neostorage.NeoBlobStorageFactory;
import org.infai.amor.test.AbstractNeo4JTest;
import org.infai.amor.test.ModelUtil;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.api.core.NeoService;

/**
 * @author sdienst
 *
 */
public class SimpleRepoIntegrationTests extends AbstractNeo4JTest {
    private static final String BRANCHNAME = "main";
    private SimpleRepository repo;
    private long trId;

    /**
     * Strip all whitespaces and line wrappings before comparing both strings.
     * 
     * @param expected
     * @param actual
     */
    private static void assertEqualsIgnoringWhitespace(final String expected, final String actual){
        assertEquals(expected.replaceAll("\\s+", ""),actual.replaceAll("\\s+", ""));
    }

    /**
     * @param string
     */
    private void checkin(final String path) {
        final String file = ModelUtil.readModel(path);
        repo.checkinEcore(file, path, trId);
    }

    private void checkinEcoreM3() throws Exception {
        // given
        final String ecore = ModelUtil.readModel("testmodels/Ecore.ecore");
        // when
        final List<String> missing = repo.checkinEcore(ecore, "testmodel/Ecore.ecore", trId);
        // then
        assertTrue(missing.isEmpty());
    }

    @Before
    public void setup() throws Exception {
        final UriHandler uh = new UriHandlerImpl("localhost", "repo");
        final NeoProvider np = new NeoProvider() {

            @Override
            public NeoService getNeo() {
                return neoservice;
            }
        };
        final NeoBranchFactory bf = new NeoBranchFactory(np);
        final TransactionManager tm = new TransactionManagerImpl(uh, np, bf);
        final NeoBlobStorageFactory sf = new NeoBlobStorageFactory(np);
        tm.addTransactionListener(sf);

        final Repository repository = new RepositoryImpl(sf, bf, uh, tm);
        repo = new SimpleRepositoryImpl(repository, uh);

        repo.createBranch(BRANCHNAME, null, -1);
        trId = repo.startTransaction(BRANCHNAME);
        checkinEcoreM3();
    }
    @Test
    public void shouldAskForDependency() throws Exception {
        // given
        final String ecore = ModelUtil.readModel("testmodels/multi/A.ecore");
        // when
        final List<String> missing = repo.checkinEcore(ecore, "testmodel/multi/A.ecore", trId);
        // then
        assertEquals(1, missing.size());
        assertEquals("B.ecore", missing.get(0));
    }
    @Test
    public void shouldAskForUnknownMetamodel() throws Exception {
        // given
        final String path = "testmodels/multi/b.xmi";
        final String model = ModelUtil.readModel(path);
        // when
        final List<String> missing = repo.checkinEcore(model, path, trId);
        // then
        assertFalse(missing.isEmpty());
        assertEquals("http://mymodels.org/packageB", missing.get(0));
    }

    @Test
    public void shouldCheckinDependencies() throws Exception {
        // given
        final String ecoreB = ModelUtil.readModel("testmodels/multi/B.ecore");
        final String ecoreA = ModelUtil.readModel("testmodels/multi/A.ecore");
        // when
        final List<String> missing1 = repo.checkinEcore(ecoreB, "testmodel/multi/B.ecore", trId);
        final List<String> missing2 = repo.checkinEcore(ecoreA, "testmodel/multi/A.ecore", trId);
        // then
        assertTrue(missing1.isEmpty());
        assertTrue(missing2.isEmpty());
    }

    @Test
    public void shouldCheckinSimpleM2() throws Exception {
        // given
        final String ecore = ModelUtil.readModel("testmodels/base.ecore");
        // when
        final List<String> missing = repo.checkinEcore(ecore, "testmodel/base.ecore", trId);
        final long revId = repo.commitTransaction(trId, "testuser", "added simple meta model");
        // then
        assertTrue(missing.isEmpty());
        assertTrue(1 <= revId);
    }

    @Test
    public void shouldRestoreModelWithDependency() throws Exception {
        // given
        final String ecoreB = ModelUtil.readModel("testmodels/multi/B.ecore");
        final String ecoreA = ModelUtil.readModel("testmodels/multi/A.ecore");
        // when
        final List<String> missing1 = repo.checkinEcore(ecoreB, "testmodel/multi/B.ecore", trId);
        final List<String> missing2 = repo.checkinEcore(ecoreA, "testmodel/multi/A.ecore", trId);
        final long revisionId = repo.commitTransaction(trId, "user", "bla");

        final String ecoreXml = repo.checkoutEcore(BRANCHNAME, revisionId, "testmodel/multi/A.ecore");
        // then
        System.out.println(ecoreXml);
        assertNotNull(ecoreXml);
        assertEqualsIgnoringWhitespace(ecoreA, ecoreXml);
    }

    @Test
    public void shouldStoreInstanceModel() throws Exception {
        // given
        checkin("testmodels/multi/B.ecore");
        checkin("testmodels/multi/A.ecore");
        final String path = "testmodels/multi/b.xmi";
        final String model = ModelUtil.readModel(path);
        // when
        final List<String> missing = repo.checkinEcore(model, path, trId);
        // then
        assertTrue(missing.isEmpty());
    }
}
