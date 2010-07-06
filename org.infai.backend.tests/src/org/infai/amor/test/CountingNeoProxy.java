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

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.Traverser.Order;

/**
 * @author sdienst
 * 
 */
public class CountingNeoProxy implements GraphDatabaseService {

    private class CountingNode implements Node {
        public final Node node;

        CountingNode(final Node node) {
            this.node = node;
        }

        /**
         * @param arg0
         * @param arg1
         * @return
         * @see org.neo4j.api.core.Node#createRelationshipTo(org.neo4j.api.core.Node, org.neo4j.api.core.RelationshipType)
         */
        public Relationship createRelationshipTo(final Node arg0, final RelationshipType arg1) {
            countRelationShip();
            return node.createRelationshipTo(arg0, arg1);
        };
        /**
         * 
         * @see org.neo4j.api.core.Node#delete()
         */
        public void delete() {
            node.delete();
        }

        /**
         * @return
         * @see org.neo4j.api.core.Node#getId()
         */
        public long getId() {
            return node.getId();
        }

        /**
         * @param arg0
         * @return
         * @see org.neo4j.api.core.PropertyContainer#getProperty(java.lang.String)
         */
        public Object getProperty(final String arg0) {
            return node.getProperty(arg0);
        }

        /**
         * @param arg0
         * @param arg1
         * @return
         * @see org.neo4j.api.core.PropertyContainer#getProperty(java.lang.String, java.lang.Object)
         */
        public Object getProperty(final String arg0, final Object arg1) {
            return node.getProperty(arg0, arg1);
        }

        /**
         * @return
         * @see org.neo4j.api.core.PropertyContainer#getPropertyKeys()
         */
        public Iterable<String> getPropertyKeys() {
            return node.getPropertyKeys();
        }

        /**
         * @return
         * @deprecated
         * @see org.neo4j.api.core.PropertyContainer#getPropertyValues()
         */
        @Deprecated
        public Iterable<Object> getPropertyValues() {
            return node.getPropertyValues();
        }

        /**
         * @return
         * @see org.neo4j.api.core.Node#getRelationships()
         */
        public Iterable<Relationship> getRelationships() {
            return node.getRelationships();
        }

        /**
         * @param arg0
         * @return
         * @see org.neo4j.api.core.Node#getRelationships(org.neo4j.api.core.Direction)
         */
        public Iterable<Relationship> getRelationships(final Direction arg0) {
            return node.getRelationships(arg0);
        }

        /**
         * @param arg0
         * @return
         * @see org.neo4j.api.core.Node#getRelationships(org.neo4j.api.core.RelationshipType[])
         */
        public Iterable<Relationship> getRelationships(final RelationshipType... arg0) {
            return node.getRelationships(arg0);
        }

        /**
         * @param arg0
         * @param arg1
         * @return
         * @see org.neo4j.api.core.Node#getRelationships(org.neo4j.api.core.RelationshipType, org.neo4j.api.core.Direction)
         */
        public Iterable<Relationship> getRelationships(final RelationshipType arg0, final Direction arg1) {
            return node.getRelationships(arg0, arg1);
        }

        /**
         * @param arg0
         * @param arg1
         * @return
         * @see org.neo4j.api.core.Node#getSingleRelationship(org.neo4j.api.core.RelationshipType, org.neo4j.api.core.Direction)
         */
        public Relationship getSingleRelationship(final RelationshipType arg0, final Direction arg1) {
            return node.getSingleRelationship(arg0, arg1);
        }

        /**
         * @param arg0
         * @return
         * @see org.neo4j.api.core.PropertyContainer#hasProperty(java.lang.String)
         */
        public boolean hasProperty(final String arg0) {
            return node.hasProperty(arg0);
        }

        /**
         * @return
         * @see org.neo4j.api.core.Node#hasRelationship()
         */
        public boolean hasRelationship() {
            return node.hasRelationship();
        }

        /**
         * @param arg0
         * @return
         * @see org.neo4j.api.core.Node#hasRelationship(org.neo4j.api.core.Direction)
         */
        public boolean hasRelationship(final Direction arg0) {
            return node.hasRelationship(arg0);
        }

        /**
         * @param arg0
         * @return
         * @see org.neo4j.api.core.Node#hasRelationship(org.neo4j.api.core.RelationshipType[])
         */
        public boolean hasRelationship(final RelationshipType... arg0) {
            return node.hasRelationship(arg0);
        }

        /**
         * @param arg0
         * @param arg1
         * @return
         * @see org.neo4j.api.core.Node#hasRelationship(org.neo4j.api.core.RelationshipType, org.neo4j.api.core.Direction)
         */
        public boolean hasRelationship(final RelationshipType arg0, final Direction arg1) {
            return node.hasRelationship(arg0, arg1);
        }

        /**
         * @param arg0
         * @return
         * @see org.neo4j.api.core.PropertyContainer#removeProperty(java.lang.String)
         */
        public Object removeProperty(final String arg0) {
            return node.removeProperty(arg0);
        }

