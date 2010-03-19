/*******************************************************************************
 * Copyright (c) 2010 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.api;

import java.io.IOException;

/**
 * Factory for remote instances of {@link SimpleRepository}.
 * 
 * @author sdienst
 * 
 */
public interface RemoteAmor {

    /**
     * Try to connect to a remote AMOR backend. This method either provides a proxy instance for local use or throws an exception
     * if the connection failed.
     * 
     * @param hostname
     * @param port
     * @return
     * @throws IOException
     */
    SimpleRepository getRepository(String hostname, int port) throws IOException;
}
