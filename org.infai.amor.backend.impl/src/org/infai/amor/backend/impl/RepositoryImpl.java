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
import java.util.*;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.xml.type.internal.DataValue.URI.MalformedURIException;
import org.infai.amor.backend.*;
import org.infai.amor.backend.Revision.ChangeType;
import org.infai.amor.backend.api.RevisionInfo;
import org.infai.amor.backend.internal.*;
import org.infai.amor.backend.responses.*;
import org.infai.amor.backend.storage.Storage;
import org.infai.amor.backend.storage.StorageFactory;
import org.infai.amor.backend.util.*;
import org.infai.amor.backend.util.ModelFinder.ModelMatcher;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;

/**
 * Default implementation of the amor repository backend.
 * 
 * @author sdienst<br>
 *         TODO do the collaborators need to be informed upon transaction commits?
 */
public class RepositoryImpl implements Repository {
    public static Logger logger = Logger.getLogger(RepositoryImpl.class.getName());

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
            storageFactory.getStorage(tr).checkin(model, modeluri, tr.getRevision());

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
            // what is the externally referenceable uri of this model, if it would get persisted?
            final URI modeluri = uriHandler.createModelUri(tr, model.getPersistencePath());
            final Storage storage = storageFactory.getStorage(tr);

            final Collection<URI> dependencies = findUnknownModelDependenciesOf(model, tr);
            // store the model

            storage.checkin(model, modeluri, tr.getRevision());

            if (!dependencies.isEmpty()) {
                // do not store model yet, ask for its dependencies
                return new UnresolvedDependencyResponse("Model not stored! Please checkin the dependencies of this model.", modeluri, dependencies);
            } else {
                return new CheckinResponse("Success.", modeluri);
            }
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
            Storage storage = getStorage(uri);
            Branch branch = branchFactory.getBranch(uriHandler.extractBranchName(uri));
            long revisionId = getRevisionId(uri);
            Revision revision = branch.getRevision(revisionId);

            return storage.checkout(uriHandler.extractModelPathFrom(uri), revision);
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
    public Response commitTransaction(CommitTransaction tr) {
        return transactionManager.commit(tr);
    }