        /**
         * @param arg0
         * @param arg1
         * @see org.neo4j.api.core.PropertyContainer#setProperty(java.lang.String, java.lang.Object)
         */
        public void setProperty(final String arg0, final Object arg1) {
            countProperty();
            node.setProperty(arg0, arg1);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            for (final String key : getPropertyKeys()) {
                sb.append(key).append(": ").append(getProperty(key)).append("\n");
            }
            for (final Relationship rel : getRelationships(Direction.INCOMING)) {
                sb.append("IN: " + rel.getType()).append("\n");
            }
            for (final Relationship rel : getRelationships(Direction.OUTGOING)) {
                sb.append("OUT: " + rel.getType()).append("\n");
            }
            return sb.toString();
        }

        /**
         * @param arg0
         * @param arg1
         * @param arg2
         * @param arg3
         * @return
         * @see org.neo4j.api.core.Node#traverse(org.neo4j.api.core.Traverser.Order, org.neo4j.api.core.StopEvaluator,
         *      org.neo4j.api.core.ReturnableEvaluator, java.lang.Object[])
         */
        public Traverser traverse(final Order arg0, final StopEvaluator arg1, final ReturnableEvaluator arg2, final Object... arg3) {
            return node.traverse(arg0, arg1, arg2, arg3);
        }

        /**
         * @param arg0
         * @param arg1
         * @param arg2
         * @param arg3
         * @param arg4
         * @return
         * @see org.neo4j.api.core.Node#traverse(org.neo4j.api.core.Traverser.Order, org.neo4j.api.core.StopEvaluator,
         *      org.neo4j.api.core.ReturnableEvaluator, org.neo4j.api.core.RelationshipType, org.neo4j.api.core.Direction)
         */
        public Traverser traverse(final Order arg0, final StopEvaluator arg1, final ReturnableEvaluator arg2, final RelationshipType arg3, final Direction arg4) {
            return node.traverse(arg0, arg1, arg2, arg3, arg4);
        }

        /**
         * @param arg0
         * @param arg1
         * @param arg2
         * @param arg3
         * @param arg4
         * @param arg5
         * @param arg6
         * @return
         * @see org.neo4j.api.core.Node#traverse(org.neo4j.api.core.Traverser.Order, org.neo4j.api.core.StopEvaluator,
         *      org.neo4j.api.core.ReturnableEvaluator, org.neo4j.api.core.RelationshipType, org.neo4j.api.core.Direction,
         *      org.neo4j.api.core.RelationshipType, org.neo4j.api.core.Direction)
         */
        public Traverser traverse(final Order arg0, final StopEvaluator arg1, final ReturnableEvaluator arg2, final RelationshipType arg3, final Direction arg4, final RelationshipType arg5, final Direction arg6) {
            return node.traverse(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        }

    }

    private final GraphDatabaseService service;
    private long numRels;
    private long numNodes;
    private long numProps;

    public CountingNeoProxy(final GraphDatabaseService ns) {
        this.service = ns;
    }

    /**
     * @return
     * @see org.neo4j.api.core.GraphDatabaseService#beginTx()
     */
    public Transaction beginTx() {
        return service.beginTx();
    }

    /**
     * 
     */
    public void countProperty() {
        numProps++;
    }

    /**
     * 
     */
    public void countRelationShip() {
        numRels++;
    }

    /**
     * @return
     * @see org.neo4j.api.core.GraphDatabaseService#createNode()
     */
    public Node createNode() {
        numNodes++;
        return new CountingNode(service.createNode());
    }

    /**
     * @return
     * @see org.neo4j.api.core.GraphDatabaseService#enableRemoteShell()
     */
    public boolean enableRemoteShell() {
        return service.enableRemoteShell();
    }

    /**
     * @param arg0
     * @return
     * @see org.neo4j.api.core.GraphDatabaseService#enableRemoteShell(java.util.Map)
     */
    public boolean enableRemoteShell(final Map<String, Serializable> arg0) {
        return service.enableRemoteShell(arg0);
    }

    /**
     * @return
     * @see org.neo4j.api.core.GraphDatabaseService#getAllNodes()
     */
    public Iterable<Node> getAllNodes() {
        return service.getAllNodes();
    }

    /**
     * @param arg0
     * @return
     * @see org.neo4j.api.core.GraphDatabaseService#getNodeById(long)
     */
    public Node getNodeById(final long arg0) {
        return service.getNodeById(arg0);
    }

    /**
     * @return the numNodes
     */
    public long getNumNodes() {
        return numNodes;
    }

    /**
     * @return the numProps
     */
    public long getNumProps() {
        return numProps;
    }

    /**
     * @return the numRels
     */
    public long getNumRels() {
        return numRels;
    }

    /**
     * @return
     * @see org.neo4j.api.core.GraphDatabaseService#getReferenceNode()
     */
    public Node getReferenceNode() {
        return service.getReferenceNode();
    }

    /**
     * @param arg0
     * @return
     * @see org.neo4j.api.core.GraphDatabaseService#getRelationshipById(long)
     */
    public Relationship getRelationshipById(final long arg0) {
        return service.getRelationshipById(arg0);
    }

    /**
     * @return
     * @see org.neo4j.api.core.GraphDatabaseService#getRelationshipTypes()
     */
    public Iterable<RelationshipType> getRelationshipTypes() {
        return service.getRelationshipTypes();
    }

    /**
     * 
     * @see org.neo4j.api.core.GraphDatabaseService#shutdown()
     */
    public void shutdown() {
        service.shutdown();
    }
}
