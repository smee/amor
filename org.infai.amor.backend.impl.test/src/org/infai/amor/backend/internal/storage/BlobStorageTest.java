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

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceFactoryImpl;
import org.infai.amor.backend.Branch;
import org.infai.amor.backend.CommitTransaction;
import org.infai.amor.backend.Model;
import org.infai.amor.backend.Response;
import org.infai.amor.backend.impl.CommitTransactionImpl;
import org.infai.amor.backend.internal.impl.UriHandlerImpl;
import org.infai.amor.backend.internal.responses.CheckinResponse;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;

/**
 * @author sdienst
 * 
 */
public class BlobStorageTest {

    private BlobStorage storage;
    private Mockery context;
    private File tempDir;

    private CommitTransaction createTransaction(final String branchname, final long revisionId) {
        final Branch branch = context.mock(Branch.class);
        context.checking(new Expectations() {
            {
                allowing(branch).getName();
                will(returnValue("testBranch"));
            }
        });
        return new CommitTransactionImpl(branch, 55, null);
    }

    /**
     * @param string
     * @return
     * @throws IOException
     */
    private EObject readModel(final String string) throws IOException {
        final ResourceSet rs = new ResourceSetImpl();

        final Resource resource = rs.createResource(URI.createFileURI(new File(string).getAbsolutePath()));
        resource.load(null);
        return resource.getContents().get(0);
    }

    @Before
    public void setUp() throws IOException {
        tempDir = File.createTempFile("storage", "temp");
        tempDir.delete();
        tempDir.mkdirs();

        storage = new BlobStorage(tempDir, new UriHandlerImpl());
        context = new Mockery();
        // init persistence mappings for ecore and xmi
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xml", new XMLResourceFactoryImpl());
    }

    @Test
    public void storeModel() throws IOException {
        // read model
        final Model m = new ModelImpl(readModel("testmodels/base.ecore"), "testmodels/base.ecore");
        // start commit transaction
        final CommitTransaction tr = createTransaction("testBranch", 55);
        storage.startTransaction(tr);
        // store it into branch testBranch and revision 55
        final Response response = storage.checkin(m, tr);
        assertEquals(CheckinResponse.class, response.getClass());

        final File storedFile = new File(tempDir, "testBranch/55/testmodels/base.ecore");
        assertTrue(storedFile.exists());
        // TODO compare contents via XMLUnit
    }

    @Test
    public void testCreatesLocalDirectories() {
        final CommitTransaction tr = createTransaction("testBranch", 55);
        final URI fileUri = storage.createUriFor(new Path("testmodels/dummymodel.xmi"), tr, false);
        assertEquals(new File(tempDir, "testBranch/55/testmodels").toURI().toString(), fileUri.toString());
    }
}
