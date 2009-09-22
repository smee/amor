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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.epatch.applier.ApplyStrategy;
import org.eclipse.emf.compare.epatch.applier.CopyingEpatchApplier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.infai.amor.backend.ChangedModel;
import org.infai.amor.backend.CommitTransaction;
import org.infai.amor.backend.Model;
import org.infai.amor.backend.Revision;
import org.infai.amor.backend.exception.TransactionException;
import org.infai.amor.backend.internal.impl.ModelImpl;
import org.infai.amor.backend.internal.impl.NeoRevision;
import org.infai.amor.backend.storage.Storage;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

/**
 * Store models as xml documents without messing with their internal structures.
 * 
 * @author sdienst
 * 
 */
public class BlobStorage implements Storage {

    private final File storageDir;
    private ResourceSetImpl resourceSet;
    private Collection<URI> addedModelUris;

    /**
     * @param modelPath
     * @param includeFilename
     * @return
     */
    private static String createModelSpecificPath(final IPath modelPath) {
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

    public BlobStorage(final File storageDir, final String branchname) {
        this.storageDir = new File(storageDir, branchname);
        /*
         * TODO create Map<nsuri, most recent metamodel>, needed to be able to load model instances
         */
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.storage.Storage#checkin(org.infai.amor.backend.ChangedModel, org.eclipse.emf.common.util.URI,
     * long)
     */
    @Override
    public void checkin(final ChangedModel model, final URI externalUri, final long revisionId) throws IOException {
        // FIXME not usable atm
        // we ignore dependant models altogether
        setMapping("amormodel");
        final ResourceSet inputRS = findMostRecentModelFor(model.getPath());
        // apply the model patch
        final CopyingEpatchApplier epatchApplier = new CopyingEpatchApplier(ApplyStrategy.LEFT_TO_RIGHT, model.getDiffModel(), inputRS);
        epatchApplier.apply();
        // store changed models
        final ResourceSet outputResourceSet = epatchApplier.getOutputResourceSet();
        for (final Resource res : outputResourceSet.getResources()) {
            System.out.println(res.getURI());
        }
        // final Resource resource = resourceSet.createResource(createStorageUriFor(model.getPath(), revisionId, true));

        // resource.save(null);
        addedModelUris.add(externalUri);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.storage.Storage#checkin(org.infai.amor.backend.Model, org.eclipse.emf.common.util.URI, long)
     */
    @Override
    public void checkin(final Model model, final URI externalUri, final long revisionId) throws IOException {
        setMapping("amormodel");
        final Resource resource = resourceSet.createResource(createStorageUriFor(model.getPersistencePath(), revisionId, true));
        resource.getContents().add(model.getContent());
        resource.save(null);
        addedModelUris.add(externalUri);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.storage.Storage#checkout(org.eclipse.emf.common.util.URI)
     */
    @Override
    public Model checkout(final IPath path, final long revisionId) throws IOException {
        // TODO first load the metamodel to prevent exception
        final Resource resource = new ResourceSetImpl().createResource(createStorageUriFor(path, revisionId, true));
        resource.load(null);
        return new ModelImpl(resource.getContents().get(0), path);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.exception.TransactionListener#commit(org.infai.amor.backend.CommitTransaction)
     */
    @Override
    public void commit(final CommitTransaction tr, final Revision rev) throws TransactionException {
        // nothing to do
        resourceSet = null;
        if (rev instanceof NeoRevision) {
            final NeoRevision revision = (NeoRevision) rev;
            for (final URI uri : addedModelUris) {
                revision.addModel(uri, null);

            }
        } else {
            throw new TransactionException("Internal error: Do not know how to commit to revision of type " + rev.getClass());
        }
    }

    /**
     * @param modelPath
     * @param tr
     * @return
     */
    protected URI createStorageUriFor(final IPath modelPath, final long revisionId, final boolean includeFilename) {
        String dirName = Long.toString(revisionId);
        // if there is a model path, use its relative directory part
        if (modelPath != null) {
            dirName = dirName + "/" + createModelSpecificPath(modelPath);
        }
        File dir = new File(storageDir, dirName);
        dir.mkdirs();
        if (includeFilename && modelPath != null) {
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

    /**
     * Create a new resourceset that contains the newest instance of the model specified by the given relative path
     * 
     * @param path
     * @return
     */
    protected ResourceSet findMostRecentModelFor(final IPath path) {
        final String modelSpecificPath = createModelSpecificPath(path) + File.separatorChar + path.lastSegment();
        // find all revisions
        final ArrayList<File> allRevDirs = Lists.newArrayList(Arrays.asList(storageDir.listFiles()));
        // order them by last modified timestamp
        final Ordering<File> order = Ordering.from(new Comparator<File>() {
            @Override
            public int compare(final File o1, final File o2) {
                final long time1 = o1.lastModified();
                final long time2 = o2.lastModified();
                // during unit tests the timestamp might be the same, then sort by revision (name of the directory)
                if (time1 == time2) {
                    final long rev1 = Long.parseLong(o1.getName());
                    final long rev2 = Long.parseLong(o2.getName());
                    return new Long(rev1).compareTo(rev2);
                } else {
                    return new Long(time1).compareTo(time2);
                }
            }
        });
        // find the newest revision
        final File newestRevisionDir = order.max(Iterables.filter(allRevDirs, new Predicate<File>() {
            @Override
            public boolean apply(final File revDir) {
                final File f = new File(revDir, modelSpecificPath);
                System.out.println("Does "+f+" exist? "+f.exists());
                return f.exists();
            }
        }));
        // TODO first, load the most recent metamodel

        // load the newest model version
        resourceSet.getResource(createStorageUriFor(path, Long.parseLong(newestRevisionDir.getName()), true), true);
        return resourceSet;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.exception.TransactionListener#rollback(org.infai.amor.backend.CommitTransaction)
     */
    @Override
    public void rollback(final CommitTransaction tr) {
        final URI fileURI = createStorageUriFor(null, tr.getRevisionId(), false);
        // delete the directory of this revision
        try {
            final File dir = new File(new java.net.URI(fileURI.toString()));
            deleteRecurively(dir);
        } catch (final URISyntaxException e) {
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
        addedModelUris = Lists.newArrayList();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.storage.Storage#view(org.eclipse.emf.common.util.URI)
     */
    @Override
    public EObject view(final IPath path, final long revisionId) throws IOException {
        throw new UnsupportedOperationException("not implemented");
    }

}
