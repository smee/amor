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
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.PackageNotFoundException;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xml.type.internal.DataValue.URI.MalformedURIException;
import org.infai.amor.backend.*;
import org.infai.amor.backend.internal.ModelImpl;
import org.infai.amor.backend.internal.UriHandler;
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
    final Repository repo;
    final UriHandler uh;
    final Map<Long, CommitTransaction> transactionMap = Maps.newHashMap();


    public SimpleRepositoryImpl(final Repository r, final UriHandler uh) {
        this.repo = r;
        this.uh = uh;
    }
    /* (non-Javadoc)
     * @see org.infai.amor.backend.SimpleRepository#checkinEcore(java.lang.String, java.lang.String)
     */
    @Override
    public List<String> checkinEcore(final String ecoreXmi, final String relativePath, final long transactionId) {
        final ResourceSet rs = new ResourceSetImpl();
        final URI fileUri = URI.createURI(relativePath);
        if (!fileUri.isRelative()) {
            throw new IllegalArgumentException("Path must be relative");
        }
        final Resource resource = rs.createResource(fileUri);

        final CommitTransaction transaction = this.transactionMap.get(transactionId);
        while (!resource.isLoaded()) {
            try {
                resource.load(new ByteArrayInputStream(ecoreXmi.getBytes()), transactionMap);

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
                        loadEPackage(rs.createResource(URI.createURI(missingPackageUri)));
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
    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.SimpleRepository#checkinXmi(java.lang.String, java.lang.String)
     */
    @Override
    public List<String> checkinXmi(final String xmi, final String relativePath, final long transactionId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.SimpleRepository#checkoutEcore(long, java.lang.String)
     */
    @Override
    public String checkoutEcore(final String branch, final long revisionId, final String relativePath) throws IOException {
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

        final Response response = repo.commitTransaction(transaction);
        // TODO handle responses
        transactionMap.remove(transactionId);
        return transaction.getRevisionId();
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.SimpleRepository#createBranch(java.lang.String)
     */
    @Override
    public void createBranch(final String branchname, final long startRevisionId) {
        // TODO respect the revisionid
        repo.createBranch(null, branchname);
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
     * @param createResource
     */
    private void loadEPackage(final Resource res) {
        final URI nsUri = res.getURI();
        // TODO find ecore that contains this namespace uri
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
        final ResourceSet rs = new ResourceSetImpl();
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
            transactionMap.put(tr.getRevisionId(), tr);
            return tr.getRevisionId();
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
        // return ((CommitTransactionImpl) transaction).hasStoredModel(ePackageUri) ||
        // transaction.getBranch().findRevisionOf(relPath) != null;
        return false;
    }


}
