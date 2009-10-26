/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.internal.responses;

import org.eclipse.emf.common.util.URI;
import org.infai.amor.backend.Response;
import org.infai.amor.backend.messages.Message;

/**
 * @author sdienst
 *
 */
public class DeleteSuccessResponse extends AbstractResponse implements Response {
    /**
     * @param msg
     * @param uri
     */
    public DeleteSuccessResponse(final Message msg, final URI uri) {
        super(msg, uri);
    }

    /**
     * @param msg
     * @param uri
     */
    public DeleteSuccessResponse(final String msg, final URI uri) {
        super(msg, uri);
    }

}
