/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.api;

import java.io.IOException;
import java.util.List;


/**
 * Delegates to {@link Repository}. Uses only primitive parameters and return values. Use this interface for all remote accesses.
 * 
 * @author sdienst
 * 
 */
public interface SimpleRepository {

    /**
     * Checkin a new (meta-)model that is not known to this repository instance yet. <br/>
     * <br/>
     * If it's a metamodel: Returns a list of namespaces of all packages that the give ecore depends on and which are yet unknown
     * to this repository. All of these dependecies need to be checked in prior to commiting, else this model won't be persisted. <br/>
     * <br/>
     * If it's an instance model: Returns a mixed list: Namespaces of metamodels that this model depends on as well as relative
     * paths to other instance models that this model references.
     * 
     * @param ecorexmi
     *            xmi representation of the ecore metamodell to checkin
     * @param relativePath
     *            relative path of the model
     * @return
     */
    List<String> checkin(String ecoreXmi, String relativePath, long transactionId);

    /**
     * Checkin a serialized epatch that should be applied to the most recent model at the given relative path of the current
     * transaction's branch.
     * 
     * @param epatch
     *            serialized epatch
     * @param relativePath
     *            relative path of the model
     * @param transactionId
     *            current transaction
     * @throws RuntimeException
     *             placeholder, TODO define exception handling/return values for error states
     */
    void checkinPatch(String epatch, String relativePath, long transactionId) throws RuntimeException;

    /**
     * Checkout serialized model from branch/revision.
     * 
     * @param branch
     * @param revisionId
     * @param relativePath
     * @return serialized xmi representation of the model
     * @throws IOException
     *             TODO define exception handling/return values for error states
     */
    String checkout(String branch, long revisionId, String relativePath) throws IOException;

    /**
     * Commit a transaction.
     * 
     * @param transactionId
     * @return revision id
     * @throws TODO
     *             define exception handling/return values for error states
     */
    long commitTransaction(long transactionId, String username, String commitMessage) throws Exception;

    /**
     * Create a new branch with name <code>branchname</code>.
     * 
     * @param newbranchname
     *            name of the new branch
     * @param oldbranchname
     *            null or name of the parent branch
     * @param startRevisionId
     *            -1 or a valid revisionid that resembles the {@link Branch#getHeadRevision()} of the new branch
     * @throws TODO
     *             define exception handling/return values for error states
     */
    void createBranch(String newbranchname, String oldbranchname, long startRevisionId) throws Exception;

    /**
     * Delete a persisted model.
     * 
     * @param transactionId
     * @param relativePath
     *            relative path of the model
     */
    void delete(long transactionId, String relativePath);

    /**
     * @see Repository#getActiveContents(org.eclipse.emf.common.util.URI) URI format is
     *      amor://hostname/repo/branchname/revisionId/relative/path/modelname or a substring of it.
     * @param uri
     * @return
     */
    List<String> getActiveContents(String uri);

    /**
     * Get the names of all known branches of this repository.
     * 
     * @see Repository#getBranches(org.eclipse.emf.common.util.URI)
     * @return
     */
    String[] getBranches();

    /**
     * Retrieve some informations about a specific revision.
     * 
     * @param branchname
     * @param revisionId
     * @return
     */
    RevisionInfo getRevisionInfo(String branchname, long revisionId);

    /**
     * Abort checkin transactions, removes all changes applied since {@link #startTransaction(String)}.
     * 
     * @param transactionId
     * @throws Exception
     */
    void rollbackTransaction(long transactionId) throws Exception;

    /**
     * Start new write transaction. Returns a id that referes to this transaction. Use this for calls to
     * {@link #checkin(String, String, long)}, {@link #checkinPatch(String, String, long)},
     * {@link #commitTransaction(long, String, String)} and {@link #rollbackTransaction(long)}
     * 
     * @see Repository#startCommitTransaction(Branch)
     * @param branchname
     * @return transaction id
     */
    long startTransaction(String branchname);
}
