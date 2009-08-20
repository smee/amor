/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.internal.impl;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.infai.amor.backend.Branch;
import org.infai.amor.backend.internal.storage.BlobStorage;
import org.infai.amor.backend.storage.Storage;
import org.infai.amor.backend.storage.StorageFactory;

/**
 * @author sdienst
 * 
 */
public class DumbStorageFactory implements StorageFactory {
    private final Map<String, Storage> storages = new HashMap<String, Storage>();
    private String storageDir = ".";

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.StorageFactory#getStorage(org.infai.amor.backend.Branch)
     */
    @Override
    public Storage getStorage(final Branch branch) {
        final String bname = branch.getName();
        Storage storage = storages.get(bname);
        if (storage == null) {
            storage = new BlobStorage(new File(storageDir), bname);
            storages.put(bname, storage);
        }
        return storage;
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
