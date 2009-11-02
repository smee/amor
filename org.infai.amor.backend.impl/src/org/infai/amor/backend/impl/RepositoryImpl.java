/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.xml.type.internal.DataValue.URI.MalformedURIException;
import org.infai.amor.backend.Branch;
import org.infai.amor.backend.ChangedModel;
import org.infai.amor.backend.CommitTransaction;
import org.infai.amor.backend.Model;
import org.infai.amor.backend.Repository;
import org.infai.amor.backend.Response;
import org.infai.amor.backend.Revision;
import org.infai.amor.backend.Revision.ChangeType;
import org.infai.amor.backend.internal.BranchFactory;
import org.infai.amor.backend.internal.TransactionManager;
import org.infai.amor.backend.internal.UriHandler;
import org.infai.amor.backend.responses.CheckinErrorResponse;
import org.infai.amor.backend.responses.CheckinResponse;
import org.infai.amor.backend.responses.DeleteErrorResponse;
import org.infai.amor.backend.responses.DeleteSuccessResponse;
import org.infai.amor.backend.storage.Storage;
import org.infai.amor.backend.storage.StorageFactory;
import org.infai.amor.backend.util.Pair;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Default implementation of the amor repository backend.
 * 
 * @author sdienst<br>
 *         TODO do the collaborators need to be informed upon transaction commits?
 */
public class RepositoryImpl implements Repository {

    private final BranchFactory branchFactory;
    private final TransactionManager transactionManager;
    private final UriHandler uriHandler;
    private final StorageFactory storageFactory;

