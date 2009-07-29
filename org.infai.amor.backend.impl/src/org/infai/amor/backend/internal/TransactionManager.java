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

import org.infai.amor.backend.Response;
import org.infai.amor.backend.Transaction;

/**
 * @author sdienst
 * 
 */
public interface TransactionManager {

    /**
     * @return
     */
    Transaction startTransaction();

    /**
     * @param tr
     */
    void rollback(Transaction tr);

    /**
     * @param tr
     */
    Response commit(Transaction tr);

}
