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

import org.infai.amor.backend.Branch;
import org.infai.amor.backend.CommitTransaction;
import org.neo4j.api.core.Transaction;

/**
 * @author sdienst
 * 
 */
public class CommitTransactionImpl implements CommitTransaction {
    private String commitMessage, username;
    private final long revId;
    private final Branch branch;
    private final Transaction neoTransaction;

    /**
     * @param revId
     * @param branch2
     */
    public CommitTransactionImpl(final Branch b, final long revId, final Transaction neoTransaction) {
        this.branch = b;
        this.revId = revId;
        this.neoTransaction = neoTransaction;
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
