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
import java.io.IOException;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xml.type.internal.DataValue.URI.MalformedURIException;
import org.infai.amor.backend.ChangedModel;
import org.infai.amor.backend.CommitTransaction;
import org.infai.amor.backend.Model;
import org.infai.amor.backend.Response;
import org.infai.amor.backend.exception.TransactionException;
import org.infai.amor.backend.internal.UriHandler;
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
    private final UriHandler uriHandler;

    public BlobStorage(final File storageDir, final UriHandler uh) {
        this.storageDir = storageDir;
        this.uriHandler = uh;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.storage.Storage#checkin(org.infai.amor.backend.ChangedModel,
     * org.infai.amor.backend.CommitTransaction)
     */
    @Override
    public Response checkin(final ChangedModel model, final CommitTransaction tr) throws IOException {
        setMapping("amormodel");
        final Resource resource = resourceSet.createResource(createUriFor(model.getPath(), tr, true));
        resource.getContents().add(model.getContent());
        resource.save(null);
        // we ignore dependant models altogether
        return new CheckinResponse("Success.", uriHandler.createModelUri(tr, createModelSpecificPath(model.getPath())));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.storage.Storage#checkin(org.infai.amor.backend.Model, org.infai.amor.backend.CommitTransaction)
     */
    @Override
    public Response checkin(final Model model, final CommitTransaction tr) throws IOException {
        setMapping("amormodel");
        final Resource resource = resourceSet.createResource(createUriFor(model.getPersistencePath(), tr, true));
        resource.getContents().add(model.getContent());
        resource.save(null);
        // we ignore dependant models altogether
        return new CheckinResponse("Success.", uriHandler.createModelUri(tr, createModelSpecificPath(model.getPersistencePath())));
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
     * @param includeFilename
     * @return
     */
    private String createModelSpecificPath(final IPath modelPath) {
        if (modelPath != null && !modelPath.isAbsolute()) {
            final int numSegments = modelPath.segmentCount();

            final StringBuilder sb = new StringBuilder();
            // ignore filename
            for (int i = 0; i < numSegments - 1; i++) {
                sb.append(File.separatorChar).append(modelPath.segment(i));
            }

            return sb.toString();
        } else {
            throw new IllegalArgumentException("The given path must be relative for storing a model, was absolute: " + modelPath);
        }
    }

    /**
     * @param modelPath
     * @param tr
     * @return
     */
    protected URI createUriFor(final IPath modelPath, final CommitTransaction tr, final boolean includeFilename) {
        String dirName = tr.getBranch().getName() + "/" + Long.toString(tr.getRevisionId());
        // if there is a model path, use its relative directory part
        dirName = dirName + "/" + createModelSpecificPath(modelPath);
        File dir = new File(storageDir, dirName);
        dir.mkdirs();
        if (includeFilename) {
            dir = new File(dir, modelPath.lastSegment());
        }
        // create uri for this new path
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
        final URI fileURI = createUriFor(null, tr, false);
        // delete the directory of this revision
        try {
            final File dir = new File(new java.net.URI(fileURI.toString()));
            deleteRecurively(dir);
        } catch (final URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * @param string
     */
    private void setMapping(final String mapping) {
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(mapping, new XMIResourceFactoryImpl());
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
