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
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.epatch.*;
import org.eclipse.emf.compare.epatch.applier.ApplyStrategy;
import org.eclipse.emf.compare.epatch.applier.CopyingEpatchApplier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.infai.amor.backend.*;
import org.infai.amor.backend.Revision.ChangeType;
import org.infai.amor.backend.exception.TransactionException;
import org.infai.amor.backend.internal.ModelImpl;
import org.infai.amor.backend.internal.NeoProvider;
import org.infai.amor.backend.neo.NeoModelLocation;
import org.infai.amor.backend.neo.NeoObjectFactory;
import org.infai.amor.backend.resources.AmorResourceSetImpl;
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
    private Map<EObject, Node> cache;

    private static String createModelSpecificPath(final IPath modelPath) {
        if (modelPath != null && !modelPath.isAbsolute()) {
            return StringUtils.join(modelPath.segments(), '/');
        } else {
            throw new IllegalArgumentException("The given path must be relative for storing a model, was absolute: " + modelPath);
        }
    }

    /**
     * @param startRev
     * @param relativePath
     * @return
     */
    private static Revision findMostRecentRevision(final Revision startRev, final String relativePath) {
        Revision rev = startRev;
        while (rev != null) {
            for (final ModelLocation loc : rev.getModelReferences(ChangeType.ADDED, ChangeType.CHANGED, ChangeType.DELETED)) {
                if (relativePath.equals(loc.getRelativePath())) {
                    if(loc.getChangeType()==ChangeType.DELETED) {
                        return null;
                    } else {
                        return rev;
                    }
                }
            }
            rev = rev.getPreviousRevision();
        }
        return null;
    }

    public NeoBlobStorage(final NeoProvider np) {
        super(np);
    }

    /**
     * @param origModel
     * @param patch
     * @return
     */
    private EList<EObject> applyEPatch(final Model origModel, final Epatch patch) {
        // build resourceset that contains everything the epatch references
        final ResourceSet inputRS = new AmorResourceSetImpl();
        // copy all known epackages
        final ResourceSet origRS = origModel.getContent().get(0).eResource().getResourceSet();

        // add resources that get referenced by this patch
        for (final ModelImport modelimport : patch.getModelImports()) {
            String name = modelimport.getName();
            if (modelimport instanceof EPackageImport) {
                name = ((EPackageImport) modelimport).getNsURI();
            }
            final Resource importedResource = origRS.getResource(URI.createURI(name), false);
            if (importedResource != null) {
                inputRS.getResources().add(importedResource);
            }
        }
        // use name of the left model uri of the patch
        final Resource resource = inputRS.createResource(URI.createURI(patch.getResources().get(0).getLeftUri()));
        resource.getContents().addAll(origModel.getContent());
        // create mapping of NamedResources to resources, because the builtin functionality of CopyingEpatchApplier is buggy :(
        final Map<NamedResource, Resource> resourceMap = Maps.newHashMap();
        resourceMap.put(patch.getResources().get(0), resource);
        // apply epatch
        final CopyingEpatchApplier epatchApplier = new CopyingEpatchApplier(ApplyStrategy.LEFT_TO_RIGHT, patch, resourceMap, inputRS);
        epatchApplier.apply();
        return epatchApplier.getOutputResourceSet().getResources().get(0).getContents();
    }
    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.storage.Storage#checkin(org.infai.amor.backend.ChangedModel, org.eclipse.emf.common.util.URI,
     * long)
     */
    @Override
    public void checkin(final ChangedModel model, final URI externalUri, final Revision revision) throws IOException {
        // find most recent revision for the model this patch should be applied to
        final Revision origRevision = findMostRecentRevision(revision, model.getPath().toString());
        if (origRevision == null) {
            throw new IOException("There is no stored model at the path " + model.getPath());
        }
        // checkout last version of this model
        final Model origModel = this.checkout(model.getPath(), origRevision);
        final EList<EObject> newModelContents = applyEPatch(origModel, model.getDiffModel());

        // use #checkin(...) for persisting the changed model
        checkin(new ModelImpl(newModelContents, model.getPath()), externalUri, revision);
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
        disp1.store(model, revision);
        logger.finer("----------2-Storing metadata----------");
        final AbstractNeoDispatcher disp2 = new NeoMetadataDispatcher(getNeoProvider());
        // reuse the eobject->neo4j node map
        disp2.setRegistry(cache);
        // store all additional references and meta relationships
        final NeoModelLocation modelLocation = disp2.store(model, revision);
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

        final InternalRevision neoRev = (InternalRevision) revision;
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
