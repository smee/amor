/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.internal.storage;

import java.util.Collections;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.infai.amor.backend.internal.ModelLocation;

/**
 * @author sdienst
 *
 */
public class FileModelLocation implements ModelLocation {

    private final String rp;
    private final URI exUri;
    private Map<String, Object> props;

    public FileModelLocation(final URI externalUri, final String relativePath) {
        this(externalUri,relativePath,Collections.EMPTY_MAP);
    }

    public FileModelLocation(final URI externalUri, final String relativePath, final Map<String, Object> props) {
        this.rp = relativePath;
        this.exUri = externalUri;
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.internal.ModelLocation#getCustomProperties()
     */
    @Override
    public Map<String, Object> getCustomProperties() {
        return props;
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.internal.ModelLocation#getExternalUri()
     */
    @Override
    public URI getExternalUri() {
        return exUri;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.ModelLocation#getRelativePath()
     */
    @Override
    public String getRelativePath() {
        return rp;
    }

}
