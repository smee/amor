/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.test;

import java.io.Serializable;
import java.util.Map;

import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.api.core.Transaction;

/**
 * @author sdienst
 * 
 */
public class MockedTransactionNeoWrapper implements NeoService {

    private final NeoService neoservice;

    /**
     * @return
     * @see org.neo4j.api.core.NeoService#beginTx()
     */
    public Transaction beginTx() {
        return new Transaction() {

            @Override
            public void success() {
            }

            @Override
            public void finish() {
            }

            @Override
            public void failure() {
            }
        };
    }

    /**
     * @return
     * @see org.neo4j.api.core.NeoService#createNode()
     */
    public Node createNode() {
        return neoservice.createNode();
    }

    /**
     * @return
     * @see org.neo4j.api.core.NeoService#enableRemoteShell()
     */
    public boolean enableRemoteShell() {
        return neoservice.enableRemoteShell();
    }

    /**
     * @param arg0
     * @return
     * @see org.neo4j.api.core.NeoService#enableRemoteShell(java.util.Map)
     */
    public boolean enableRemoteShell(final Map<String, Serializable> arg0) {
        return neoservice.enableRemoteShell(arg0);
    }

    /**
     * @return
     * @see org.neo4j.api.core.NeoService#getAllNodes()
     */
    public Iterable<Node> getAllNodes() {
        return neoservice.getAllNodes();
    }

    /**
     * @param arg0
     * @return
     * @see org.neo4j.api.core.NeoService#getNodeById(long)
     */
    public Node getNodeById(final long arg0) {
        return neoservice.getNodeById(arg0);
    }

    /**
     * @return
     * @see org.neo4j.api.core.NeoService#getReferenceNode()
     */
    public Node getReferenceNode() {
        return neoservice.getReferenceNode();
    }

    /**
     * @param arg0
     * @return
     * @see org.neo4j.api.core.NeoService#getRelationshipById(long)
     */
    public Relationship getRelationshipById(final long arg0) {
        return neoservice.getRelationshipById(arg0);
    }

    /**
     * @return
     * @see org.neo4j.api.core.NeoService#getRelationshipTypes()
     */
    public Iterable<RelationshipType> getRelationshipTypes() {
        return neoservice.getRelationshipTypes();
    }

    /**
     * 
     * @see org.neo4j.api.core.NeoService#shutdown()
     */
    public void shutdown() {
        neoservice.shutdown();
    }

    /**
     * @param neoservice
     */
    public MockedTransactionNeoWrapper(final NeoService neoservice) {
        this.neoservice = neoservice;
    }

}
