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

import static com.google.common.collect.Iterables.toArray;
import static com.google.common.collect.Iterables.transform;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.epatch.Epatch;
import org.eclipse.emf.compare.epatch.impl.EpatchPackageImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.xmi.PackageNotFoundException;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xml.type.internal.DataValue.URI.MalformedURIException;
import org.infai.amor.backend.*;
import org.infai.amor.backend.api.RevisionInfo;
import org.infai.amor.backend.api.SimpleRepository;
import org.infai.amor.backend.internal.*;
import org.infai.amor.backend.resources.AmorResourceSetImpl;
import org.infai.amor.backend.responses.UnresolvedDependencyResponse;
import org.infai.amor.backend.util.EcoreModelHelper;
import org.infai.amor.backend.util.ModelFinder;
import org.infai.amor.backend.util.ModelFinder.ModelMatcher;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.*;

/**
 * @author sdienst
 * 
 */
/**
 * @author sdienst
 * 
 */
public class SimpleRepositoryImpl implements SimpleRepository {
    private static Logger logger = Logger.getLogger(SimpleRepositoryImpl.class.getName());
    /*
     * TODOS: - register uri type amor:// with resourcesets - access models with such uris - provide uri without revision ids (?)
     */
    final Repository repo;
    final UriHandler uh;
    /**
     * TODO neo4j transactions are bound to a specific thread, need to cope with!
     */
    final Map<Long, CommitTransaction> transactionMap = Maps.newHashMap();
    /**
     * XXX dirty hack: read transaction should be handled within api objects (neobranch etc.)
     */
    private TransactionManager tm;

    public SimpleRepositoryImpl(final Repository r, final UriHandler uh) {
        this.repo = r;
        this.uh = uh;
    }

