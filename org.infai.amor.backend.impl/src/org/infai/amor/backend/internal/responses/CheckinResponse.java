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
import org.infai.amor.backend.messages.Message;

/**
 * @author sdienst
 *
 */
public class CheckinResponse extends AbstractResponse {

    /**
     * @param msg
     * @param uri
     */
    public CheckinResponse(Message msg, URI uri) {
        super(msg, uri);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param msg
     * @param uri
     */
    public CheckinResponse(String msg, URI uri) {
        super(msg, uri);
        // TODO Auto-generated constructor stub
    }

}
