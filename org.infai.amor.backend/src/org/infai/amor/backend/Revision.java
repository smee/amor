/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend;

import java.util.Collection;

import org.eclipse.emf.common.util.URI;

/**
 * @author sdienst
 * 
 */
public interface Revision {

    /**
     * Get the branch this revision belongs to.
     * 
     * @return
     */
    Branch getBranch();

    /**
     * Get a collection of uris for every touched model of this revision.
     * 
     * @return
     */
    Collection<URI> getModelReferences();

    /**
     * Get the unique revision id.
     * 
     * @return
     */
    long getRevisionId();
}