    public SimpleRepositoryImpl(final Repository r, final UriHandler uh, TransactionManager tm) {
        this.repo = r;
        this.uh = uh;
        this.tm = tm;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.SimpleRepository#checkinEcore(java.lang.String, java.lang.String)
     */
    @Override
    public List<String> checkin(final String ecoreXmi, final String relativePath, final long transactionId) {
        Preconditions.checkNotNull(ecoreXmi, "Missing serialized model!");
        Preconditions.checkNotNull(relativePath, "Missing relative path for this model file!");
        Preconditions.checkArgument(relativePath.length() > 0, "Missing relative path for this model file!");

        final ResourceSet rs = createResourceSet();
        final URI fileUri = URI.createURI(relativePath);
        if (!fileUri.isRelative()) {
            throw new IllegalArgumentException("Path must be relative");
        }
        logger.finer("Storing new model with path " + relativePath);

        final Resource resource = rs.createResource(fileUri);

        final CommitTransaction transaction = checkTransaction(transactionId);
        while (!(resource.isLoaded() && resource.getErrors().isEmpty())) {
            try {
                resource.load(new ByteArrayInputStream(ecoreXmi.getBytes()), getLoadOptions());
            } catch (final IOException e) {
                final Throwable cause = e.getCause();
                if (cause instanceof PackageNotFoundException) {
                    // we are missing at least one other epackage
                    final String missingPackageUri = ((PackageNotFoundException) cause).uri();
                    if (weKnowThisPackage(missingPackageUri, transaction)) {
                        try {
                            logger.finer("Restoring metamodel package " + missingPackageUri);
                            loadEPackage(rs, missingPackageUri, (InternalCommitTransaction) transaction);
                            // clear load status
                            resource.unload();
                        } catch (final MalformedURIException e1) {
                            throw new RuntimeException("Internal error!", e1);
                        } catch (final IOException e1) {
                            throw new RuntimeException("Internal error!", e1);
                        }
                        continue;
                    } else {
                        return Arrays.asList(missingPackageUri);
                    }
                } else {
                    throw new RuntimeException(e);
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Could not load model", e);
                throw new RuntimeException("Internal error in the backend. Could not load model!");
            }

        }
        final Response response = repo.checkin(new ModelImpl(resource.getContents(), new Path(relativePath)), transaction);

        if (response instanceof UnresolvedDependencyResponse) {
            return extractMissingDependencies((UnresolvedDependencyResponse) response);
        }
        return Collections.EMPTY_LIST;
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.SimpleRepository#checkinPatch(java.lang.String, java.lang.String, long)
     */
    @Override
    public void checkinPatch(final String epatch, final String relativePath, final long transactionId) throws RuntimeException {
        Preconditions.checkNotNull(epatch, "Missing serialized epatch!");
        Preconditions.checkNotNull(relativePath, "Missing relative path for this model file!");
        Preconditions.checkArgument(relativePath.length() > 0, "Missing relative path for this model file!");

        final CommitTransaction transaction = checkTransaction(transactionId);

        final ResourceSet rs = createResourceSet();
        EpatchPackageImpl.init();
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("epatch", new XMIResourceFactoryImpl());
        final Resource resource = rs.createResource(URI.createURI("patch.epatch"));
        try {
            resource.load(new ByteArrayInputStream(epatch.getBytes()), getLoadOptions());
            final Response response = repo.checkin(new ChangedModelImpl((Epatch) resource.getContents().get(0), relativePath), transaction);
            // TODO what to do with it?
        } catch (final IOException e) {
            logger.log(Level.SEVERE, "Could not read epatch.", e);
            throw new RuntimeException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.SimpleRepository#checkoutEcore(long, java.lang.String)
     */
    @Override
    public String checkout(final String branch, final long revisionId, final String relativePath) throws IOException {
        Preconditions.checkNotNull(branch, "Missing branch name!");
        Preconditions.checkNotNull(relativePath, "Missing relative model path!");

        try {
            final URI uriForRevision = uh.createUriFor(repo.getBranch(uh.createUriFor(branch)), revisionId);
            final Model model = repo.checkout(uriForRevision.appendSegments(relativePath.split("/")));

            return EcoreModelHelper.serializeModel(model.getContent(), relativePath);

        } catch (final MalformedURIException e) {
            throw new IllegalArgumentException(String.format("Could not find revision '%d' on branch '%s'", revisionId, branch), e);
        }
    }

    /**
     * @param transactionId
     * @return
     */
    private CommitTransaction checkTransaction(final long transactionId) {
        final CommitTransaction transaction = transactionMap.get(transactionId);
        if (transaction == null) {
            throw new IllegalArgumentException("no such transaction!");
        }
        return transaction;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.SimpleRepository#commitTransaction(long)
     */
    @Override
    public long commitTransaction(final long transactionId, final String username, final String commitMessage) throws Exception {
        Preconditions.checkNotNull(username);
        Preconditions.checkNotNull(commitMessage);

        final CommitTransaction transaction = checkTransaction(transactionId);
        transaction.setUser(username);
        transaction.setCommitMessage(commitMessage);
        try {
            final Response response = repo.commitTransaction(transaction);
            // TODO handle responses
            return transaction.getRevision().getRevisionId();
        } finally {
            transactionMap.remove(transactionId);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.SimpleRepository#createBranch(java.lang.String)
     */
    @Override
    public void createBranch(final String newBranchname, final String oldbranchname, final long startRevisionId) {
        Preconditions.checkNotNull(newBranchname, "Missing new branch name!");

        if (oldbranchname == null || startRevisionId < 0) {
            repo.createBranch(null, newBranchname);
        } else {
            try {
                final Revision rev = repo.getRevision(uh.createUriFor(repo.getBranch(uh.createUriFor(oldbranchname)), startRevisionId));
                final Branch newbranch = repo.createBranch(rev, newBranchname);
                // TODO return anything?
            } catch (final MalformedURIException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @return
     */
    private ResourceSet createResourceSet() {
        final ResourceSet rs = new AmorResourceSetImpl();
        // rs.getResourceFactoryRegistry().getProtocolToFactoryMap().put("amor", new ResourceFactoryImpl() {
        // @Override
        // public Resource createResource(final URI uri) {
        // // TODO return resource that reads from our storage
        // return super.createResource(uri);
        // }
        // });
        return rs;
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.SimpleRepository#delete(long, java.lang.String)
     */
    @Override
    public void delete(final long transactionId, final String relativePath) {
        Preconditions.checkNotNull(relativePath, "Missing relative model path!");

        final CommitTransaction transaction = checkTransaction(transactionId);
        try {
            final Response response = repo.deleteModel(new Path(relativePath), transaction);
            // TODO inform caller about success/failure.
        } catch (final IOException e) {
            logger.log(Level.SEVERE, "Could not delete model at path=" + relativePath, e);
        }

    }

    /**
     * @param response
     * @return
     */
    private List<String> extractMissingDependencies(final UnresolvedDependencyResponse response) {
        final Collection<URI> dependencies = response.getDependencies();
        final List<String> missingDeps = Lists.newArrayList();
        for (final URI uri : dependencies) {
            missingDeps.add(uri.toString());
        }
        return missingDeps;
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.SimpleRepository#getActiveContents(java.lang.String)
     */
    @Override
    public List<String> getActiveContents(final String uri) {
        Preconditions.checkNotNull(uri, "Missing uri to check contents of!");

        final List<String> res = Lists.newArrayList();
        try {
            for (final URI u : repo.getActiveContents(URI.createURI(uri))) {
                res.add(u.toString());
            }
        } catch (final MalformedURIException e) {
            logger.severe(String.format("Uri '%s' is not valid for contents of a AMOR repository.", uri));
        }
        return res;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.SimpleRepository#getBranches()
     */
    @Override
    public String[] getBranches() {
        try {
            Iterable<Branch> branches = repo.getBranches(uh.getDefaultUri());

            if (tm != null) {
                tm.startReadTransaction();
            }
            return toArray(transform(branches,
                    getBranchName()),
                    String.class);
        } catch (final MalformedURIException e) {
            e.printStackTrace();
            return new String[0];
        } finally {
            if (tm != null) {
                tm.closeReadTransaction();
            }
        }
    }

    /**
     * @return
     */
    private Function<Branch, String> getBranchName() {
        return new Function<Branch, String>() {

            @Override
            public String apply(final Branch branch) {
                return branch.getName();
            }
        };
    }

    /**
     * @param ePackageUri
     * @return
     */
    private URI getExternalUriForEPackage(final CommitTransaction transaction, final String ePackageUri) {
        final Revision rev = transaction.getBranch().getHeadRevision();
        final ModelLocation loc = ModelFinder.findActiveModel(rev, new ModelMatcher() {
            @Override
            public boolean matches(final ModelLocation loc) {
                return loc.isMetaModel() && loc.getNamespaceUris().contains(ePackageUri);
            }
        });

        if (loc != null) {
            return loc.getExternalUri();
        } else {
            return null;
        }
    }

    /**
     * @return
     */
    private Map<?, ?> getLoadOptions() {
        return ImmutableMap.of(XMLResource.OPTION_EXTENDED_META_DATA, true, XMLResource.OPTION_ENCODING, "UTF-8");
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.api.SimpleRepository#getRevisionInfo(java.lang.String, long)
     */
    @Override
    public RevisionInfo getRevisionInfo(String branchname, long revisionId) {
        Preconditions.checkNotNull(branchname, "Missing branchname!");
        Preconditions.checkArgument(revisionId >= 0, "No such revision!");
        try {
            // XXX ugly hack
            return ((RepositoryImpl) repo).getRevisionInfo(uh.createUriFor(repo.getBranch(uh.createUriFor(branchname)), revisionId));
        } catch (final MalformedURIException e) {
            logger.severe(String.format("Used invalid uri for accessing model contents on branch '%s', revisionId '%d'", branchname, revisionId));
            return null;
        }
    }

    /**
     * @param rs
     * @param missingPackageUri
     * @param transaction
     * @throws IOException
     * @throws MalformedURIException
     */
    private void loadEPackage(final ResourceSet rs, final String missingPackageUri, final InternalCommitTransaction transaction) throws MalformedURIException, IOException {
        final URI externalUriForEPackage = getExternalUriForEPackage(transaction, missingPackageUri);

        final Model checkout = repo.checkout(externalUriForEPackage);
        if (!checkout.getContent().isEmpty()) {
            /* XXX move all resources that were restored to our resource set
             * This is needed because EMF does not throw a PackageNotFoundException
             * if a supertype of an eclass depends on types in another resource, i.e. if
             * the supertype is a proxy :(
             * 
             * TODO think about return type of repository.checkout(...)!
             */
            final ResourceSet otherResourceSet = checkout.getContent().get(0).eResource().getResourceSet();
            rs.getResources().addAll(otherResourceSet.getResources());
            for (final Resource res : rs.getResources()) {
                rs.getPackageRegistry().putAll(EcoreModelHelper.createPackageNamespaceMap(res.getContents()));
            }
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.SimpleRepository#rollbackTransaction(long)
     */
    @Override
    public void rollbackTransaction(final long transactionId) throws Exception {
        final CommitTransaction transaction = checkTransaction(transactionId);
        repo.rollbackTransaction(transaction);
        transactionMap.remove(transactionId);

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.SimpleRepository#startTransaction(java.lang.String)
     */
    @Override
    public long startTransaction(final String branchname) {
        Preconditions.checkNotNull(branchname, "Missing branchname!");

        try {
            final CommitTransaction tr = repo.startCommitTransaction(repo.getBranch(uh.createUriFor(branchname)));
            final long revisionId = tr.getRevision().getRevisionId();
            transactionMap.put(revisionId, tr);
            return revisionId;
        } catch (final MalformedURIException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param transaction
     * @param missingPackageUri
     * @return
     */
    private boolean weKnowThisPackage(final String ePackageUri, final CommitTransaction transaction) {
        return getExternalUriForEPackage(transaction, ePackageUri) != null;
    }

}
