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
import org.infai.amor.backend.CommitTransaction;

/**
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

}
