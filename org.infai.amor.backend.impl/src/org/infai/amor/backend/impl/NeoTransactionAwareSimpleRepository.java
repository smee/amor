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
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.transaction.*;

import org.infai.amor.backend.api.RevisionInfo;
import org.infai.amor.backend.api.SimpleRepository;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Neo4J associates running transactions with the current thread. We need to make sure this transaction gets disassociated from
 * the tread as well as reattached on the next call to any {@link SimpleRepository} method. This way we can offer this
 * implementation as a remote service not caring about the exact thread every call happens on.
 * 
 * @author sdienst
 * 
 */
public class NeoTransactionAwareSimpleRepository implements SimpleRepository {
    private static final Logger logger = Logger.getLogger(NeoTransactionAwareSimpleRepository.class.getName());
    /**
     * Transactions time out after xxx msec.
     */
    private static final long TX_TIMEOUT = 20 * 1000;
    private static final long TIMER_INTERVALL = 5 * 1000;
    private final SimpleRepository wrappedInstance;
    private TransactionManager txmanager;
    public final Map<Long, Transaction> txmap;
    final Map<Long, Long> txtimers;
    final Timer cleanupTimer;

    public NeoTransactionAwareSimpleRepository(final SimpleRepository wrappedInstance, final TransactionManager txm) {
        this.wrappedInstance = wrappedInstance;
        this.txmanager = txm;
        txmap = Maps.newHashMap();
        txtimers = Maps.newHashMap();
        cleanupTimer = new Timer(true);
        // kill stalled transactions (no action for > 20sec.)
        cleanupTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                Set<Long> toRemove = Sets.newHashSet();

                for (Long txId : txtimers.keySet()) {
                    long stallTime = System.currentTimeMillis() - txtimers.get(txId);
                    if (stallTime > TX_TIMEOUT) {
                        Transaction stalledTransaction = txmap.remove(txId);
                        try {
                            toRemove.add(txId);
                            assert txmanager != null;
                            txmanager.resume(stalledTransaction);
                            txmanager.rollback();
                        } catch (IllegalStateException e) {
                            logger.log(Level.WARNING, "Error on rolling back stalled transaction!", e);
                        } catch (SystemException e) {
                            logger.log(Level.WARNING, "Error on rolling back stalled transaction!", e);
                        } catch (InvalidTransactionException e) {
                            logger.log(Level.WARNING, "Error on rolling back stalled transaction!", e);
                        }
                    }
                }
                // remove timestamps
                for (Long txId : toRemove) {
                    txtimers.remove(txId);
                }
            }
        }, 0, TIMER_INTERVALL);
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
            suspend(transactionId);
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
            suspend(transactionId);
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
        try {
            return wrappedInstance.commitTransaction(transactionId, username, commitMessage);
        } finally {
            txtimers.remove(transactionId);
        }
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
        suspend(transactionId);
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
     * @see org.infai.amor.backend.api.SimpleRepository#getRevisionInfo(java.lang.String, long)
     */
    @Override
    public RevisionInfo getRevisionInfo(String branchname, long revisionId) {
        return wrappedInstance.getRevisionInfo(branchname, revisionId);
    }

    /**
     * Reattach the neo4j transaction to the current thread
     */
    private Transaction resume(final long transactionId) {
        txtimers.remove(transactionId);
        final Transaction tx = txmap.get(transactionId);
        Preconditions.checkNotNull(tx, "There is no transaction with id=" + transactionId);
        try {
            if (txmanager != null) {
                txmanager.resume(tx);
            }
            return tx;
        } catch (final InvalidTransactionException e) {
            logger.log(Level.WARNING, "No such transaction!", e);
            throw new RuntimeException("No such transaction id!", e);
        } catch (final IllegalStateException e) {
            logger.log(Level.WARNING, "Invalid state for transaction!", e);
            throw new RuntimeException("Internal error!", e);
        } catch (final SystemException e) {
            logger.log(Level.WARNING, "Systemexception!?", e);
            throw new RuntimeException("Internal error!", e);
        }
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.SimpleRepository#rollbackTransaction(long)
     */
    @Override
    public void rollbackTransaction(final long transactionId) throws Exception {
        resume(transactionId);
        try {
            wrappedInstance.rollbackTransaction(transactionId);
        } finally {
            txtimers.remove(transactionId);
        }
    }

    /**
     * @param tm
     */
    public void setTransactionManager(final TransactionManager tm){
        this.txmanager = tm;
        logger.finer("Got a TransactionManager reference! " + tm);
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
        suspend(transactionId);
        return transactionId;
    }

    /**
     * 
     */
    private void suspend(long txId) {
        txtimers.put(txId, System.currentTimeMillis());
        if (txmanager != null) {
            try {
                txmanager.suspend();
            } catch (final SystemException e) {
                e.printStackTrace();
            }
        }
    }

}
