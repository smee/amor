/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend;

import org.eclipse.emf.common.util.URI;
import org.infai.amor.backend.messages.Message;

/**
 * Common interface for backend responses returned by the methods of the backend main interface {@link Repository}.
 * 
 * @author sdienst
 * 
 */
public interface Response {

    /**
     * The message of the server.
     * 
     * @return
     */
    Message getMessage();

    /**
     * Location of a model. This reference is valid within a transaction started with {@link Repository#startTransaction()} and
     * stays valid only if {@link Repository#commitTransaction()} returns without an error.
     * 
     * @return reference to a persisted model
     */
    URI getURI();

}
