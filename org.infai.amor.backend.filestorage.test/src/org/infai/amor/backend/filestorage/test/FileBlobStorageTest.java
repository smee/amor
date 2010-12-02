/*******************************************************************************
 * Copyright (c) 2010 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.filestorage.test;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

import org.infai.amor.backend.filestorage.FileStorageFactory;
import org.infai.amor.backend.impl.AbstractStorageIntegrationTest;
import org.infai.amor.backend.neo.NeoProvider;
import org.infai.amor.backend.storage.StorageFactory;

/**
 * @author sdienst
 *
 */
public class FileBlobStorageTest extends AbstractStorageIntegrationTest {

    /**
     * @param m
     */
    public FileBlobStorageTest(final String... m) {
        super(m);
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.neoblobstorage.test.AbstractStorageIntegrationTest#getStorageFactory(org.infai.amor.backend.internal.NeoProvider)
     */
    @Override
    protected StorageFactory getStorageFactory(final NeoProvider np) {
        File tempFile;
        try {
            tempFile = File.createTempFile("integration", "test");
            tempFile.delete();
            tempFile.mkdirs();

            final FileStorageFactory sf = new FileStorageFactory();
            sf.setStorageDir(tempFile.getAbsolutePath());

            return sf;
        } catch (final IOException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
        return null;
    }

}
