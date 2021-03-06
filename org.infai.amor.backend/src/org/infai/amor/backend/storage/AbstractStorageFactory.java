/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.storage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.infai.amor.backend.*;
import org.infai.amor.backend.exception.TransactionException;

/**
 * Common superclass for {@link StorageFactory} implementations.
 * @author sdienst
 * 
 */
public abstract class AbstractStorageFactory implements StorageFactory {
    private final Map<String, Storage> storages = new HashMap<String, Storage>();
    private final Map<Long, Storage> runningStorages = new HashMap<Long, Storage>();

    /**
     * @param branchname
     */
    public AbstractStorageFactory() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.exception.TransactionListener#commit(org.infai.amor.backend.CommitTransaction)
     */
    @Override
    public void commit(final CommitTransaction tr) throws TransactionException {
        final Storage storage = getStorage(tr);
        try {
            storage.commit(tr);
        } finally {
            runningStorages.remove(tr.getRevision().getRevisionId());
        }
    }

    /**
     * @param branch
     * @return
     */
    abstract protected Storage createNewStorage(Branch branch);

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
            storage = makeReadOnly(createNewStorage(branch));
            storages.put(bname, storage);
        }
        return storage;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.storage.StorageFactory#getStorage(org.infai.amor.backend.CommitTransaction)
     */
    public Storage getStorage(final CommitTransaction tr) {
        return runningStorages.get(tr.getRevision().getRevisionId());
    }

    /**
     * @param createNewStorage
     * @return
     */
    private Storage makeReadOnly(final Storage wrappedStorage) {
        return new Storage() {

            @Override
            public void checkin(final ChangedModel model, final URI externalUri, final Revision revision) throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void checkin(final Model model, final URI externalUri, final Revision revision) throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public Model checkout(final IPath path, final Revision revision) throws IOException {
                return wrappedStorage.checkout(path, revision);
            }

            @Override
            public void commit(final CommitTransaction tr) throws TransactionException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void delete(final IPath modelPath, final URI externalUri, final Revision revision) throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void rollback(final CommitTransaction tr) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void startTransaction(final CommitTransaction tr) {
                throw new UnsupportedOperationException();
            }

            @Override
            public EObject view(final IPath path, final Revision revision) throws IOException {
                return wrappedStorage.view(path, revision);
            }
        };
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.exception.TransactionListener#rollback(org.infai.amor.backend.CommitTransaction)
     */
    @Override
    public void rollback(final CommitTransaction tr) {
        final Storage storage = getStorage(tr);
        try {
            storage.rollback(tr);
        } finally {
            runningStorages.remove(tr.getRevision().getRevisionId());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.exception.TransactionListener#startTransaction(org.infai.amor.backend.CommitTransaction)
     */
    @Override
    public void startTransaction(final CommitTransaction tr) {
        final Storage storage = createNewStorage(tr.getBranch());
        runningStorages.put(tr.getRevision().getRevisionId(), storage);

        storage.startTransaction(tr);
    }

}
