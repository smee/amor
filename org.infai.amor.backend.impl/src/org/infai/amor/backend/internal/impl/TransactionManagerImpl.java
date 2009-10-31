/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.internal.impl;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import javax.swing.event.EventListenerList;

import org.infai.amor.backend.Branch;
import org.infai.amor.backend.CommitTransaction;
import org.infai.amor.backend.Response;
import org.infai.amor.backend.Revision;
import org.infai.amor.backend.exception.TransactionException;
import org.infai.amor.backend.exception.TransactionListener;
import org.infai.amor.backend.impl.CommitTransactionImpl;
import org.infai.amor.backend.internal.NeoProvider;
import org.infai.amor.backend.internal.TransactionManager;
import org.infai.amor.backend.internal.UriHandler;
import org.infai.amor.backend.responses.CommitSuccessResponse;
import org.infai.amor.backend.responses.TransactionErrorResponse;
import org.neo4j.api.core.DynamicRelationshipType;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Transaction;

/**
 * @author sdienst
 * 
 */
public class TransactionManagerImpl extends NeoObjectFactory implements TransactionManager {
    /**
     * 
     */
    private static final String REVISIONCOUNTER_PROPERTY = "revisionCounter";
    private final EventListenerList listeners = new EventListenerList();
    private final UriHandler urihandler;

    /**
     * @param uh
     * @param np
     */
    public TransactionManagerImpl(final UriHandler uh, final NeoProvider np) {
        super(np);
        this.urihandler = uh;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.infai.amor.backend.internal.TransactionManager#addTransactionListener(org.infai.amor.backend.internal.TransactionListener
     * )
     */
    @Override
    public void addTransactionListener(final TransactionListener listener) {
        listeners.add(TransactionListener.class, listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.TransactionManager#commit(org.infai.amor.backend.CommitTransaction)
     */
    @Override
    public Response commit(final CommitTransaction tr, final Revision rev) {
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final PrintStream ps = new PrintStream(baos);
            boolean failure = false;
            // tell all listeners that we are going to commit
            // and log errors
            for (final TransactionListener listener : listeners.getListeners(TransactionListener.class)) {
                try {
                    listener.commit(tr, rev);
                } catch (final TransactionException e) {
                    failure = true;
                    e.printStackTrace(ps);
                    ps.append("\n");
                }
            }

            if (failure) {
                return new TransactionErrorResponse("Error on commit: " + baos.toString());
            } else {
                return new CommitSuccessResponse("Success", urihandler.createUriFor(tr));
            }
        } finally {
            if (tr instanceof CommitTransactionImpl) {
                // commit to neo4j
                final Transaction transaction = ((CommitTransactionImpl) tr).getNeoTransaction();
                transaction.success();
                transaction.finish();
            }
        }
    }

    /**
     * Create a new unique revision number.<br>
     * 
     * @return
     */
    private long createNextRevisionId() {
        final Transaction tx = getNeo().beginTx();
        try {
            final Node node = getFactoryNode(DynamicRelationshipType.withName("lastRevision"));
            if (!node.hasProperty(REVISIONCOUNTER_PROPERTY)) {
                node.setProperty(REVISIONCOUNTER_PROPERTY, 0L);
            }
            Long lastRevision = (Long) node.getProperty(REVISIONCOUNTER_PROPERTY);
            if (lastRevision == null) {
                lastRevision = 1L;
            } else {
                lastRevision += 1;
            }
            node.setProperty(REVISIONCOUNTER_PROPERTY, lastRevision);
            return lastRevision;
        }finally{
            tx.success();
            tx.finish();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.infai.amor.backend.internal.TransactionManager#removeTransactionListener(org.infai.amor.backend.internal.
     * TransactionListener)
     */
    @Override
    public void removeTransactionListener(final TransactionListener listener) {
        listeners.remove(TransactionListener.class, listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.TransactionManager#rollback(org.infai.amor.backend.CommitTransaction)
     */
    @Override
    public void rollback(final CommitTransaction tr) {
        try {
            for (final TransactionListener listener : listeners.getListeners(TransactionListener.class)) {
                listener.rollback(tr);
            }
        } finally {
            if (tr instanceof CommitTransactionImpl) {
                // rollback all neo4j database changes
                final Transaction transaction = ((CommitTransactionImpl) tr).getNeoTransaction();
                transaction.failure();
                transaction.finish();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.TransactionManager#startTransaction(org.infai.amor.backend.Branch)
     */
    @Override
    public CommitTransaction startCommitTransaction(final Branch branch) {
        long revisionId = createNextRevisionId();
        // create a new neo transaction, increment revisioncounter
        final Transaction tx = getNeo().beginTx();
        final CommitTransaction tr = new CommitTransactionImpl(branch, revisionId, tx);
        // inform listeners
        for (final TransactionListener listener : listeners.getListeners(TransactionListener.class)) {
            listener.startTransaction(tr);
        }
        return tr;
    }
}
