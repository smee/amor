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

import java.io.*;
import java.util.*;

import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceFactoryImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.PackageNotFoundException;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xml.type.internal.DataValue.URI.MalformedURIException;
import org.infai.amor.backend.*;
import org.infai.amor.backend.Revision.ChangeType;
import org.infai.amor.backend.internal.*;
import org.infai.amor.backend.responses.UnresolvedDependencyResponse;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * @author sdienst
 *
 */
public class SimpleRepositoryImpl implements SimpleRepository {
    /*
     * TODOS:
     * - remember package nsuris for stored ecores, within transaction as well as within revisions
     * - load package prior to loading a model instance
     * - clean up api
     */
    final Repository repo;
    final UriHandler uh;
    /**
     * TODO neo4j transactions are bound to a specific thread, need to cope with!
     */
    final Map<Long, CommitTransaction> transactionMap = Maps.newHashMap();


    public SimpleRepositoryImpl(final Repository r, final UriHandler uh) {
        this.repo = r;
        this.uh = uh;
    }
    /* (non-Javadoc)
     * @see org.infai.amor.backend.SimpleRepository#checkinEcore(java.lang.String, java.lang.String)
     */
    @Override
    public List<String> checkin(final String ecoreXmi, final String relativePath, final long transactionId) {
        final ResourceSet rs = createResourceSet();
        final URI fileUri = URI.createURI(relativePath);
        if (!fileUri.isRelative()) {
            throw new IllegalArgumentException("Path must be relative");
        }
        final Resource resource = rs.createResource(fileUri);

        final CommitTransaction transaction = this.transactionMap.get(transactionId);
        while (!resource.isLoaded()) {
            try {
                resource.load(new ByteArrayInputStream(ecoreXmi.getBytes()), null);

                final Response response = repo.checkin(new ModelImpl(resource.getContents(), new Path(relativePath)), transaction);

                if (response instanceof UnresolvedDependencyResponse) {
                    return extractMissingDependencies((UnresolvedDependencyResponse) response);
                }
            } catch (final IOException e) {
                final Throwable cause = e.getCause();
                if (cause instanceof PackageNotFoundException) {
                    // we are missing at least one other epackage
                    final String missingPackageUri = ((PackageNotFoundException) cause).uri();
                    if (weKnowThisPackage(missingPackageUri, transaction)) {
                        try {
                            loadEPackage(rs, missingPackageUri, (InternalCommitTransaction) transaction);
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
            }
        }
        return Collections.EMPTY_LIST;
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.SimpleRepository#checkoutEcore(long, java.lang.String)
     */
    @Override
    public String checkout(final String branch, final long revisionId, final String relativePath) throws IOException {
        Preconditions.checkNotNull(branch);

        try {
            final URI uriForRevision = uh.createUriFor(repo.getBranch(uh.createUriFor(branch)), revisionId);
            final Model model = repo.checkout(uriForRevision.appendSegments(relativePath.split("/")));

            return serializeModel(model.getContent());

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
        if(transaction == null) {
            throw new IllegalArgumentException("no such transaction!");
        }
        return transaction;
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.SimpleRepository#commitTransaction(long)
     */
    @Override
    public long commitTransaction(final long transactionId, final String username, final String commitMessage) throws Exception {
        Preconditions.checkNotNull(username);
        Preconditions.checkNotNull(commitMessage);

        final CommitTransaction transaction = checkTransaction(transactionId);
        transaction.setUser(username);
        transaction.setCommitMessage(commitMessage);
        try{
            final Response response = repo.commitTransaction(transaction);
            // TODO handle responses
            return transaction.getRevision().getRevisionId();
        } finally {
            transactionMap.remove(transactionId);
        }
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.SimpleRepository#createBranch(java.lang.String)
     */
    @Override
    public void createBranch(final String newBranchname, final String oldbranchname, final long startRevisionId) {
        if (oldbranchname == null || startRevisionId < 0) {
            repo.createBranch(null, newBranchname);
        } else {
            try {
                final Revision rev = repo.getRevision(uh.createUriFor(repo.getBranch(uh.createUriFor(oldbranchname)), startRevisionId));
                repo.createBranch(rev, newBranchname);
            } catch (final MalformedURIException e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * @param contents
     * @return
     */
    private Map<String, Object> createPackageNamespaceMap(final EList<? extends EObject> contents) {
        final Map<String, Object> res = Maps.newHashMap();
        for(final EObject eo: contents){
            if(eo instanceof EPackage){
                final EPackage epckg = (EPackage) eo;
                res.put(epckg.getNsURI(), epckg);
                res.putAll(createPackageNamespaceMap(epckg.getESubpackages()));
            }
        }
        return res;
    }

    /**
     * @return
     */
    private ResourceSet createResourceSet() {
        final ResourceSet rs =  new ResourceSetImpl();
        rs.getResourceFactoryRegistry().getProtocolToFactoryMap().put("amor", new ResourceFactoryImpl(){
            @Override
            public Resource createResource(final URI uri){
                // TODO return resource that reads from our storage
                return super.createResource(uri);
            }
        });
        return rs;
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
     * @see org.infai.amor.backend.SimpleRepository#getBranches()
     */
    @Override
    public String[] getBranches(final String uri) {
        try {
            return toArray(transform(repo.getBranches(URI.createURI(uri)), getBranchName()), String.class);
        } catch (final MalformedURIException e) {
            e.printStackTrace();
            return new String[0];
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
        Revision rev = transaction.getBranch().getHeadRevision();
        while (rev != null) {
            for (final ModelLocation loc : rev.getModelReferences(ChangeType.ADDED, ChangeType.CHANGED, ChangeType.DELETED)) {

                if (loc.isMetaModel() && loc.getNamespaceUris().contains(ePackageUri)) {
                    if (loc.getChangeType().equals(Revision.ChangeType.DELETED)) {
                        // if the newest change to this relative path was a deletion,
                        // we do not have this model stored
                        return null;
                    } else {
                        return loc.getExternalUri();
                    }
                }
            }
            rev = rev.getPreviousRevision();
        }
        return null;
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
        final Resource res = rs.createResource(URI.createURI(checkout.getPersistencePath().toString()));
        res.getContents().addAll(checkout.getContent());
        rs.getPackageRegistry().putAll(createPackageNamespaceMap(res.getContents()));
    }
    /* (non-Javadoc)
     * @see org.infai.amor.backend.SimpleRepository#rollbackTransaction(long)
     */
    @Override
    public void rollbackTransaction(final long transactionId) throws Exception {
        final CommitTransaction transaction = checkTransaction(transactionId);
        repo.rollbackTransaction(transaction);
        transactionMap.remove(transactionId);

    }

    /**
     * @param content
     * @return
     * @throws IOException
     */
    private String serializeModel(final List<EObject> contents) throws IOException {
        final ResourceSet rs = createResourceSet();
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());

        final Resource res = rs.createResource(URI.createURI("foo.ecore"));
        res.getContents().addAll(contents);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        res.save(baos, transactionMap);
        return baos.toString();
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.SimpleRepository#startTransaction(java.lang.String)
     */
    @Override
    public long startTransaction(final String branchname) {
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
        return getExternalUriForEPackage(transaction,ePackageUri)!=null;
    }


}
