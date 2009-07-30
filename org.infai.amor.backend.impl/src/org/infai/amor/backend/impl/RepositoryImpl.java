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

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xml.type.internal.DataValue.URI.MalformedURIException;
import org.infai.amor.backend.Branch;
import org.infai.amor.backend.ChangedModel;
import org.infai.amor.backend.CommitTransaction;
import org.infai.amor.backend.Model;
import org.infai.amor.backend.Repository;
import org.infai.amor.backend.Response;
import org.infai.amor.backend.Revision;
import org.infai.amor.backend.internal.BranchFactory;
import org.infai.amor.backend.internal.StorageFactory;
import org.infai.amor.backend.internal.TransactionManager;
import org.infai.amor.backend.internal.UriHandler;
import org.infai.amor.backend.storage.Storage;

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
        return storageFactory.getStorage(tr.getBranch()).checkin(model, tr);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Repository#checkin(org.infai.amor.backend.Model, org.infai.amor.backend.Branch,
     * org.infai.amor.backend.Transaction)
     */
    @Override
    public Response checkin(final Model model, final CommitTransaction tr) {
        return storageFactory.getStorage(tr.getBranch()).checkin(model, tr);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Repository#checkout(org.eclipse.emf.common.util.URI)
     */
    @Override
    public Model checkout(final URI uri) throws MalformedURIException {
        final String branchname = uriHandler.extractBranchName(uri);
        final Branch branch = branchFactory.getBranch(branchname);
        final Storage storage = storageFactory.getStorage(branch);
        return storage.checkout(uri);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Repository#commitTransaction(org.infai.amor.backend.Transaction)
     */
    @Override
    public Response commitTransaction(final CommitTransaction tr) {
        return transactionManager.commit(tr);
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

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Repository#getBranch(org.eclipse.emf.common.util.URI)
     */
    @Override
    public Branch getBranch(final URI uri) throws MalformedURIException {
        final String branchName = uriHandler.extractBranchName(uri);

        return branchFactory.getBranch(branchName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Repository#getBranches(org.eclipse.emf.common.util.URI)
     */
    @Override
    public Iterable<Branch> getBranches(final URI uri) throws MalformedURIException {
        // FIXME uri implies multiple repositories, remove it?
        return branchFactory.getBranches();
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
        final Branch branch = branchFactory.getBranch(uriHandler.extractBranchName(uri));

        return branch.getRevision(uriHandler.extractRevision(uri));
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
    public CommitTransaction startTransaction(final Branch branch) {
        return transactionManager.startTransaction(branch);
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
