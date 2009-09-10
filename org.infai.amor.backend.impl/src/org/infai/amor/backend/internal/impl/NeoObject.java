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

import org.infai.amor.backend.internal.NeoProvider;
import org.neo4j.api.core.Node;

/**
 * @author sdienst
 * 
 */
public class NeoObject {

    private final Node node;
    private final NeoProvider np;

    /**
     * @param node
     */
    public NeoObject(final NeoProvider np) {
        this.np = np;
        this.node = np.getNeo().createNode();
    }

    /**
     * @param node
     */
    public NeoObject(final Node node) {
        this.node = node;
        this.np = null;
    }

    /**
     * Create a new node.
     * 
     * @return
     */
    protected Node createNode() {
        return np.getNeo().createNode();
    }

    /**
     * @return
     */
    public Node getNode() {
        return node;
    }
}
