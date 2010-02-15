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

import java.util.Set;

import org.infai.amor.backend.Branch;
import org.infai.amor.backend.internal.InternalCommitTransaction;
import org.neo4j.api.core.Transaction;

import com.google.common.collect.Sets;

/**
 * @author sdienst
 * 
 */
public class CommitTransactionImpl implements InternalCommitTransaction {
    private String commitMessage, username;
    private final long revId;
    private final Branch branch;
    private final Transaction neoTransaction;
    private final Set<String> storedModels;

    /**
     * @param revId
     * @param branch2
     */
    public CommitTransactionImpl(final Branch b, final long revId, final Transaction neoTransaction) {
        this.branch = b;
        this.revId = revId;
        this.neoTransaction = neoTransaction;
        this.storedModels = Sets.newHashSet();
    }

    /**
     * @param relPath
     */
    public void addStoredModel(final String relPath) {
        this.storedModels.add(relPath);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.CommitTransaction#getBranch()
     */
    @Override
    public Branch getBranch() {
        return branch;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.CommitTransaction#getCommitMessage()
     */
    @Override
    public String getCommitMessage() {
        return commitMessage;
    }

    /**
     * @return the neoTransaction
     */
    public Transaction getNeoTransaction() {
        return neoTransaction;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.CommitTransaction#getRevisionId()
     */
    @Override
    public long getRevisionId() {
        return revId;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.CommitTransaction#getUser()
     */
    @Override
    public String getUser() {
        return username;
    }


    /**
     * @param relPath
     * @return
     */
    public boolean hasStoredModel(final String relPath) {
        return storedModels.contains(relPath);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.CommitTransaction#setCommitMessage(java.lang.String)
     */
    @Override
    public void setCommitMessage(final String message) {
        this.commitMessage = message;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.CommitTransaction#setUser(java.lang.String)
     */
    @Override
    public void setUser(final String username) {
        this.username = username;
    }

}
