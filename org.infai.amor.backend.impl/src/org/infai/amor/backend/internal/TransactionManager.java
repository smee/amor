/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.internal;

import org.infai.amor.backend.Branch;
import org.infai.amor.backend.CommitTransaction;
import org.infai.amor.backend.Response;
import org.infai.amor.backend.exception.TransactionListener;

/**
 * @author sdienst
 * 
 */
public interface TransactionManager {

    /**
     * @param listener
     */
    void addTransactionListener(TransactionListener listener);

    /**
     * @param tr
     */
    Response commit(CommitTransaction tr);

    /**
     * @param listener
     */
    void removeTransactionListener(TransactionListener listener);

    /**
     * @param tr
     */
    void rollback(CommitTransaction tr);

    /**
     * @param branch
     *            the branch to commit to
     * @return
     */
    CommitTransaction startCommitTransaction(Branch branch);

}
