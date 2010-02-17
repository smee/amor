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
import org.infai.amor.backend.Revision;
import org.infai.amor.backend.internal.InternalCommitTransaction;
import org.infai.amor.backend.internal.InternalRevision;
import org.neo4j.graphdb.Transaction;

/**
 * @author sdienst
 * 
 */
public class CommitTransactionImpl implements InternalCommitTransaction {
    private final InternalRevision rev;
    private final Branch branch;
    private final Transaction neoTransaction;

    /**
     * @param revId
     * @param branch2
     */
    public CommitTransactionImpl(final Branch b, final InternalRevision rev, final Transaction neoTransaction) {
        this.branch = b;
        this.rev = rev;
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
        return rev.getCommitMessage();
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
    public Revision getRevision() {
        return rev;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.CommitTransaction#getUser()
     */
    @Override
    public String getUser() {
        return rev.getUser();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.CommitTransaction#setCommitMessage(java.lang.String)
     */
    @Override
    public void setCommitMessage(final String message) {
        rev.setCommitMessage(message);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.CommitTransaction#setUser(java.lang.String)
     */
    @Override
    public void setUser(final String username) {
        rev.setUser(username);
    }

}
