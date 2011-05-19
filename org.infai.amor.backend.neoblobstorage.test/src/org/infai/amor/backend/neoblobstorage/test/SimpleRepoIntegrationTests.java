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
import org.infai.amor.backend.api.SimpleRepository;
import org.infai.amor.backend.impl.RepositoryImpl;
import org.infai.amor.backend.impl.SimpleRepositoryImpl;
import org.infai.amor.backend.internal.TransactionManager;
import org.infai.amor.backend.internal.UriHandler;
import org.infai.amor.backend.internal.impl.*;
import org.infai.amor.backend.neo.NeoProvider;
import org.infai.amor.backend.neostorage.NeoBlobStorageFactory;
import org.infai.amor.test.AbstractNeo4JTest;
import org.infai.amor.test.ModelUtil;
import org.junit.*;
import org.neo4j.graphdb.GraphDatabaseService;

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
        // assertEquals(expected.replaceAll("^ +", ""), actual.replaceAll("^ +", ""));
        assertEquals(expected.replaceAll("\\s+", ""), actual.replaceAll("\\s+", ""));
    }
    /**
     * @param string
     */
    private void checkin(final String path) {
        final String file = ModelUtil.readModel(path);
        repo.checkin(file, path, trId);
    }

    // @Override
    // protected boolean isRollbackAfterTest() {
    // return false;
    // }

    /* (non-Javadoc)
     * @see org.infai.amor.test.AbstractNeo4JTest#isRollbackAfterTest()
     */
    @Override
    protected boolean isRollbackAfterTest() {
        return true;
    }
    @Before
    public void setup() throws Exception {
        final UriHandler uh = new UriHandlerImpl("localhost", "repo");
        final NeoProvider np = new NeoProvider() {

            @Override
            public GraphDatabaseService getNeo() {
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
    }
    @Test
    public void shouldAskForDependency() throws Exception {
        // given
        final String ecore = ModelUtil.readModel("testmodels/multi/A.ecore");
        // when
        final List<String> missing = repo.checkin(ecore, "testmodel/multi/A.ecore", trId);
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
        final List<String> missing = repo.checkin(model, path, trId);
        // then
        assertFalse(missing.isEmpty());
        assertEquals("http://mymodels.org/packageB", missing.get(0));
    }
    @Test
    public void shouldCheckinComplexModel() throws Exception {
        // given
        checkin("testmodels/02/primitive_types.ecore");
        checkin("testmodels/02/java.ecore");
        // when
        final List<String> missing = repo.checkin(ModelUtil.readModel("testmodels/02/Hello.java.xmi"), "testmodels/02/Hello.java.xmi", trId);
        // then
        assertFalse(missing.isEmpty());
    }

    @Test
    public void shouldCheckinDependencies() throws Exception {
        // given
        final String ecoreB = ModelUtil.readModel("testmodels/multi/B.ecore");
        final String ecoreA = ModelUtil.readModel("testmodels/multi/A.ecore");
        // when
        final List<String> missing1 = repo.checkin(ecoreB, "testmodel/multi/B.ecore", trId);
        final List<String> missing2 = repo.checkin(ecoreA, "testmodel/multi/A.ecore", trId);
        // then
        assertTrue(missing1.isEmpty());
        assertTrue(missing2.isEmpty());
    }

    @Test
    public void shouldCheckinEEnumLiterals() throws Exception {
        // given
        final String ecore = ModelUtil.readModel("testmodels/video/videothek.ecore");
        // when
        final List<String> missing = repo.checkin(ecore, "testmodel/video/videothek.ecore", trId);
        repo.checkin(ModelUtil.readModel("testmodels/video/Beispiel.videothek"), "Beispiel.videothek", trId);
        final long revId = repo.commitTransaction(trId, "testuser", "added videothek example");
        // then
        assertTrue(missing.isEmpty());
    }
    @Test
    public void shouldCheckinPatch() throws Exception {
        // given
        // a checked in metamodel
        final String ecore = ModelUtil.readModel("testmodels/filesystem.ecore");
        repo.checkin(ecore, "filesystem.ecore", trId);
        // and an instance model
        final String initialModel = ModelUtil.readModel("testmodels/fs/simplefilesystem_v1.filesystem");
        repo.checkin(initialModel, "simplefilesystem.xmi", trId);
        long revId1 = repo.commitTransaction(trId, "user", "added inital filesystem model");
        // active contents should contain the instance model
        assertTrue(repo.getActiveContents("amor://localhost/repo/" + BRANCHNAME + "/" + revId1).contains("amor://localhost/repo/main/1/simplefilesystem.xmi"));
        // when
        // checking in an epatch
        trId = repo.startTransaction(BRANCHNAME);
        repo.checkinPatch(ModelUtil.readModel("testmodels/fs/v1-v2.epatch"), "simplefilesystem.xmi", trId);
        final long revId2 = repo.commitTransaction(trId, "user", "added changed model");
        // make sure the active contents contain revision id 2 for the updated model
        assertTrue(repo.getActiveContents("amor://localhost/repo/" + BRANCHNAME + "/" + revId2).contains("amor://localhost/repo/main/2/simplefilesystem.xmi"));

        // and checking out the most recent version of the instance model
        final String checkout = repo.checkout(BRANCHNAME, revId2, "simplefilesystem.xmi");
        // then
        assertEqualsIgnoringWhitespace(ModelUtil.readModel("testmodels/fs/simplefilesystem_v2.filesystem"), checkout);
    }

    @Test
    public void shouldCheckinSimpleM2() throws Exception {
        // given
        final String ecore = ModelUtil.readModel("testmodels/base.ecore");
        // when
        final List<String> missing = repo.checkin(ecore, "testmodel/base.ecore", trId);
        final long revId = repo.commitTransaction(trId, "testuser", "added simple meta model");
        // then
        assertTrue(missing.isEmpty());
        assertTrue(1 <= revId);
    }

    @Test
    @Ignore(value = "Comparing XML strings is not sufficient. See JavaEcoreTest.java for same test")
    public void shouldRestoreComplexEcore() throws Exception {
        // given
        final String ecoreB = ModelUtil.readModel("testmodels/02/primitive_types.ecore");
        final String ecoreA = ModelUtil.readModel("testmodels/02/java.ecore");
        // when
        repo.checkin(ecoreB, "primitive_types.ecore", trId);
        repo.checkin(ecoreA, "java.ecore", trId);
        final long revisionId = repo.commitTransaction(trId, "user", "bla");

        final String ecoreXml = repo.checkout(BRANCHNAME, revisionId, "java.ecore");
        // then
        assertNotNull(ecoreXml);
        assertEqualsIgnoringWhitespace(ecoreA, ecoreXml);
    }

    @Test
    public void shouldRestoreDOEcore() throws Exception {
        // given
        final String cdoecore = ModelUtil.readModel("testmodels/cdo.ecore");
        // when
        repo.checkin(cdoecore, "cdo.ecore", trId);
        final long revisionId = repo.commitTransaction(trId, "user", "bla");

        final String ecoreXml = repo.checkout(BRANCHNAME, revisionId, "cdo.ecore");
        // then
        assertNotNull(ecoreXml);
        assertEqualsIgnoringWhitespace(cdoecore, ecoreXml);
    }

    @Test
    public void shouldRestoreModelWithDependency() throws Exception {
        // given
        final String ecoreB = ModelUtil.readModel("testmodels/multi/B.ecore");
        final String ecoreA = ModelUtil.readModel("testmodels/multi/A.ecore");
        // when
        final List<String> missing1 = repo.checkin(ecoreB, "testmodel/multi/B.ecore", trId);
        final List<String> missing2 = repo.checkin(ecoreA, "testmodel/multi/A.ecore", trId);
        final long revisionId = repo.commitTransaction(trId, "user", "bla");

        final String ecoreXml = repo.checkout(BRANCHNAME, revisionId, "testmodel/multi/A.ecore");
        // then
        System.out.println(ecoreXml);
        assertNotNull(ecoreXml);
        // FIXME comparing via text equality is stupid...
        // assertEqualsIgnoringWhitespace(ecoreA, ecoreXml);
    }

    @Test
    public void shouldStoreInstanceModel() throws Exception {
        // given
        checkin("testmodels/multi/B.ecore");
        checkin("testmodels/multi/A.ecore");
        final String path = "testmodels/multi/b.xmi";
        final String model = ModelUtil.readModel(path);
        // when
        final List<String> missing = repo.checkin(model, path, trId);
        // then
        assertTrue(missing.isEmpty());
    }

    @Test
    public void shouldStoreInstanceModelInLaterRevision() throws Exception {
        // given
        checkin("testmodels/bflow/bflow.ecore");
        checkin("testmodels/bflow/oepc.ecore");
        repo.commitTransaction(trId, "foo", "bar");
        this.trId = repo.startTransaction(BRANCHNAME);

        final String path = "testmodels/bflow/sample.xmi";
        final String model = ModelUtil.readModel(path);
        // when
        final List<String> missing = repo.checkin(model, path, trId);
        // then
        assertTrue(missing.isEmpty());
    }
}
