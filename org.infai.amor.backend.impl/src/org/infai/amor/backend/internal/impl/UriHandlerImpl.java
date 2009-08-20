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

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.xml.type.internal.DataValue.URI.MalformedURIException;
import org.infai.amor.backend.CommitTransaction;
import org.infai.amor.backend.internal.UriHandler;

/**
 * URI format: amor:/host/repositoryname/branchname/revisionnumber/directories.../modelname
 * 
 * @author sdienst
 * 
 */
public class UriHandlerImpl implements UriHandler {

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.UriHandler#createModelUri(org.infai.amor.backend.CommitTransaction, java.lang.String)
     */
    @Override
    public URI createModelUri(final CommitTransaction tr, final IPath modelPath) {
        final URI uri = createUriFor(tr);

        if (modelPath != null && !modelPath.isAbsolute()) {
            return uri.appendSegments(modelPath.segments());
        } else {
            throw new IllegalArgumentException("The given path must be relative for storing a model, was absolute: " + modelPath);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.UriHandler#createUriFor(org.infai.amor.backend.CommitTransaction)
     */
    @Override
    public URI createUriFor(final CommitTransaction tr) {
        // TODO configurable host and repository!
        return URI.createHierarchicalURI("amor", "localhost", null, new String[] { "repo", tr.getBranch().getName(), Long.toString(tr.getRevisionId()) }, null, null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.UriHandler#extractBranchName(org.eclipse.emf.common.util.URI)
     */
    @Override
    public String extractBranchName(final URI uri) throws MalformedURIException {
        if (uri.segmentCount() < 4) {
            throw new MalformedURIException("This uri doesn't contain a branch name: " + uri.toString());
        } else {
            return uri.segment(1);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.UriHandler#extractModelPathFrom(org.eclipse.emf.common.util.URI)
     */
    @Override
    public IPath extractModelPathFrom(final URI uri) throws MalformedURIException {
        if (uri.segmentCount() < 4) {
            throw new MalformedURIException("This uri doesn't contain any model paths or name: " + uri.toString());
        } else {
            final String[] modelPath = new String[uri.segmentCount() - 4];
            // copy all segments after the revision number
            System.arraycopy(uri.segments(), 4, modelPath, 0, uri.segmentCount() - 4);
            return new Path(StringUtils.join(modelPath, '/'));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.UriHandler#extractRevision(org.eclipse.emf.common.util.URI)
     */
    @Override
    public long extractRevision(final URI uri) throws MalformedURIException {
        if (uri.segmentCount() < 4) {
            throw new MalformedURIException("This uri doesn't contain a revision number: " + uri.toString());
        } else {
            return Long.parseLong(uri.segment(2));
        }
    }

}
