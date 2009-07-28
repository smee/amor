/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.messages;

import java.util.Collection;

import org.eclipse.emf.common.util.URI;

/**
 * @author sdienst
 * 
 */
public interface CheckinMessage extends Message {
    /**
     * 
     */
    public enum ErrorCondition {
        /**
         * No error occured on checkin
         */
        OKAY,
        /**
         * The checked in model has references to other models, that aren't under version control yet. You should check them in
         * within the current transaction.
         */
        MISSING_DEPENDENCIES,
        /**
         * The model has missing elements, that are referenced by other models.
         */
        STALE_REFERENCES
    }

    /**
     * In case of <code>getErrorCondition!=ErrorCondition.OKAY</code> this method returns references to all model instances
     * affected by the current error.
     * 
     * @return
     */
    Collection<URI> getAffectedModels();

    /**
     * @return
     */
    ErrorCondition getErrorCondition();
}
