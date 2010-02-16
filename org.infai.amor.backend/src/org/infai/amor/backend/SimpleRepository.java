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
     * Checkin a new metamodel that is not known to this repository instance yet. Returns a list of namespaces of all packages
     * that the give ecore depends on and which are yet unknown to this repository. All of these dependecies need to be checked in prior to commiting, else
     * this model won't be persisted.
     * 
     * @param ecorexmi xmi representation of the ecore metamodell to checkin
     * @param relativePath
     * @return
     */
    List<String> checkinEcore(String ecoreXmi, String relativePath, long transactionId);

    /**
     * Like {@link #checkinEcore(String, String)} but for instance models. The returned list contains a mix of needed ecore metamodels as well as instance models
     * that get referenced by this model.
     * @param xmi
     * @param relativePath
     * @return
     */
    List<String> checkinXmi(String xmi, String relativePath, long transactionId);

    /**
     * @param branch
     * @param revisionId
     * @param relativePath
     * @return
     * @throws IOException
     */
    String checkoutEcore(String branch, long revisionId, String relativePath) throws IOException;

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
     * @param startRevisionIdw
     *            -1 or a valid revisionid that resembles the {@link Branch#getHeadRevision()} of the new branch
     * @throws TODO
     *             which exceptions exactly?
     */
    void createBranch(String newbranchname, String oldbranchname, long startRevisionId) throws Exception;

    /**
     * @see Repository#getBranches(org.eclipse.emf.common.util.URI)
     * @return
     */
    String[] getBranches(String uri);

    /**
     * @param transactionId
     * @throws Exception
     */
    void rollbackTransaction(long transactionId) throws Exception;

    /**
     * @see Repository#startCommitTransaction(Branch)
     * @param branchname
     * @return
     */
    long startTransaction(String branchname);
}
