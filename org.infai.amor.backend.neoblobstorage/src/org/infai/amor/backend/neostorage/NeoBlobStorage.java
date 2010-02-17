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

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.infai.amor.backend.*;
import org.infai.amor.backend.Revision.ChangeType;
import org.infai.amor.backend.exception.TransactionException;
import org.infai.amor.backend.internal.*;
import org.infai.amor.backend.internal.impl.*;
import org.infai.amor.backend.storage.Storage;
import org.neo4j.graphdb.Node;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Store models into neo4j, stores complete models while ignoring all differences between versions.
 * 
 * @author sdienst
 * 
 */
public class NeoBlobStorage extends NeoObjectFactory implements Storage {
    private final static Logger logger = Logger.getLogger(NeoBlobStorage.class.getName());
    private final NeoBranch branch;
    private Map<EObject, Node> cache;

    private static String createModelSpecificPath(final IPath modelPath) {
        if (modelPath != null && !modelPath.isAbsolute()) {
            return StringUtils.join(modelPath.segments(), '/');
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
    public void checkin(final ChangedModel model, final URI externalUri, final Revision revision) throws IOException {
        throw new UnsupportedOperationException("not implemented");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.storage.Storage#checkin(org.infai.amor.backend.Model, org.eclipse.emf.common.util.URI, long)
     */
    @Override
    public void checkin(final Model model, final URI externalUri, final Revision revision) throws IOException {
        final Collection<String> ePackageUris = getEPackageUrisFrom(model.getContent());
        // store all eobjects/epackages
        logger.finer("----------1-Storing contents----------");
        final NeoMappingDispatcher disp1 = new NeoMappingDispatcher(getNeoProvider());
        disp1.setRegistry(cache);
        disp1.store(model);
        logger.finer("----------2-Storing metadata----------");
        final AbstractNeoDispatcher disp2 = new NeoMetadataDispatcher(getNeoProvider());
        // reuse the eobject->neo4j node map
        disp2.setRegistry(cache);
        // store all additional references and meta relationships
        final NeoModelLocation modelLocation = disp2.store(model);
        // store epackage namespace uris
        if (!ePackageUris.isEmpty()) {
            modelLocation.setEPackageNamespaces(ePackageUris);
        }
        modelLocation.setExternalUri(externalUri);
        modelLocation.setRelativePath(createModelSpecificPath(model.getPersistencePath()));
        modelLocation.setChangetype(ChangeType.ADDED);
        // remember new model node
        ((InternalRevision) revision).touchedModel(modelLocation);
    }
    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.storage.Storage#checkout(org.eclipse.core.runtime.IPath, long)
     */
    @Override
    public Model checkout(final IPath path, final Revision revision) throws IOException {
        logger.finer(String.format("checking out %s of revision %d", path.toString(), revision.getRevisionId()));

        final NeoRevision neoRev = (NeoRevision) revision;
        final NeoModelLocation modelLocation = (NeoModelLocation) neoRev.getModelLocation(createModelSpecificPath(path));

        final NeoRestorer restorer = new NeoRestorer(getNeoProvider());
        return new ModelImpl(restorer.load(modelLocation), path);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.exception.TransactionListener#commit(org.infai.amor.backend.CommitTransaction)
     */
    @Override
    public void commit(final CommitTransaction tr) throws TransactionException {
        cache = null;
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.storage.Storage#delete(org.eclipse.core.runtime.IPath, long)
     */
    @Override
    public void delete(final IPath modelPath, final URI externalUri, final Revision revision) throws IOException {
        // TODO test!
        // remember deleted model node
        ((InternalRevision) revision).touchedModel(new NeoModelLocation(getNeoProvider(), getNeo().createNode(), modelPath.toString(), externalUri, ChangeType.DELETED));
    }

    /**
     * Returns the {@link EPackage} namespace uri for every {@link EObject} in <code>content</code> that is an instance of {@link EPackage}.
     * @param content
     * @return
     */
    private Collection<String> getEPackageUrisFrom(final List<? extends EObject> content) {
        final Collection<String> res = Lists.newArrayList();
        for(final EObject eo: content){
            if(eo instanceof EPackage) {
                final EPackage epackage = (EPackage) eo;
                res.add(epackage.getNsURI());
                res.addAll(getEPackageUrisFrom(epackage.getESubpackages()));
            }
        }
        return res;
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
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.exception.TransactionListener#startTransaction(org.infai.amor.backend.CommitTransaction)
     */
    @Override
    public void startTransaction(final CommitTransaction tr) {
        cache = Maps.newHashMap();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.storage.Storage#view(org.eclipse.core.runtime.IPath, long)
     */
    @Override
    public EObject view(final IPath path, final Revision revision) throws IOException {
        throw new UnsupportedOperationException("not implemented");
    }
}
