/*******************************************************************************
 * Copyright (c) 2010 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.api.impl;

import java.io.IOException;

import org.infai.amor.backend.api.RemoteAmor;
import org.infai.amor.backend.api.SimpleRepository;

import ch.ethz.iks.r_osgi.*;

/**
 * @author sdienst
 *
 */
public class RemoteAmorProvider implements RemoteAmor {

    private RemoteOSGiService remote;

    /**
     * Try to connect to another remote osgi container running r-osgi. Try to fetch a reference to a {@link SimpleRepository}
     * implementation.
     * 
     * @param remoteUri
     * @return true if connect and fetch went successfully
     * @throws IOException
     */
    private SimpleRepository fetchRepositoryProxy(final URI remoteUri) throws IOException {
        try {
            final RemoteServiceReference[] remoteServiceReferences = remote.getRemoteServiceReferences(remoteUri, SimpleRepository.class.getName(), null);
            if (remoteServiceReferences != null) {
                final RemoteServiceReference remoteReference = remoteServiceReferences[0];
                return (SimpleRepository) remote.getRemoteService(remoteReference);
            }
        } catch (final RemoteOSGiException e) {
            throw new IOException("Warning: Could not access remote AMOR repository!", e);
        }
        throw new IOException("Warning: Could not access remote AMOR repository!");
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.api.RemoteAmor#getRepository(java.lang.String, int)
     */
    @Override
    public SimpleRepository getRepository(final String hostname, final int port) throws IOException {
        return fetchRepositoryProxy(URI.create(hostname + ":" + port));
    }

    public void removeRemoteService(final RemoteOSGiService remote){
        if(this.remote!=remote) {
            System.err.println("Error: Asked to remove an unknown instance of RemoteOSGiService!");
        }
        this.remote =null;
    }

    /**
     * @param remote
     */
    public void setRemoteService(final RemoteOSGiService remote) {
        this.remote = remote;
    }

}
