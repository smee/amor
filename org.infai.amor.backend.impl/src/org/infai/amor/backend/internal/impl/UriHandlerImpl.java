/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.internal.impl;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.xml.type.internal.DataValue.URI.MalformedURIException;
import org.infai.amor.backend.internal.UriHandler;

/**
 * URI format: amor:/host/repositoryname/branchname/directories.../modelname/revisionnumber
 * 
 * @author sdienst
 * 
 */
public class UriHandlerImpl implements UriHandler {

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.UriHandler#extractBranchName(org.eclipse.emf.common.util.URI)
     */
    @Override
    public String extractBranchName(final URI uri) throws MalformedURIException {
        if (uri.segmentCount() < 5) {
            throw new MalformedURIException("This uri doesn't contain a branch name: " + uri.toString());
        } else {
            return uri.segment(2);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.UriHandler#extractRevision(org.eclipse.emf.common.util.URI)
     */
    @Override
    public long extractRevision(final URI uri) throws MalformedURIException {
        if (uri.segmentCount() < 5) {
            throw new MalformedURIException("This uri doesn't contain a branch name: " + uri.toString());
        } else {
            return Long.parseLong(uri.lastSegment());
        }
    }

}
