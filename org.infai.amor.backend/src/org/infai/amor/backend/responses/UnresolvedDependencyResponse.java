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

import java.util.Collection;

import org.eclipse.emf.common.util.URI;

/**
 * @author sdienst
 *
 */
public class UnresolvedDependencyResponse extends AbstractResponse {

    private final Collection<URI> dependencies;

    /**
     * @param msg
     * @param uri
     * @param dependencies
     */
    public UnresolvedDependencyResponse(final String msg, final URI uri, final Collection<URI> dependencies) {
        super(msg, uri);
        this.dependencies = dependencies;
    }

    /**
     * Get uris for all model dependencies.
     * 
     * @return the dependencies
     */
    public Collection<URI> getDependencies() {
        return dependencies;
    }

}
