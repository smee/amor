/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.responses;

import org.infai.amor.backend.messages.Message;

/**
 * @author sdienst
 * 
 */
public class MessageImpl implements Message {

    private final String message;

    /**
     * @param message
     */
    public MessageImpl(final String message) {
        this.message = message;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.messages.Message#getContent()
     */
    @Override
    public String getContent() {
        return message;
    }

}
