/*******************************************************************************
 * Copyright (c) 2010 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.impl;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.transaction.*;

import org.infai.amor.backend.api.SimpleRepository;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

/**
 * Neo4J associates running transactions with the current thread. We need to make sure this transaction gets disassociated from
 * the tread as well as reattached on the next call to any {@link SimpleRepository} method. This way we can offer this
 * implementation as a remote service not caring about the exact thread every call happens on.
 * 
 * @author sdienst
 * 
 */
public class NeoTransactionAwareSimpleRepository implements SimpleRepository {

    private final SimpleRepository wrappedInstance;
    private TransactionManager txmanager;
    final Map<Long, Transaction> txmap;

    public NeoTransactionAwareSimpleRepository(final SimpleRepository wrappedInstance, final TransactionManager txm) {
        this.wrappedInstance = wrappedInstance;
        this.txmanager = txm;
        txmap = Maps.newHashMap();
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.SimpleRepository#checkin(java.lang.String, java.lang.String, long)
     */
    @Override
    public List<String> checkin(final String ecoreXmi, final String relativePath, final long transactionId) {
        resume(transactionId);
        try {
            return wrappedInstance.checkin(ecoreXmi, relativePath, transactionId);
        } finally {
            suspend();
        }
    }
    /* (non-Javadoc)
     * @see org.infai.amor.backend.SimpleRepository#checkinPatch(java.lang.String, java.lang.String, long)
     */
    @Override
    public void checkinPatch(final String epatch, final String relativePath, final long transactionId) throws RuntimeException {
        resume(transactionId);
        try{
            wrappedInstance.checkinPatch(epatch, relativePath, transactionId);
        }finally{
            suspend();
        }
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.SimpleRepository#checkout(java.lang.String, long, java.lang.String)
     */
    @Override
    public String checkout(final String branch, final long revisionId, final String relativePath) throws IOException {
        return wrappedInstance.checkout(branch, revisionId, relativePath);
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.SimpleRepository#commitTransaction(long, java.lang.String, java.lang.String)
     */
    @Override
    public long commitTransaction(final long transactionId, final String username, final String commitMessage) throws Exception {
        resume(transactionId);
        return wrappedInstance.commitTransaction(transactionId, username, commitMessage);
    }
    /* (non-Javadoc)
     * @see org.infai.amor.backend.SimpleRepository#createBranch(java.lang.String, java.lang.String, long)
     */
    @Override
    public void createBranch(final String newbranchname, final String oldbranchname, final long startRevisionId) throws Exception {
        wrappedInstance.createBranch(newbranchname, oldbranchname, startRevisionId);
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.SimpleRepository#delete(long, java.lang.String)
     */
    @Override
    public void delete(final long transactionId, final String relativePath) {
        resume(transactionId);
        wrappedInstance.delete(transactionId, relativePath);
        suspend();
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.SimpleRepository#getActiveContents(java.lang.String)
     */
    @Override
    public List<String> getActiveContents(final String uri) {
        return wrappedInstance.getActiveContents(uri);
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.SimpleRepository#getBranches(java.lang.String)
     */
    @Override
    public String[] getBranches() {
        return wrappedInstance.getBranches();
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.SimpleRepository#getTouchedModelPaths(java.lang.String, long, int)
     */
    @Override
    public List<String> getTouchedModelPaths(final String branchname, final long revisionId, final int changeType) {
        return wrappedInstance.getTouchedModelPaths(branchname, revisionId, changeType);
    }

    /**
     * Reattach the neo4j transaction to the current thread
     */
    private Transaction resume(final long transactionId) {
        final Transaction tx = txmap.get(transactionId);
        Preconditions.checkNotNull(tx, "There is no transaction with id=" + transactionId);
        try {
            if (txmanager != null) {
                txmanager.resume(tx);
            }
            return tx;
        } catch (final InvalidTransactionException e) {
            throw new RuntimeException(e);
        } catch (final IllegalStateException e) {
            throw new RuntimeException("Internal error!", e);
        } catch (final SystemException e) {
            throw new RuntimeException("Internal error!", e);
        }
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.SimpleRepository#rollbackTransaction(long)
     */
    @Override
    public void rollbackTransaction(final long transactionId) throws Exception {
        resume(transactionId);
        wrappedInstance.rollbackTransaction(transactionId);

    }

    /**
     * @param tm
     */
    public void setTransactionManager(final TransactionManager tm){
        this.txmanager = tm;
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.SimpleRepository#startTransaction(java.lang.String)
     */
    @Override
    public long startTransaction(final String branchname) {
        final long transactionId = wrappedInstance.startTransaction(branchname);
        if (txmanager != null) {
            try {
                txmap.put(transactionId, txmanager.getTransaction());
            } catch (final SystemException e) {
                throw new RuntimeException(e);
            }
        }
        suspend();
        return transactionId;
    }

    /**
     * 
     */
    private void suspend() {
        if (txmanager != null) {
            try {
                txmanager.suspend();
            } catch (final SystemException e) {
                e.printStackTrace();
            }
        }
    }

}
