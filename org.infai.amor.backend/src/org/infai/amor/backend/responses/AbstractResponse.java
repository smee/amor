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

import org.eclipse.emf.common.util.URI;
import org.infai.amor.backend.Response;
import org.infai.amor.backend.messages.Message;

/**
 * @author sdienst
 * 
 */
public class AbstractResponse implements Response {

    private final Message msg;
    private final URI uri;

    public AbstractResponse(final Message msg, final URI uri) {
        this.msg = msg;
        this.uri = uri;
    }

    public AbstractResponse(final String msg, final URI uri) {
        this(new MessageImpl(msg), uri);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Response#getMessage()
     */
    @Override
    public Message getMessage() {
        return msg;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Response#getURI()
     */
    @Override
    public URI getURI() {
        return uri;
    }

}
