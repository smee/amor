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
import java.util.Map;

import org.eclipse.emf.common.util.URI;

/**
 * Describes metadata of one stored model in one revision.
 * 
 * @author sdienst
 * 
 */
public interface ModelLocation {
    static final String RELATIVE_PATH = "relativePath";
    /**
     * Uri that references a model version, can be used as parameter for the {@link org.infai.amor.backend.Repository} methods.
     */
    static final String EXTERNAL_URI = "externalUri";
    static final String NAMESPACE_URIS = "namespaceUris";

    /**
     * @return
     */
    ChangeType getChangeType();

    /**
     * @return external amor uri that references this model
     */
    URI getExternalUri();

    /**
     * Some undefined properties, use them for storage specific informations.
     * 
     * @return
     */
    Map<String,Object> getMetaData();

    /**
     * If this modellocation represents a metamodel this method will return all stored namespace uris of them.
     * 
     * @return
     */
    Collection<String> getNamespaceUris();

    /**
     * What is the relative path component of this model?
     * 
     * @return
     */
    String getRelativePath();

    /**
     * Does this model location reference a metamodel (ecore)?
     * 
     * @return
     */
    boolean isMetaModel();

}