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

/**
 * A backend operation was invoked without running transaction. See {@link Repository}.
 * 
 * @author sdienst
 * 
 */
public class OutOfTransactionException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = -3398824477360185613L;

    /**
     * 
     */
    public OutOfTransactionException() {
    }

    /**
     * @param message
     */
    public OutOfTransactionException(final String message) {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public OutOfTransactionException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause
     */
    public OutOfTransactionException(final Throwable cause) {
        super(cause);
    }

}
