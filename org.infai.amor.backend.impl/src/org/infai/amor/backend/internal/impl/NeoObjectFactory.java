/*
 * NeoObjectFactory.java
 *
 * Copyright (c) 2007 Intershop Communications AG
 */
package org.infai.amor.backend.internal.impl;

import org.infai.amor.backend.internal.NeoProvider;
import org.neo4j.api.core.Direction;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;

/**
 * This class is the common superclass for all Neo factories.
 * 
 * @author Peter H&auml;nsgen
 */
public abstract class NeoObjectFactory {
    /**
     * The neo engine.
     */
    private final NeoProvider np;

    /**
     * The node representing the factory itself.
     */
    private Node factoryNode;

    /**
     * The constructor.
     */
    public NeoObjectFactory(final NeoProvider neo) {
        this.np = neo;
    }

    /**
     * @return
     */
    protected NeoService getNeo() {
        return np.getNeo();
    }

    /**
     * Determines the node that represents the factory itself. This node holds references to all existing instances.
     */
    protected Node getFactoryNode(final RelationshipType type) {
        if (factoryNode == null) {
            final Relationship rel = np.getNeo().getReferenceNode().getSingleRelationship(type, Direction.OUTGOING);

            if (rel == null) {
                // does not exist yet, create one
                factoryNode = np.getNeo().createNode();

                np.getNeo().getReferenceNode().createRelationshipTo(factoryNode, type);
            } else
                factoryNode = rel.getEndNode();
        }

        return factoryNode;
    }
}
