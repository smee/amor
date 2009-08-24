/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.storage;

import org.infai.amor.backend.Branch;
import org.infai.amor.backend.exception.TransactionListener;

/**
 * @author sdienst
 * 
 */
public interface StorageFactory extends TransactionListener {

    /**
     * @param branch
     * @return
     */
    Storage getStorage(Branch branch);

}
