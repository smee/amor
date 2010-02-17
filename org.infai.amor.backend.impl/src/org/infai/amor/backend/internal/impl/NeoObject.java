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
import org.neo4j.graphdb.*;

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
        this(np, np.getNeo().createNode());
    }

    /**
     * @param node
     */
    public NeoObject(final NeoProvider np, final Node node) {
        this.np = np;
        this.node = node;
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
     * @param key
     * @return
     */
    protected Object get(final String key){
        final Transaction tx = getNeoProvider().getNeo().beginTx();
        try{
            return getNode().getProperty(key);
        }finally{
            tx.success();
            tx.finish();
        }
    }

    /**
     * @return
     */
    protected NeoProvider getNeoProvider(){
        return np;
    }

    /**
     * @return
     */
    public Node getNode() {
        return node;
    }

    /**
     * Set a property iff value != null.
     * 
     * @param node
     * @param key
     * @param value
     */
    protected void set(final String key, final Object value) {
        if (value != null) {
            node.setProperty(key, value);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(this.toString());

        for (final String p : node.getPropertyKeys()) {
            sb.append(p + ": " + node.getProperty(p)).append("\n");
        }
        for (final Relationship rel : node.getRelationships(Direction.INCOMING)) {
            sb.append("IN " + rel.getType()).append("\n");
        }
        for (final Relationship rel : node.getRelationships(Direction.OUTGOING)) {
            sb.append("OUT " + rel.getType()).append("\n");
        }
        return sb.toString();
    }
}
