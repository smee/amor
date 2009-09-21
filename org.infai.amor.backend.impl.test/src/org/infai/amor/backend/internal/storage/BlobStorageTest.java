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
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.infai.amor.ModelUtil;
import org.infai.amor.backend.Branch;
import org.infai.amor.backend.CommitTransaction;
import org.infai.amor.backend.Model;
import org.infai.amor.backend.exception.TransactionException;
import org.infai.amor.backend.impl.CommitTransactionImpl;
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
    public void testCreatesLocalDirectories() {
        final CommitTransaction tr = createTransaction(BRANCHNAME, 55);
        final URI fileUri = storage.createStorageUriFor(new Path("testmodels/dummymodel.xmi"), tr.getRevisionId(), false);
        assertEquals(new File(tempDir, "testBranch/55/testmodels").toURI().toString(), fileUri.toString());
    }
}
