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

import java.io.File;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xml.type.internal.DataValue.URI.MalformedURIException;
import org.infai.amor.backend.ChangedModel;
import org.infai.amor.backend.CommitTransaction;
import org.infai.amor.backend.Model;
import org.infai.amor.backend.Response;
import org.infai.amor.backend.exception.TransactionException;
import org.infai.amor.backend.internal.responses.CheckinResponse;
import org.infai.amor.backend.storage.Storage;

/**
 * Store models as xml documents without messing with their internal structures.
 * 
 * @author sdienst
 * 
 */
public class BlobStorage implements Storage {

    private final File storageDir;
    private ResourceSetImpl resourceSet;

    public BlobStorage(final File storageDir) {
        this.storageDir = storageDir;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.storage.Storage#checkin(org.infai.amor.backend.ChangedModel,
     * org.infai.amor.backend.CommitTransaction)
     */
    @Override
    public Response checkin(final ChangedModel model, final CommitTransaction tr) {
        final URI fileURI = createUriFor(model.getPath(), tr);

        // TODO create real repository uri usable as external reference
        return new CheckinResponse("Checked in model [" + model.getPath().lastSegment() + "]", null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.storage.Storage#checkin(org.infai.amor.backend.Model, org.infai.amor.backend.CommitTransaction)
     */
    @Override
    public Response checkin(final Model model, final CommitTransaction tr) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.storage.Storage#checkout(org.eclipse.emf.common.util.URI)
     */
    @Override
    public Model checkout(final URI uri) throws MalformedURIException {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.exception.TransactionListener#commit(org.infai.amor.backend.CommitTransaction)
     */
    @Override
    public void commit(final CommitTransaction tr) throws TransactionException {
        // nothing to do
        resourceSet = null;
    }

    /**
     * @param modelPath
     * @param tr
     * @return
     */
    protected URI createUriFor(final IPath modelPath, final CommitTransaction tr) {
        String dirName = tr.getBranch().getName() + File.separatorChar + Long.toString(tr.getRevisionId());
        // if there is a model path, use its relative directory part
        if (modelPath != null && !modelPath.isAbsolute()) {
            final int numSegments = modelPath.segmentCount();
            for (int i = 0; i < numSegments - 1; i++) {
                dirName = dirName + File.separatorChar + modelPath.segment(i);
            }
        }
        // create new directory for this revision
        final File dir = new File(storageDir, dirName);
        dir.mkdirs();
        // create uri for this new directory
        final URI fileUri = URI.createURI(dir.toURI().toString());
        return fileUri;
    }

    /**
     * @param dir
     */
    private void deleteRecurively(final File dir) {
        if (dir.isDirectory()) {
            for (final File file : dir.listFiles()) {
                deleteRecurively(file);
            }
        }
        dir.delete();

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.exception.TransactionListener#rollback(org.infai.amor.backend.CommitTransaction)
     */
    @Override
    public void rollback(final CommitTransaction tr) {
        final URI fileURI = createUriFor(null, tr);
        // delete the directory of this revision
        try {
            final File dir = new File(new java.net.URI(fileURI.toString()));
            deleteRecurively(dir);
        } catch (final URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.exception.TransactionListener#startTransaction(org.infai.amor.backend.CommitTransaction)
     */
    @Override
    public void startTransaction(final CommitTransaction tr) {
        resourceSet = new ResourceSetImpl();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.storage.Storage#view(org.eclipse.emf.common.util.URI)
     */
    @Override
    public EObject view(final URI uri) throws MalformedURIException {
        // TODO Auto-generated method stub
        return null;
    }

}
