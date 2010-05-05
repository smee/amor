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
    final int ADDED = 0;
    final int CHANGED = 1;
    final int DELETED = 2;

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
     *             placeholder, TODO
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
     */
    String checkout(String branch, long revisionId, String relativePath) throws IOException;

    /**
     * Commit a transaction.
     * 
     * @param transactionId
     * @return revision id
     * @throws TODO
     *             which exceptions?
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
     *             which exceptions exactly?
     */
    void createBranch(String newbranchname, String oldbranchname, long startRevisionId) throws Exception;

    /**
     * @param transactionId
     * @param relativePath
     */
    void delete(long transactionId, String relativePath);
    /**
     * @see Repository#getActiveContents(org.eclipse.emf.common.util.URI)
     * @param uri
     * @return
     */
    List<String> getActiveContents(String uri);

    /**
     * @see Repository#getBranches(org.eclipse.emf.common.util.URI)
     * @return
     */
    String[] getBranches();

    /**
     * Get relative paths of models that were touched in the given revision.
     * 
     * @param branchname
     * @param revisionId
     * @param changeType
     *            type of change, could be {@link #ADDED}, {@link #CHANGED} or {@link #DELETED}
     * @return list of relative paths
     */
    List<String> getTouchedModelPaths(String branchname,long revisionId,int changeType);

    /**
     * Abort checkin transactions, removes all changes applied since {@link #startTransaction(String)}.
     * 
     * @param transactionId
     * @throws Exception
     */
    void rollbackTransaction(long transactionId) throws Exception;

    /**
     * Start new write transaction.
     * 
     * @see Repository#startCommitTransaction(Branch)
     * @param branchname
     * @return
     */
    long startTransaction(String branchname);
}