    /**
     * @param storage
     */
    public RepositoryImpl(final StorageFactory sf, final BranchFactory bf, final UriHandler uh, final TransactionManager tr) {
        this.storageFactory = sf;
        this.branchFactory = bf;
        this.uriHandler = uh;
        this.transactionManager = tr;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Repository#checkin(org.infai.amor.backend.ChangedModel, org.infai.amor.backend.Branch,
     * org.infai.amor.backend.Transaction)
     */
    @Override
    public Response checkin(final ChangedModel model, final CommitTransaction tr) {
        try {
            // remember the repository uri for this model
            final URI modeluri = uriHandler.createModelUri(tr, model.getPath());
            storageFactory.getStorage(tr.getBranch()).checkin(model, modeluri, tr.getRevisionId());

            return new CheckinResponse("Success.", modeluri);
        } catch (final IOException e) {
            e.printStackTrace();
            return new CheckinErrorResponse("Could not persist this model, reason: " + e.getMessage(), null);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Repository#checkin(org.infai.amor.backend.Model, org.infai.amor.backend.Branch,
     * org.infai.amor.backend.Transaction)
     */
    @Override
    public Response checkin(final Model model, final CommitTransaction tr) {
        try {
            // remember the repository uri for this model
            final URI modeluri = uriHandler.createModelUri(tr, model.getPersistencePath());
            final Storage storage = storageFactory.getStorage(tr.getBranch());
            storage.checkin(model, modeluri, tr.getRevisionId());

            return new CheckinResponse("Success.", modeluri);
        } catch (final IOException e) {
            e.printStackTrace();
            return new CheckinErrorResponse("Could not persist this model, reason: " + e.getMessage(), null);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Repository#checkout(org.eclipse.emf.common.util.URI)
     */
    @Override
    public Model checkout(final URI uri) throws IOException {
        transactionManager.startReadTransaction();
        try {
            final Storage storage = getStorage(uri);
            return storage.checkout(uriHandler.extractModelPathFrom(uri), uriHandler.extractRevision(uri));
        } finally {
            transactionManager.closeReadTransaction();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Repository#commitTransaction(org.infai.amor.backend.Transaction)
     */
    @Override
    public Response commitTransaction(final CommitTransaction tr) {
        final Revision revision = branchFactory.createRevision(tr);
        return transactionManager.commit(tr, revision);
    }

    /**
     * @param rev
     * @param uri
     * @throws MalformedURIException
     */
    private Iterable<URI> constructActiveRepositoryContents(Revision rev, final URI uri)
    throws MalformedURIException {
        final Map<IPath, Pair<Long, URI>> activeModels = Maps.newHashMap();
        final Map<IPath, Pair<Long, URI>> deletedModels = Maps.newHashMap();
        // cycle through to all older revisions
        while (rev != null) {
            final Iterable<URI> addedModelUris = Iterables.concat(rev
                    .getModelReferences(ChangeType.ADDED), rev
                    .getModelReferences(ChangeType.CHANGED));
            final Iterable<URI> deletedModelUris = rev.getModelReferences(ChangeType.DELETED);

            for (final URI deletedModelUri : deletedModelUris) {
                deletedModels.put(uriHandler.extractModelPathFrom(deletedModelUri),
                        new Pair(rev.getRevisionId(), deletedModelUri));
            }
            for (final URI touchedModelUri : addedModelUris) {
                // extract model path+name
                final IPath path = uriHandler.extractModelPathFrom(touchedModelUri);
                // was it deleted in a later revision?
                final Pair<Long, URI> p = deletedModels.get(path);
                if (p == null || p.first < rev.getRevisionId()) {
                    // was never deleted or was deleted prior to the current
                    // revision
                    if (!activeModels.containsKey(path)) {
                        activeModels.put(path, new Pair(rev.getRevisionId(),touchedModelUri));
                    }
                } else {
                    // will get deleted in a later revision, not active at this
                    // rev
                    activeModels.remove(path);
                }
            }

            rev = rev.getPreviousRevision();
        }
        final Collection<URI> result = Lists.newArrayList();
        for (final Pair<Long, URI> pair : activeModels.values()) {
            if (uriHandler.isPrefixIgnoringRevision(uri, pair.second)) {
                result.add(uriHandler.trimToNextSegmentKeepingRevision(uri.segmentCount() + 1, pair.second));
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Repository#createBranch(org.infai.amor.backend.Branch)
     */
    @Override
    public Branch createBranch(final Revision parent, final String name) {
        return branchFactory.createBranch(parent, name);
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.Repository#deleteModel(org.eclipse.core.runtime.IPath, org.infai.amor.backend.CommitTransaction)
     */
    @Override
    public Response deleteModel(final IPath modelPath, final CommitTransaction tr) throws IOException {
        try {
            final URI modeluri = uriHandler.createModelUri(tr, modelPath);
            storageFactory.getStorage(tr.getBranch()).delete(modelPath, modeluri,tr.getRevisionId());
            return new DeleteSuccessResponse("Model deleted successfully", uriHandler.createModelUri(tr, modelPath));
        } catch (final IOException ioe) {
            ioe.printStackTrace();
            return new DeleteErrorResponse(ioe.getMessage(), uriHandler.createModelUri(tr, modelPath));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Repository#getActiveContents(org.eclipse.emf.common.util.URI)
     */
    @Override
    public Iterable<URI> getActiveContents(final URI uri) throws MalformedURIException {
        transactionManager.startReadTransaction();
        try {
            String branchname = null;
            long revisionId = -1;
            boolean hasBranch = true, hasRevision = true;
            // what does this uri point to?
            try {
                // has it a branch?
                branchname = uriHandler.extractBranchName(uri);
            } catch (final MalformedURIException e) {
                hasBranch = false;
            }
            try {
                // has it a revision?
                revisionId = uriHandler.extractRevision(uri);
            } catch (final MalformedURIException e) {
                hasRevision = false;
            }
            if (!hasBranch) {
                // find all branchnames, convert them into uris
                return Iterables.transform(branchFactory.getBranches(), new Function<Branch, URI>() {
                    @Override
                    public URI apply(final Branch branch) {
                        return uriHandler.createUriFor(branch);
                    }
                });
            } else if (!hasRevision) {
                // find all revisions of the branch, convert them into uris
                final Branch branch = branchFactory.getBranch(branchname);
                return Iterables.transform(branch.getRevisions(), new Function<Revision, URI>() {
                    @Override
                    public URI apply(final Revision r) {
                        return uriHandler.createUriFor(branch, r.getRevisionId());
                    }
                });
            } else {
                // find all alive models, convert them into uris
                final Branch branch = branchFactory.getBranch(branchname);
                final Revision rev = branch.getRevision(revisionId);
                if (rev == null) {
                    throw new MalformedURIException("Unknown revision: " + uri);
                } else {
                    return constructActiveRepositoryContents(rev,uri);
                }

            }
        } finally {
            transactionManager.closeReadTransaction();
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Repository#getBranch(org.eclipse.emf.common.util.URI)
     */
    @Override
    public Branch getBranch(final URI uri) throws MalformedURIException {
        transactionManager.startReadTransaction();
        try {

            final String branchName = uriHandler.extractBranchName(uri);
            return branchFactory.getBranch(branchName);

        } finally {
            transactionManager.closeReadTransaction();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Repository#getBranches(org.eclipse.emf.common.util.URI)
     */
    @Override
    public Iterable<Branch> getBranches(final URI uri) throws MalformedURIException {
        transactionManager.startReadTransaction();
        try {

            // FIXME uri implies multiple repositories, remove it?
            return (Iterable<Branch>) branchFactory.getBranches();
        } finally {
            transactionManager.closeReadTransaction();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Repository#getContents(org.eclipse.emf.common.util.URI)
     */
    @Deprecated
    @Override
    public Iterable<URI> getContents(final URI uri) throws MalformedURIException {
        String branchname = null;
        long revisionId = -1;
        boolean hasBranch = true, hasRevision = true;
        // what does this uri point to?
        try {
            // has it a branch?
            branchname = uriHandler.extractBranchName(uri);
        } catch (final MalformedURIException e) {
            hasBranch = false;
        }
        try {
            // has it a revision?
            revisionId = uriHandler.extractRevision(uri);
        } catch (final MalformedURIException e) {
            hasRevision = false;
        }
        if (!hasBranch) {
            // find all branchnames, convert them into uris
            return Iterables.transform(branchFactory.getBranches(), new Function<Branch, URI>() {
                @Override
                public URI apply(final Branch branch) {
                    return uriHandler.createUriFor(branch);
                }
            });
        } else if (!hasRevision) {
            // find all revisions of the branch, convert them into uris
            final Branch branch = branchFactory.getBranch(branchname);
            return Iterables.transform(branch.getRevisions(), new Function<Revision, URI>() {
                @Override
                public URI apply(final Revision r) {
                    return uriHandler.createUriFor(branch, r.getRevisionId());
                }
            });
        } else {
            // find all alive models, convert them into uris
            final Collection<URI> result = Lists.newArrayList();
            final Branch branch = branchFactory.getBranch(branchname);
            final Revision rev = branch.getRevision(revisionId);
            if (rev == null) {
                throw new MalformedURIException("Unknown revision: " + uri);
            } else {
                // TODO also show changed (and deleted?) models
                final Iterable<URI> knownModels = Iterables.concat(
                        rev.getModelReferences(ChangeType.ADDED));
                for (final URI modelUri : knownModels) {
                    if (uriHandler.isPrefix(uri, modelUri)) {
                        result.add(uriHandler.trimToNextSegment(uri, modelUri));
                    }
                }
                return result;

            }

        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Repository#getDependencies(org.eclipse.emf.common.util.URI)
     */
    @Override
    public Iterable<URI> getDependencies(final URI uri) throws MalformedURIException {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Repository#getRevision(org.eclipse.emf.common.util.URI)
     */
    @Override
    public Revision getRevision(final URI uri) throws MalformedURIException {
        transactionManager.startReadTransaction();
        try {

            final Branch branch = branchFactory.getBranch(uriHandler.extractBranchName(uri));

            return branch.getRevision(uriHandler.extractRevision(uri));
        } finally {
            transactionManager.closeReadTransaction();
        }
    }

    /**
     * @param uri
     * @return
     * @throws MalformedURIException
     */
    private Storage getStorage(final URI uri) throws MalformedURIException {
        final String branchname = uriHandler.extractBranchName(uri);
        final Branch branch = branchFactory.getBranch(branchname);
        final Storage storage = storageFactory.getStorage(branch);
        return storage;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Repository#rollbackTransaction(org.infai.amor.backend.Transaction)
     */
    @Override
    public void rollbackTransaction(final CommitTransaction tr) {
        transactionManager.rollback(tr);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Repository#startTransaction()
     */
    @Override
    public CommitTransaction startCommitTransaction(final Branch branch) {
        final CommitTransaction tr = transactionManager.startCommitTransaction(branch);
        return tr;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Repository#view(org.eclipse.emf.common.util.URI)
     */
    @Override
    public EObject view(final URI uri) throws IOException {
        transactionManager.startReadTransaction();
        try {

            final Storage storage = getStorage(uri);
            return storage.view(uriHandler.extractModelPathFrom(uri), uriHandler.extractRevision(uri));
        } finally {
            transactionManager.closeReadTransaction();
        }
    }

}
