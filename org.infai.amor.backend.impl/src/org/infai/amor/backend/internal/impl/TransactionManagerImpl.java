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

import org.infai.amor.backend.Branch;
import org.infai.amor.backend.CommitTransaction;
import org.infai.amor.backend.Response;
import org.infai.amor.backend.internal.TransactionManager;

/**
 * @author sdienst
 *
 */
public class TransactionManagerImpl implements TransactionManager {

    /* (non-Javadoc)
     * @see org.infai.amor.backend.internal.TransactionManager#commit(org.infai.amor.backend.CommitTransaction)
     */
    @Override
    public Response commit(CommitTransaction tr) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.internal.TransactionManager#rollback(org.infai.amor.backend.CommitTransaction)
     */
    @Override
    public void rollback(CommitTransaction tr) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.internal.TransactionManager#startTransaction(org.infai.amor.backend.Branch)
     */
    @Override
    public CommitTransaction startTransaction(Branch branch) {
        // TODO Auto-generated method stub
        return null;
    }

}
