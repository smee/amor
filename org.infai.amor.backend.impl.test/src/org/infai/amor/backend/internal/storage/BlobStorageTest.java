/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.internal.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.custommonkey.xmlunit.XMLAssert;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.diff.metamodel.DiffModel;
import org.eclipse.emf.compare.diff.service.DiffService;
import org.eclipse.emf.compare.epatch.Epatch;
import org.eclipse.emf.compare.epatch.diff.DiffEpatchService;
import org.eclipse.emf.compare.match.metamodel.MatchModel;
import org.eclipse.emf.compare.match.service.MatchService;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.infai.amor.ModelUtil;
import org.infai.amor.backend.Branch;
import org.infai.amor.backend.ChangedModel;
import org.infai.amor.backend.CommitTransaction;
import org.infai.amor.backend.Model;
import org.infai.amor.backend.exception.TransactionException;
import org.infai.amor.backend.impl.CommitTransactionImpl;
import org.infai.amor.backend.internal.impl.ChangedModelImpl;
import org.infai.amor.backend.internal.impl.ModelImpl;
import org.infai.amor.backend.internal.impl.NeoRevision;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * @author sdienst
 * 
 */
public class BlobStorageTest {

    /**
     * 
     */
    private static final String BRANCHNAME = "testBranch";
    private BlobStorage storage;
    private Mockery context;
    private File tempDir;
    private NeoRevision rev;

    /**
     * @throws IOException
     * @throws TransactionException
     */
    private void createSomeRevisions() throws IOException, TransactionException {
        final Model m = new ModelImpl(ModelUtil.readInputModel("testmodels/base.ecore"), "testmodels/foo.ecore");
        // given several older revisions
        // rev 33
        CommitTransaction tr = createTransaction(BRANCHNAME, 33);
        storage.startTransaction(tr);
        storage.checkin(m, null, tr.getRevisionId());
        storage.commit(tr, rev);
        // rev 55
        tr = createTransaction(BRANCHNAME, 55);
        storage.startTransaction(tr);
        storage.checkin(new ModelImpl(m.getContent(), "testmodel/base.ecore"), null, tr.getRevisionId());
        storage.commit(tr, rev);
        // rev 88
        tr = createTransaction(BRANCHNAME, 88);
        storage.startTransaction(tr);
        storage.checkin(new ModelImpl(m.getContent(), "testmodel/foo.ecore"), null, tr.getRevisionId());
        storage.commit(tr, rev);
        // rev 99
        tr = createTransaction(BRANCHNAME, 99);
        storage.startTransaction(tr);
        storage.checkin(new ModelImpl(m.getContent(), "testmodel/foo.ecore"), null, tr.getRevisionId());
        storage.commit(tr, rev);
    }
    private CommitTransaction createTransaction(final String branchname, final long revisionId) {
        final Branch branch = context.mock(Branch.class, "" + Math.random());
        context.checking(new Expectations() {
            {
                allowing(branch).getName();
                will(returnValue(BRANCHNAME));
            }
        });
        return new CommitTransactionImpl(branch, revisionId, null);
    }
    @Before
    public void setUp() throws IOException {
        tempDir = File.createTempFile("storage", "temp");
        tempDir.delete();
        tempDir.mkdirs();

        storage = new BlobStorage(tempDir, BRANCHNAME);
        context = new Mockery() {{ setImposteriser(ClassImposteriser.INSTANCE); }};
        rev = context.mock(NeoRevision.class);
        context.checking(new Expectations() {{ allowing(rev); }});
    }

    @Test
    public void shouldEqualSavedAndRestoredModel() throws Exception {
        // given
        final Model m = new ModelImpl(ModelUtil.readInputModel("testmodels/base.ecore"), "testmodels/base.ecore");
        final CommitTransaction tr = createTransaction(BRANCHNAME, 88);

        // when
        storage.startTransaction(tr);
        storage.checkin(m, null, tr.getRevisionId());
        final Model checkedout = storage.checkout(m.getPersistencePath(), tr.getRevisionId());

        // then
        // TODO compare via emfcompare or xmlunit
        ModelUtil.assertModelEqual(m.getContent(), checkedout.getContent());
    }

