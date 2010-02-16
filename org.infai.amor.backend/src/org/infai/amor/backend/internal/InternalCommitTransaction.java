/*******************************************************************************
 * Copyright (c) 2010 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.internal;

import org.infai.amor.backend.CommitTransaction;

/**
 * @author sdienst
 *
 */
public interface InternalCommitTransaction extends CommitTransaction {
    void addStoredModel(final String relPath);

    boolean hasStoredModel(final String relPath);
}
