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
import org.infai.amor.ModelUtil;
import org.infai.amor.backend.Branch;
import org.infai.amor.backend.CommitTransaction;
import org.infai.amor.backend.Model;
import org.infai.amor.backend.impl.CommitTransactionImpl;
import org.infai.amor.backend.internal.impl.ModelImpl;
import org.jmock.Expectations;
import org.jmock.Mockery;
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

    private CommitTransaction createTransaction(final String branchname, final long revisionId) {
        final Branch branch = context.mock(Branch.class);
        context.checking(new Expectations() {
            {
                allowing(branch).getName();
                will(returnValue(BRANCHNAME));
            }
        });
        return new CommitTransactionImpl(branch, 55, null);
    }

    @Before
    public void setUp() throws IOException {
        tempDir = File.createTempFile("storage", "temp");
        tempDir.delete();
        tempDir.mkdirs();

        storage = new BlobStorage(tempDir, BRANCHNAME);
        context = new Mockery();
    }

    @Test
    public void shouldEqualSavedAndRestoredModel() throws Exception {
        // given
        final Model m = new ModelImpl(ModelUtil.readInputModel("testmodels/base.ecore"), "testmodels/base.ecore");
        final CommitTransaction tr = createTransaction(BRANCHNAME, 88);

        // when
        storage.startTransaction(tr);
        storage.checkin(m, tr);
        final Model checkedout = storage.checkout(m.getPersistencePath(), tr.getRevisionId());

        // then
        // TODO compare via emfcompare or xmlunit
        ModelUtil.assertModelEqual(m.getContent(), checkedout.getContent());
    }

    @Test
    public void shouldSaveModelWithoutChanges() throws IOException, SAXException {
        // read model
        final Model m = new ModelImpl(ModelUtil.readInputModel("testmodels/base.ecore"), "testmodels/base.ecore");
        // start commit transaction
        final CommitTransaction tr = createTransaction(BRANCHNAME, 55);
        storage.startTransaction(tr);
        // store it into branch testBranch and revision 55
        storage.checkin(m, tr);

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
