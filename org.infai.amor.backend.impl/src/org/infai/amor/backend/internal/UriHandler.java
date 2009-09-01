/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.internal;

import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.xml.type.internal.DataValue.URI.MalformedURIException;
import org.infai.amor.backend.Branch;
import org.infai.amor.backend.CommitTransaction;

/**
 * Internal interface that knows about the structure of externally referenceable uris to contents of the amor repository.
 * 
 * @author sdienst
 * 
 */
public interface UriHandler {

    /**
     * Create an externally usable uri that links to this exact model version.
     * 
     * @param modelSpecificPart
     *            relative storage specific path
     * @param tr
     *            current commit transaction
     * @return
     */
    URI createModelUri(CommitTransaction tr, IPath persistencePath);

    /**
     * @param r
     * @return
     */
    URI createUriFor(Branch branch);

    /**
     * @param branch
     * @param revisionId
     * @return
     */
    URI createUriFor(Branch branch, long revisionId);

    /**
     * @param branch
     * @param revisionId
     * @param path
     * @return
     */
    URI createUriFor(Branch branch, long revisionId, IPath path);

    /**
     * Create an externally usable uri that links to this revision.
     * 
     * @param tr
     * @return
     */
    URI createUriFor(CommitTransaction tr);

    /**
     * @param uri
     * @return
     */
    String extractBranchName(URI uri) throws MalformedURIException;

    /**
     * @param uri
     * @return
     * @throws MalformedURIException
     */
    IPath extractModelPathFrom(URI uri) throws MalformedURIException;

    /**
     * @param uri
     * @return
     */
    long extractRevision(URI uri) throws MalformedURIException;

    /**
     * @param prefixUri
     * @param fullUri
     * @return
     */
    boolean isPrefix(final URI prefixUri, final URI fullUri);

    /**
     * @param prefixUri
     * @param fullUri
     * @return
     */
    boolean isPrefixIgnoringRevision(final URI prefixUri, final URI fullUri);

    /**
     * Works like the 'ls' command, returns the part of fullUri, that resembles prefix+one more segment.
     * <p>
     * Example: <code>trimToNextSegment("a/b/","a/b/c/d/e") => "a/b/c"
     * 
     * @param prefix
     * @param fullUri
     * @return
     */
    URI trimToNextSegment(URI prefix, URI fullUri);

    /**
     * Same as {@link #trimToNextSegment(URI, URI)} but keeps the revision number of prefix. If there is no revision, an exception
     * gets thrown.
     * 
     * @param segmentIdx
     * @param fullUri
     * @return
     */
    URI trimToNextSegmentKeepingRevision(int segmentIdx, URI fullUri);

}
