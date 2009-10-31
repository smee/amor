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

import java.io.File;

import org.infai.amor.backend.Branch;
import org.infai.amor.backend.internal.AbstractStorageFactory;
import org.infai.amor.backend.storage.Storage;

/**
 * @author sdienst
 * 
 */
public class FileStorageFactory extends AbstractStorageFactory {
    private String storageDir = ".";

    /**
     * @param branchname
     */
    public FileStorageFactory() {
        super();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.impl.AbstractStorageFactory#createNewStorage(java.lang.String)
     */
    @Override
    protected Storage createNewStorage(final Branch branch) {
        return new FileBlobStorage(new File(storageDir), branch.getName());
    }

    /**
     * @return the storageDir
     */
    public String getStorageDir() {
        return storageDir;
    }

    /**
     * @param storageDir
     *            the storageDir to set
     */
    public void setStorageDir(final String storageDir) {
        this.storageDir = storageDir;
    }

}
