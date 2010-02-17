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

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;

/**
 * @author sdienst
 * 
 */
public class MockedTransactionNeoWrapper implements GraphDatabaseService {

    private final GraphDatabaseService neoservice;

    /**
     * @return
     * @see org.neo4j.api.core.GraphDatabaseService#beginTx()
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
     * @see org.neo4j.api.core.GraphDatabaseService#createNode()
     */
    public Node createNode() {
        return neoservice.createNode();
    }

    /**
     * @return
     * @see org.neo4j.api.core.GraphDatabaseService#enableRemoteShell()
     */
    public boolean enableRemoteShell() {
        return neoservice.enableRemoteShell();
    }

    /**
     * @param arg0
     * @return
     * @see org.neo4j.api.core.GraphDatabaseService#enableRemoteShell(java.util.Map)
     */
    public boolean enableRemoteShell(final Map<String, Serializable> arg0) {
        return neoservice.enableRemoteShell(arg0);
    }

    /**
     * @return
     * @see org.neo4j.api.core.GraphDatabaseService#getAllNodes()
     */
    public Iterable<Node> getAllNodes() {
        return neoservice.getAllNodes();
    }

    /**
     * @param arg0
     * @return
     * @see org.neo4j.api.core.GraphDatabaseService#getNodeById(long)
     */
    public Node getNodeById(final long arg0) {
        return neoservice.getNodeById(arg0);
    }

    /**
     * @return
     * @see org.neo4j.api.core.GraphDatabaseService#getReferenceNode()
     */
    public Node getReferenceNode() {
        return neoservice.getReferenceNode();
    }

    /**
     * @param arg0
     * @return
     * @see org.neo4j.api.core.GraphDatabaseService#getRelationshipById(long)
     */
    public Relationship getRelationshipById(final long arg0) {
        return neoservice.getRelationshipById(arg0);
    }

    /**
     * @return
     * @see org.neo4j.api.core.GraphDatabaseService#getRelationshipTypes()
     */
    public Iterable<RelationshipType> getRelationshipTypes() {
        return neoservice.getRelationshipTypes();
    }

    /**
     * 
     * @see org.neo4j.api.core.GraphDatabaseService#shutdown()
     */
    public void shutdown() {
        neoservice.shutdown();
    }

    /**
     * @param neoservice
     */
    public MockedTransactionNeoWrapper(final GraphDatabaseService neoservice) {
        this.neoservice = neoservice;
    }

}
