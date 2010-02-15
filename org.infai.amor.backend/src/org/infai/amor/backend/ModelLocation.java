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

import java.util.Map;

import org.eclipse.emf.common.util.URI;

/**
 * @author sdienst
 *
 */
public interface ModelLocation {
    static final String RELATIVE_PATH = "relativePath";
    static final String EXTERNAL_URI = "externalUri";

    /**
     * @return
     */
    Revision.ChangeType getChangeType();

    /**
     * Some undefined properties, use them for storage specific informations.
     * 
     * @return
     */
    Map<String,Object> getMetaData();

    /**
     * @return amor uri that references this model
     */
    URI getExternalUri();

    /**
     * What is the relative path component of this model?
     * 
     * @return
     */
    String getRelativePath();

}