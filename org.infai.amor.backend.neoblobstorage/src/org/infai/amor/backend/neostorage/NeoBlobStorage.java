/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.neostorage;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.infai.amor.backend.*;
import org.infai.amor.backend.Revision.ChangeType;
import org.infai.amor.backend.exception.TransactionException;
import org.infai.amor.backend.internal.*;
import org.infai.amor.backend.internal.impl.*;
import org.infai.amor.backend.storage.Storage;
import org.neo4j.api.core.Node;

import com.google.common.collect.Maps;

/**
 * Store models into neo4j, stores complete models while ignoring all differences between versions.
 * 
 * @author sdienst
 * 
 */
public class NeoBlobStorage extends NeoObjectFactory implements Storage {
    private final static Logger logger = Logger.getLogger(NeoBlobStorage.class.getName());
    private Map<URI, NeoModelLocation> addedModelNodes;
    private final NeoBranch branch;
    private Map<EObject, Node> cache;

    private static String createModelSpecificPath(final IPath modelPath) {
        if (modelPath != null && !modelPath.isAbsolute()) {
            final int numSegments = modelPath.segmentCount();

            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < numSegments; i++) {
                sb.append(File.separatorChar).append(modelPath.segment(i));
            }
            return sb.toString();
        } else {
            throw new IllegalArgumentException("The given path must be relative for storing a model, was absolute: " + modelPath);
        }
    }

    public NeoBlobStorage(final NeoProvider np, final NeoBranch branch) {
        super(np);
        this.branch = branch;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.storage.Storage#checkin(org.infai.amor.backend.ChangedModel, org.eclipse.emf.common.util.URI,
     * long)
     */
    @Override
    public void checkin(final ChangedModel model, final URI externalUri, final long revisionId) throws IOException {
        throw new UnsupportedOperationException("not implemented");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.storage.Storage#checkin(org.infai.amor.backend.Model, org.eclipse.emf.common.util.URI, long)
     */
    @Override
    public void checkin(final Model model, final URI externalUri, final long revisionId) throws IOException {
        // store all eobjects/epackages
        logger.finer("----------1-Storing contents----------");
        final NeoMappingDispatcher disp1 = new NeoMappingDispatcher(getNeoProvider());
        disp1.setRegistry(cache);

        for (final EObject eo : model.getContent()) {
            disp1.dispatch(eo);
            for (final TreeIterator<EObject> it = eo.eAllContents(); it.hasNext();) {
                final EObject eoSub = it.next();
                // System.out.println("storing " + eoSub);
                disp1.dispatch(eoSub);
            }
        }
        logger.finer("----------2-Storing metadata----------");
        final NeoMetadataDispatcher disp2 = new NeoMetadataDispatcher(getNeoProvider());
        // reuse the eobject->neo4j node map
        disp2.setRegistry(disp1.getRegistry());

        // store all additional references and meta relationships
        for (final EObject eo : model.getContent()) {
            disp2.dispatch(eo);
            for (final TreeIterator<EObject> it = eo.eAllContents(); it.hasNext();) {
                final EObject eoSub = it.next();
                disp2.dispatch(eoSub);
            }
        }
        this.cache = disp2.getRegistry();
        // remember new model node
        // FIXME we wrote several model elements, need to link to all of them, not just to the first
        this.addedModelNodes.put(externalUri, new NeoModelLocation(getNeoProvider(), disp2.getRegistry().get(model.getContent().get(0)), createModelSpecificPath(model.getPersistencePath()), externalUri, ChangeType.ADDED));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.storage.Storage#checkout(org.eclipse.core.runtime.IPath, long)
     */
    @Override
    public Model checkout(final IPath path, final long revisionId) throws IOException {
        logger.finer(String.format("checking out %s of revision %d", path.toString(), revisionId));

        final NeoRevision revision = branch.getRevision(revisionId);
        final NeoModelLocation modelLocation = (NeoModelLocation) revision.getModelLocation(createModelSpecificPath(path));

        final NeoRestorer restorer = new NeoRestorer(getNeoProvider());
        return new ModelImpl(restorer.load(modelLocation.getModelHead()), path);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.exception.TransactionListener#commit(org.infai.amor.backend.CommitTransaction)
     */
    @Override
    public void commit(final CommitTransaction tr, final Revision rev) throws TransactionException {
        if (rev instanceof InternalRevision) {
            // add all modelLocations to the revision
            final InternalRevision revision = (InternalRevision) rev;
            for (final URI uri : addedModelNodes.keySet()) {
                final NeoModelLocation loc = addedModelNodes.get(uri);
                revision.touchedModel(loc);

            }
            cache = null;
            addedModelNodes = null;
        } else {
            throw new TransactionException("Internal error: Do not know how to commit to revision of type " + rev.getClass());
        }
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.storage.Storage#delete(org.eclipse.core.runtime.IPath, long)
     */
    @Override
    public void delete(final IPath modelPath, final URI externalUri,final long revisionId) throws IOException {
        throw new UnsupportedOperationException("not implemented");
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.exception.TransactionListener#rollback(org.infai.amor.backend.CommitTransaction)
     */
    @Override
    public void rollback(final CommitTransaction tr) {
        // nothing to do, gets handled by the neo4j transaction
        cache = null;
        addedModelNodes = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.exception.TransactionListener#startTransaction(org.infai.amor.backend.CommitTransaction)
     */
    @Override
    public void startTransaction(final CommitTransaction tr) {
        this.addedModelNodes = Maps.newHashMap();
        cache = Maps.newHashMap();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.storage.Storage#view(org.eclipse.core.runtime.IPath, long)
     */
    @Override
    public EObject view(final IPath path, final long revisionId) throws IOException {
        throw new UnsupportedOperationException("not implemented");
    }
}
