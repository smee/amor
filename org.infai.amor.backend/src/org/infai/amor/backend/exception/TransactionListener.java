/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.exception;

import java.util.EventListener;

import org.infai.amor.backend.CommitTransaction;

/**
 * @author sdienst
 * 
 */
public interface TransactionListener extends EventListener {

    /**
     * @param tr
     */
    void commit(CommitTransaction tr) throws TransactionException;

    /**
     * @param tr
     */
    void rollback(CommitTransaction tr);

    /**
     * @param tr
     */
    void startTransaction(CommitTransaction tr);
}