    /**
     * @param rev
     * @param uri
     * @throws MalformedURIException
     */
    private Collection<URI> constructActiveRepositoryContents(Revision rev, final URI uri)
    throws MalformedURIException {
        final Map<IPath, Pair<Long, URI>> activeModels = Maps.newHashMap();
        final Map<IPath, Pair<Long, URI>> removedModels = Maps.newHashMap();
        // cycle through to all older revisions
        while (rev != null) {
            final Iterable<ModelLocation> addedModels = Iterables.concat(rev
                    .getModelReferences(ChangeType.ADDED), rev
                    .getModelReferences(ChangeType.CHANGED));
            final Iterable<ModelLocation> deletedModels = rev.getModelReferences(ChangeType.DELETED);

            for (final ModelLocation deletedModel : deletedModels) {
                removedModels.put(uriHandler.extractModelPathFrom(deletedModel.getExternalUri()),
                        new Pair(rev.getRevisionId(), deletedModel));
            }
            for (final ModelLocation touchedModel : addedModels) {
                // extract model path+name
                final IPath path = uriHandler.extractModelPathFrom(touchedModel.getExternalUri());
                // was it deleted in a later revision?
                final Pair<Long, URI> p = removedModels.get(path);
                if (p == null || p.first < rev.getRevisionId()) {
                    // was never deleted or was deleted prior to the current
                    // revision
                    if (!activeModels.containsKey(path)) {
                        activeModels.put(path, new Pair(rev.getRevisionId(), touchedModel.getExternalUri()));
                    }
                } else {
                    // will get deleted in a later revision, not active at this
                    // rev
                    // activeModels.remove(path);
                }
            }

            rev = rev.getPreviousRevision();
        }
        final Collection<URI> result = Sets.newHashSet();
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
    public Branch createBranch(Revision parent, String name) {
        return branchFactory.createBranch(parent, name);
    }

    /**
     * @param revision
     * @return
     */
    private RevisionInfo createRevisionInfo(Revision rev) {
        return new RevisionInfo(
                rev.getUser(),
                rev.getCommitMessage(),
                rev.getCommitTimestamp().getTime(),
                getPaths(rev.getModelReferences(Revision.ChangeType.ADDED)),
                getPaths(rev.getModelReferences(Revision.ChangeType.CHANGED)),
                getPaths(rev.getModelReferences(Revision.ChangeType.DELETED)));
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.Repository#deleteModel(org.eclipse.core.runtime.IPath, org.infai.amor.backend.CommitTransaction)
     */
    @Override
    public Response deleteModel(final IPath modelPath, final CommitTransaction tr) throws IOException {
        try {
            final URI modeluri = uriHandler.createModelUri(tr, modelPath);
            // test, if model really exists
            ModelLocation modelLocation = ModelFinder.findActiveModel(tr.getRevision(), new ModelMatcher() {
                String toDeletePath=modelPath.toString();
                boolean wasDeleted=false;
                @Override
                public boolean matches(ModelLocation loc) {
                    if (!wasDeleted && loc.getExternalUri().toString().endsWith(toDeletePath)) {
                        if(loc.getChangeType()==ChangeType.DELETED){
                            wasDeleted=true;
                            return false;
                        } else {
                            return true;
                        }
                    }
                    return false;
                }
            });
            if (modelLocation == null) {
                return new DeleteErrorResponse("No such persisted model! Please delete only existing models.", uriHandler.createModelUri(tr, modelPath));
            } else {
                storageFactory.getStorage(tr).delete(modelPath, modeluri, tr.getRevision());
                return new DeleteSuccessResponse("Model deleted successfully", uriHandler.createModelUri(tr, modelPath));
            }
        } catch (final IOException ioe) {
            ioe.printStackTrace();
            return new DeleteErrorResponse(ioe.getMessage(), uriHandler.createModelUri(tr, modelPath));
        }
    }

    /**
     * Find all models that get referenced by any eobject within the containment hierarchies of each root element in contents.
     * 
     * @param model
     *            .getContent() list of model root elements
     * @param transaction
     * @return
     */
    private Collection<URI> findUnknownModelDependenciesOf(final Model model, final CommitTransaction transaction) {
        final Set<URI> refs = Sets.newHashSet();
        for (final EObject root : model.getContent()) {
            // find relative uris to referenced models
            // see rfc2396 5.2.6.b: everything past the last / will get excluded from the resolution process
            // strip down uri to make it point to the first common segment

            final Set<URI> referencedModels = EcoreModelHelper.findReferencedModels(root, root.eResource().getURI());
            for (final URI refuri : referencedModels) {
                assert refuri.isRelative();
                // if we don't have a copy of this model yet
                final URI modelUri = URI.createURI(model.getPersistencePath().toString());
                if (!isKnownModel(EcoreModelHelper.normalizeUri(modelUri.trimSegments(1).appendSegments(refuri.segments())), transaction)) {
                    // remember it
                    refs.add(refuri);
                }
            }
        }
        return refs;
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
                revisionId = getRevisionId(uri);
            } catch (final MalformedURIException e) {
                hasRevision = false;
            }
            final Collection<URI> result = Lists.newLinkedList();
            if (!hasBranch) {
                // find all branchnames, convert them into uris
                for (final Branch branch : branchFactory.getBranches()) {
                    result.add(uriHandler.createUriFor(branch));
                }
            } else if (!hasRevision) {
                // find all revisions of the branch, convert them into uris
                final Branch branch = branchFactory.getBranch(branchname);
                for (final Revision r : branch.getRevisions()) {
                    result.add(uriHandler.createUriFor(branch, r.getRevisionId()));
                }
            } else {
                // find all alive models, convert them into uris
                final Branch branch = branchFactory.getBranch(branchname);
                final Revision rev = branch.getRevision(revisionId);
                if (rev == null) {
                    throw new MalformedURIException("Unknown revision: " + uri);
                } else {
                    result.addAll(constructActiveRepositoryContents(rev, uri));
                }

            }
            return result;
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
     * @see org.infai.amor.backend.Repository#getDependencies(org.eclipse.emf.common.util.URI)
     */
    @Override
    public Iterable<URI> getDependencies(final URI uri) throws MalformedURIException {
        throw new UnsupportedOperationException();
    }

    /**
     * @param modelReferences
     * @return
     */
    private Collection<String> getPaths(Collection<ModelLocation> refs) {
        Collection<String> paths=new ArrayList<String>(refs.size());
        for(ModelLocation ref:refs) {
            paths.add(ref.getRelativePath());
        }
        return paths;
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

            return branch.getRevision(getRevisionId(uri));
        } finally {
            transactionManager.closeReadTransaction();
        }
    }

    /**
     * Returns the revision id content of the uri. This id should be numeric. Also HEAD is valid, it references the most recent
     * revision of a branch.
     * 
     * @param uri
     * @return
     */
    private long getRevisionId(URI uri) throws MalformedURIException{
        if(uri.segmentCount()>2 && uri.segment(2).toUpperCase().equals("HEAD")){
            return branchFactory.getBranch(uriHandler.extractBranchName(uri)).getHeadRevision().getRevisionId();
        } else {
            return uriHandler.extractRevision(uri);
        }
    }

    /**
     * FIXME Internal call, needed because every call to any api object needs to be wrapped in neo transactions right now.
     * 
     * @param uri
     * @return
     * @throws MalformedURIException
     */
    public RevisionInfo getRevisionInfo(final URI uri) throws MalformedURIException {
        transactionManager.startReadTransaction();
        try {

            final Branch branch = branchFactory.getBranch(uriHandler.extractBranchName(uri));

            return createRevisionInfo(branch.getRevision(getRevisionId(uri)));
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

    /**
     * Do we know this model?
     * 
     * @param transaction
     * 
     * @param refuri
     * @return
     */
    private boolean isKnownModel(final URI relativeRefToModelUri, final CommitTransaction transaction) {
        final String relativePath = relativeRefToModelUri.toString();
        final Revision rev = transaction.getBranch().getHeadRevision();
        final ModelLocation loc = ModelFinder.findActiveModel(rev, new ModelMatcher() {
            @Override
            public boolean matches(final ModelLocation loc) {
                return loc.getRelativePath().equals(relativePath);
            }
        });
        return loc != null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Repository#rollbackTransaction(org.infai.amor.backend.Transaction)
     */
    @Override
    public void rollbackTransaction(final CommitTransaction tr) {
        Preconditions.checkNotNull(tr);
        transactionManager.rollback(tr);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Repository#startTransaction()
     */
    @Override
    public CommitTransaction startCommitTransaction(final Branch branch) {
        Preconditions.checkNotNull(branch, "Please specify a branch for this transaction.");
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
            final Revision revision = branchFactory.getBranch(uriHandler.extractBranchName(uri)).getRevision(getRevisionId(uri));
            return storage.view(uriHandler.extractModelPathFrom(uri), revision);
        } finally {
            transactionManager.closeReadTransaction();
        }
    }

}
