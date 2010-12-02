/*******************************************************************************
 * Copyright (c) 2010 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.neoblobstorage.codeasmodels;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.*;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
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
 * @author sdienst TODO adjust amorresource, package http://www.emftext.org/java/types, class TypedElement, ereference "type"
 *         needs to be called typeReference
 */
public class CodeHistoryTest extends AbstractNeo4JTest {
    /**
     * @author sdienst
     * 
     */
    static class CommitInfo {
        Properties p;

        public CommitInfo(String classpathLocation) throws IOException {
            p = new Properties();
            p.load(this.getClass().getClassLoader().getResourceAsStream(classpathLocation));
        }

        String getAuthor() {
            return p.getProperty("author");
        }

        String getBranch(){
            return p.getProperty("branch").replace('/', '_');
        }

        String getId(){
            return p.getProperty("id");
        }

        String getMessage() {
            return p.getProperty("message");
        }

        String getParentId() {
            return p.getProperty("parentid");
        }
    }

    private static final String MAINBRANCH = "mainbranch";
    private SimpleRepository repo;
    private long lastRevId;
    /**
     * @param ci
     * @param rev
     * @param branchHeads
     * @throws Exception
     */
    private void checkin(CommitInfo ci, int rev, Map<String, CommitInfo> parents) throws Exception {
        final String patch = loadPatch(rev);

        CommitInfo parent = parents.get(ci.getParentId());
        if (!parent.getBranch().equals(ci.getBranch())) {
            // new subbranch
            repo.createBranch(ci.getBranch(), parent.getBranch(), rev - 1);
        }

        long trId = repo.startTransaction(ci.getBranch());
        repo.checkinPatch(patch, "testmodels/codehistory/SystemUtils.java.xmi", trId);
        long revId = commit(trId, ci);
    }

    /**
     * @param string
     * @return
     * @throws Exception
     */
    private long checkin(final String path) throws Exception {
        final String file = ModelUtil.readModel(path);
        repo.createBranch(MAINBRANCH, null, -1);
        long trId = repo.startTransaction(MAINBRANCH);
        repo.checkin(file, path, trId);
        try {
            return repo.commitTransaction(trId, "testuser", "added metamodel " + path);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
            return -1;
        }
    }

    /**
     * @param loadCommit
     * @param string
     * @param branchHeads
     * @return
     * @throws Exception
     */
    private long checkinInitial(CommitInfo ci, String path) throws Exception {
        final String file = ModelUtil.readModel(path);
        repo.createBranch(ci.getBranch(), MAINBRANCH, lastRevId);

        long trId = repo.startTransaction(ci.getBranch());
        List<String> missing = repo.checkin(file, path, trId);
        System.out.println(missing);
        assertTrue(missing.isEmpty());

        long revId = commit(trId, ci);
        return revId;
    }

    /**
     * @param trId
     * @param ci
     * @return revision id
     */
    private long commit(long trId, CommitInfo ci) {
        try {
            return repo.commitTransaction(trId, ci.getAuthor(), ci.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
            throw new AssertionError("will never reach this code.");
        }

    }

    /**
     * @param branch
     * @param branches
     * @return
     */
    private boolean isNewBranch(String branch, String[] branches) {
        Set<String> set = new HashSet<String>(Arrays.asList(branches));
        return !set.contains(branch);
    }

    /* (non-Javadoc)
     * @see org.infai.amor.test.AbstractNeo4JTest#isRollbackAfterTest()
     */
    @Override
    protected boolean isRollbackAfterTest() {
        return false;
    }

    /**
     * @param i
     * @return
     * @throws IOException
     */
    private CommitInfo loadCommit(int idx) throws IOException {
        String num = "" + ((idx < 10) ? "0" + idx : idx);
        return new CommitInfo("testmodels/codehistory/commits/SystemUtils.java."+num+".properties");
    }

    private String loadPatch(int idx) {
        String num = "" + ((idx < 10) ? "0" + idx : idx);
        return ModelUtil.readModel("testmodels/codehistory/patches/SystemUtils.java." + num + ".epatch");
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
        this.lastRevId = checkin("testmodels/codehistory/javaGen.ecore");

    }

    @Test
    @Ignore
    public void shouldLoadJavaInstances() throws Exception {
        // given
        ResourceSet rs = new ResourceSetImpl();
        ModelUtil.readInputModels("testmodels/codehistory/javaGen.ecore", rs);
        ModelUtil.readInputModels("testmodels/codehistory/SystemUtils.java.xmi", rs);
        // when

        // then
    }

    @Test
    public void shouldPersistSimpleCodeHistory() throws Exception {
        // given
        Map<String, CommitInfo> parents = new HashMap<String, CommitInfo>();

        // when
        CommitInfo firstCommit = loadCommit(0);
        parents.put(firstCommit.getId(), firstCommit);

        checkinInitial(firstCommit, "testmodels/codehistory/SystemUtils.java.xmi");

        int rev = 0;
        while (rev++ <= 59) {
            CommitInfo ci = loadCommit(rev);
            checkin(ci, rev, parents);
        }

        // then
    }

    // @Test
    // @Ignore
    // public void shouldSaveGeneratedJavaPackage() throws Exception {
    // ModelUtil.storeViaXml((List) JavaPackage.eINSTANCE.getESubpackages(), "javaGen.ecore");
    // }

}
