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

import java.io.File;
import java.io.IOException;

import org.eclipse.emf.common.util.URI;
import org.infai.amor.backend.Branch;
import org.infai.amor.backend.impl.CommitTransactionImpl;
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

    @Before
    public void setUp() throws IOException {
        tempDir = File.createTempFile("storage", "temp");
        storage = new BlobStorage(tempDir);
        context = new Mockery();
    }

    @Test
    public void testCreatesLocalDirectories() {
        final Branch branch = context.mock(Branch.class);
        context.checking(new Expectations() {
            {
                allowing(branch).getName();
                will(returnValue("testBranch"));
            }
        });
        final URI fileUri = storage.createUriFor(null, new CommitTransactionImpl(branch, 55, null));
        assertEquals(new File(tempDir, "testBranch/55").toURI().toString(), fileUri.toString());
    }
}
