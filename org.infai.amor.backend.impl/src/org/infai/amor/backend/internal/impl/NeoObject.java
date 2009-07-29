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

import org.neo4j.api.core.Node;

/**
 * @author sdienst
 * 
 */
public class NeoObject {

    private final Node node;

    /**
     * @param node
     */
    public NeoObject(final Node node) {
        this.node = node;
    }

    /**
     * @return
     */
    public Node getNode() {
        return node;
    }
}
