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

import java.util.Collection;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xml.type.internal.DataValue.URI.MalformedURIException;
import org.infai.amor.backend.Branch;
import org.infai.amor.backend.ChangedModel;
import org.infai.amor.backend.Model;
import org.infai.amor.backend.Repository;
import org.infai.amor.backend.Response;
import org.infai.amor.backend.Revision;
import org.infai.amor.backend.Transaction;
import org.infai.amor.backend.branch.BranchFactory;
import org.infai.amor.backend.storage.Storage;

/**
 * Default implementation of the amor repository backend.
 * 
 * @author sdienst
 * 
 */
public class RepositoryImpl implements Repository {

    private final Storage storage;
    private final BranchFactory branchFactory;

    /**
     * @param storage
     */
    public RepositoryImpl(final Storage storage, final BranchFactory bf) {
        this.storage = storage;
        this.branchFactory = bf;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Repository#checkin(org.infai.amor.backend.ChangedModel, org.infai.amor.backend.Branch,
     * org.infai.amor.backend.Transaction)
     */
    @Override
    public Response checkin(final ChangedModel model, final Branch branch, final Transaction tr) {
        // TODO branches!
        return storage.checkin(model, tr);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Repository#checkin(org.infai.amor.backend.Model, org.infai.amor.backend.Branch,
     * org.infai.amor.backend.Transaction)
     */
    @Override
    public Response checkin(final Model model, final Branch branch, final Transaction tr) {
        // TODO branches!
        return storage.checkin(model, tr);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Repository#checkout(org.eclipse.emf.common.util.URI)
     */
    @Override
    public Model checkout(final URI uri) throws MalformedURIException {
        // TODO branches
        // TODO - find matching storage for uri
        // TODO - find better way to communicate with storage
        return storage.checkout(uri);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Repository#commitTransaction(org.infai.amor.backend.Transaction)
     */
    @Override
    public Response commitTransaction(final Transaction tr) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Repository#createBranch(org.infai.amor.backend.Branch)
     */
    @Override
    public Branch createBranch(final Branch parent, final String name) {
        return branchFactory.createBranch(parent, name);
    }

    /**
     * @param uri
     * @return
     */
    private String extractBranchName(final URI uri) throws MalformedURIException {
        // TODO
        // return null;
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Repository#getBranch(org.eclipse.emf.common.util.URI)
     */
    @Override
    public Branch getBranch(final URI uri) throws MalformedURIException {
        final String branchName = extractBranchName(uri);

        return branchFactory.getBranch(branchName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Repository#getBranches(org.eclipse.emf.common.util.URI)
     */
    @Override
    public Collection<Branch> getBranches(final URI uri) throws MalformedURIException {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Repository#getDependencies(org.eclipse.emf.common.util.URI)
     */
    @Override
    public Collection<URI> getDependencies(final URI uri) throws MalformedURIException {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Repository#getRevision(org.eclipse.emf.common.util.URI)
     */
    @Override
    public Revision getRevision(final URI uri) throws MalformedURIException {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Repository#rollbackTransaction(org.infai.amor.backend.Transaction)
     */
    @Override
    public void rollbackTransaction(final Transaction tr) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Repository#startTransaction()
     */
    @Override
    public Transaction startTransaction() {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Repository#view(org.eclipse.emf.common.util.URI)
     */
    @Override
    public Resource view(final URI uri) throws MalformedURIException {
        throw new UnsupportedOperationException();
    }

}
