/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend;

import java.util.Collection;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xml.type.internal.DataValue.URI.MalformedURIException;

/**
 * Backend interface for usage by clients. Every invocation of {@link #checkin(Model)} and {@link #checkin(ChangedModel)} needs to
 * be in a running transaction. <br>
 * Usage of a repository:<br>
 * <ol>
 * <li>this.{@link #startTransaction()}</li>
 * <li>(this.{@link #checkin(Model)} or this.{@link #checkin(ChangedModel)})*</li>
 * <li>this.{@link #commitTransaction()} or this.{@link #rollbackTransaction()}</li>
 * </ol>
 * Any reading operation doesn't need a transaction. <br>
 * TODO document concurrent transaction behaviour!
 * 
 * @author sdienst
 * @author hkern
 */
public interface Repository {

    /**
     * Checkin a changed model.
     * 
     * @param model
     *            a changed model
     * @return
     */
    Response checkin(ChangedModel model, Transaction tr);

    /**
     * Add a {@link Model} that is new to the backend
     * 
     * @param model
     *            a model
     * @return information about success or error conditions
     */
    Response checkin(Model model, Transaction tr);

    /**
     * Restore a {@link Model} with the exact same contents given by the model referenced via the uri.
     * 
     * @param uri
     *            an amor uri specifying exactly one versioned model
     * @return a {@link Model}
     * @throws MalformedURIException
     *             for URIs that do not address a versioned model
     */
    Model checkout(URI uri) throws MalformedURIException;

    /**
     * Commit the current client transaction.
     * 
     * @return
     */
    Response commitTransaction(Transaction tr);

    /**
     * Get a branch. <code>uri</code> needs to be a valid suburi specifying a branch.
     * 
     * @param uri
     * @return
     * @throws MalformedURIException
     */
    Branch getBranch(URI uri) throws MalformedURIException;

    /**
     * Get all branches. <code>uri</code> needs to be a valid suburi specifying a repository.
     * 
     * @param uri
     * @return
     * @throws MalformedURIException
     */
    Collection<Branch> getBranches(URI uri) throws MalformedURIException;

    /**
     * This method returns {@link URI}s to all models the model addressed by <code>uri</code> references to.
     * 
     * @param uri
     *            a persisted model
     * @return all referenced models
     * @throws MalformedURIException
     */
    Collection<URI> getDependencies(URI uri) throws MalformedURIException;

    /**
     * Get a revision. <code>uri</code> needs to be a valid suburi specifying a revision.
     * 
     * @param uri
     * @return
     */
    Revision getRevision(URI uri) throws MalformedURIException;

    /**
     * Cancel all checked in models/changes.
     */
    void rollbackTransaction(Transaction tr);

    /**
     * Start a new transaction. Store every model commited during the transaction's lifetime. Persist these changes only upon
     * invocation of {@link #commitTransaction()}.
     */
    Transaction startTransaction();

    /**
     * Return a readonly resource that resolves dependencies to persisted models. This view has different uris than checked out
     * models.
     * 
     * @param uri
     *            uri of a model
     * @return a read only resource that resolves proxies within this repository
     */
    Resource view(URI uri) throws MalformedURIException;
}
