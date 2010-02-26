/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.filestorage;

import static org.infai.amor.test.TestUtils.createTransaction;
import static org.junit.Assert.*;

import java.io.*;

import org.custommonkey.xmlunit.XMLAssert;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.infai.amor.backend.*;
import org.infai.amor.backend.exception.TransactionException;
import org.infai.amor.backend.internal.ModelImpl;
import org.infai.amor.backend.internal.impl.ChangedModelImpl;
import org.infai.amor.backend.internal.impl.NeoRevision;
import org.infai.amor.test.ModelUtil;
import org.infai.amor.test.TestUtils;
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
    private FileBlobStorage storage;
    private Mockery context;
    private File tempDir;
    private InternalRevision rev;

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
        storage.checkin(m, null, tr.getRevision());
        storage.commit(tr);
        // rev 55
        tr = createTransaction(BRANCHNAME, 55);
        storage.startTransaction(tr);
        storage.checkin(new ModelImpl(m.getContent(), "testmodel/base.ecore"), null, tr.getRevision());
        storage.commit(tr);
        // rev 88
        tr = createTransaction(BRANCHNAME, 88);
        storage.startTransaction(tr);
        storage.checkin(new ModelImpl(m.getContent(), "testmodel/foo.ecore"), null, tr.getRevision());
        storage.commit(tr);
        // rev 99
        tr = createTransaction(BRANCHNAME, 99);
        storage.startTransaction(tr);
        storage.checkin(new ModelImpl(m.getContent(), "testmodel/foo.ecore"), null, tr.getRevision());
        storage.commit(tr);
    }

    @Before
    public void setUp() throws IOException {
        tempDir = File.createTempFile("storage", "temp");
        tempDir.delete();
        tempDir.mkdirs();

        storage = new FileBlobStorage(tempDir, BRANCHNAME);
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
        storage.checkin(m, null, tr.getRevision());
        final Model checkedout = storage.checkout(m.getPersistencePath(), tr.getRevision());

        // then
        ModelUtil.assertModelEqual(m.getContent().get(0), checkedout.getContent().get(0));
    }

    @Test
    public void shouldFindMostRecentParentModel() throws Exception {
        createSomeRevisions();
        // when
        final CommitTransaction tr = createTransaction(BRANCHNAME, 100);
        storage.startTransaction(tr);
        // final ChangedModel cm = new ChangedModelImpl(null, "testmodels/base.ecore");
        final ResourceSet rs = storage.findMostRecentModelFor(new Path("testmodel/base.ecore"), 100);

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
        final ResourceSet rs = storage.findMostRecentModelFor(new Path("testmodel/foo.ecore"), 100);

        // then
        assertEquals(1, rs.getResources().size());
        System.out.println(rs.getResources().get(0).getURI());
        assertTrue(rs.getResources().get(0).getURI().toString().endsWith("99/testmodel/foo.ecore"));
    }

    @Test
    public void shouldFindPackageNameViaXPath() throws Exception {
        // given
        final File file = new File("../org.infai.backend.tests/src/testmodels/fs/simplefilesystem_v1.filesystem");
        // when
        final String nsuri =storage.getM2Uri(file);
        // then
        assertEquals("http://filesystem",nsuri);
    }

    @Test
    public void shouldNotFindDifferences() throws IOException{
        final ResourceSet rs = new ResourceSetImpl();
        ModelUtil.readInputModel("testmodels/filesystem.ecore", rs);
        final EObject sfs1 = ModelUtil.readInputModel("testmodels/encoding/sfs_ascii.filesystem", rs);
        final EObject sfs2 = ModelUtil.readInputModel("testmodels/encoding/sfs_utf8.filesystem", rs);

        ModelUtil.assertModelEqual(sfs1, sfs2);
    }

    @Test
    public void shouldSaveAndRestoreM2ModelWithoutChanges() throws IOException, SAXException {
        // store the model
        shouldSaveModelWithoutChanges();

        final Model model = storage.checkout(new Path("testmodels/base.ecore"), TestUtils.createRevision(55));
        assertNotNull(model.getContent());
    }

    @Test
    public void shouldSaveModelWithoutChanges() throws IOException, SAXException {
        // read model
        final Model m = new ModelImpl(ModelUtil.readInputModel("testmodels/base.ecore"), "testmodels/base.ecore");
        // start commit transaction
        final CommitTransaction tr = createTransaction(BRANCHNAME, 55);
        storage.startTransaction(tr);
        // store it into branch testBranch and revision 55
        storage.checkin(m, null, tr.getRevision());

        final File storedFile = new File(tempDir, "testBranch/55/testmodels/base.ecore");
        assertTrue(storedFile.exists());

        // compare both models as xml documents
        XMLAssert.assertXMLEqual(new BufferedReader(new FileReader("../org.infai.backend.tests/src/testmodels/base.ecore")), new BufferedReader(new FileReader(storedFile)));
    }

    @Test
    public void shouldStoreChangedModels() throws Exception {
        // given
        final ResourceSet rs = new ResourceSetImpl();
        // checked in metamodel
        final Model mm = new ModelImpl(ModelUtil.readInputModel("testmodels/filesystem.ecore", rs), "testmodels/filesystem.ecore");
        CommitTransaction tr = createTransaction(BRANCHNAME, 1);
        storage.startTransaction(tr);
        storage.checkin(mm, null, tr.getRevision());
        storage.commit(tr);
        // given several older revisions
        final String modelpath = "model/simplefilesystem.filesystem";
        final Model m = new ModelImpl(ModelUtil.readInputModel("testmodels/fs/simplefilesystem_v1.filesystem", rs), modelpath);
        // rev 33
        tr = createTransaction(BRANCHNAME, 33);
        storage.startTransaction(tr);
        storage.checkin(m, null, tr.getRevision());
        storage.commit(tr);
        // rev 55
        tr = createTransaction(BRANCHNAME, 55);
        storage.startTransaction(tr);

        // when changing model
        final EObject changedModel = ModelUtil.readInputModel("testmodels/fs/simplefilesystem_v2.filesystem", rs);
        final ChangedModel cm = new ChangedModelImpl(ModelUtil.createEpatch(m.getContent().get(0), changedModel), modelpath);

        storage.checkin(cm, null, tr.getRevision());
        storage.commit(tr);
        // then ??
        final Model checkedoutmodel = storage.checkout(new Path(modelpath), tr.getRevision());

        // ModelUtil.storeViaXml(changedModel, checkedoutmodel.getContent());

        ModelUtil.assertModelEqual(changedModel, checkedoutmodel.getContent().get(0));
    }

    @Test
    public void testCreatesLocalDirectories() {
        final CommitTransaction tr = createTransaction(BRANCHNAME, 55);
        final URI fileUri = storage.createStorageUriFor(new Path("testmodels/dummymodel.xmi"), tr.getRevision().getRevisionId(), false);
        assertEquals(new File(tempDir, "testBranch/55/testmodels").toURI().toString(), fileUri.toString());
    }
}