    @Test
    public void shouldFindMostRecentParentModel() throws Exception {
        createSomeRevisions();
        // when
        final CommitTransaction tr = createTransaction(BRANCHNAME, 100);
        storage.startTransaction(tr);
        // final ChangedModel cm = new ChangedModelImpl(null, "testmodels/base.ecore");
        final ResourceSet rs = storage.findMostRecentModelFor(new Path("testmodel/base.ecore"));

        // then
        assertEquals(1, rs.getResources().size());
        assertTrue(rs.getResources().get(0).getURI().toString().endsWith("55/testmodel/base.ecore"));
    }

    @Test
    public void shouldFindMostRecentParentModel2() throws Exception {
        createSomeRevisions();
        // when
        final CommitTransaction tr = createTransaction(BRANCHNAME, 100);
        storage.startTransaction(tr);
        // final ChangedModel cm = new ChangedModelImpl(null, "testmodels/base.ecore");
        final ResourceSet rs = storage.findMostRecentModelFor(new Path("testmodel/foo.ecore"));

        // then
        assertEquals(1, rs.getResources().size());
        System.out.println(rs.getResources().get(0).getURI());
        assertTrue(rs.getResources().get(0).getURI().toString().endsWith("99/testmodel/foo.ecore"));
    }

    @Test
    public void shouldSaveModelWithoutChanges() throws IOException, SAXException {
        // read model
        final Model m = new ModelImpl(ModelUtil.readInputModel("testmodels/base.ecore"), "testmodels/base.ecore");
        // start commit transaction
        final CommitTransaction tr = createTransaction(BRANCHNAME, 55);
        storage.startTransaction(tr);
        // store it into branch testBranch and revision 55
        storage.checkin(m, null, tr.getRevisionId());

        final File storedFile = new File(tempDir, "testBranch/55/testmodels/base.ecore");
        assertTrue(storedFile.exists());

        // compare both models as xml documents
        XMLAssert.assertXMLEqual(new BufferedReader(new FileReader("testmodels/base.ecore")), new BufferedReader(new FileReader(storedFile)));
    }

    @Test
    public void shouldStoreChangedModels() throws Exception {
        // given
        final ResourceSet rs = new ResourceSetImpl();
        // checked in metamodel
        final Model mm = new ModelImpl(ModelUtil.readInputModel("testmodels/filesystem.ecore", rs), "testmodels/filesystem.ecore");
        CommitTransaction tr = createTransaction(BRANCHNAME, 1);
        storage.startTransaction(tr);
        storage.checkin(mm, null, tr.getRevisionId());
        storage.commit(tr, rev);
        // given several older revisions
        final String modelpath = "model/simplefilesystem.xml";
        final Model m = new ModelImpl(ModelUtil.readInputModel("testmodels/fs/simplefilesystem_v1.filesystem", rs), modelpath);
        // rev 33
        tr = createTransaction(BRANCHNAME, 33);
        storage.startTransaction(tr);
        storage.checkin(m, null, tr.getRevisionId());
        storage.commit(tr, rev);
        // rev 55
        tr = createTransaction(BRANCHNAME, 55);
        storage.startTransaction(tr);

        // when changing model
        final EObject changedModel = ModelUtil.readInputModel("testmodels/fs/simplefilesystem_v2.filesystem", rs);
        final MatchModel match = MatchService.doMatch(m.getContent(), changedModel, null);
        final DiffModel diff = DiffService.doDiff(match, false);
        final Epatch epatch = DiffEpatchService.createEpatch(match, diff, "testpatch");
        final ChangedModel cm = new ChangedModelImpl(epatch, modelpath);

        storage.checkin(cm, null, 55);
        storage.commit(tr, rev);
        // then ??

    }

    @Test
    public void testCreatesLocalDirectories() {
        final CommitTransaction tr = createTransaction(BRANCHNAME, 55);
        final URI fileUri = storage.createStorageUriFor(new Path("testmodels/dummymodel.xmi"), tr.getRevisionId(), false);
        assertEquals(new File(tempDir, "testBranch/55/testmodels").toURI().toString(), fileUri.toString());
    }
}
