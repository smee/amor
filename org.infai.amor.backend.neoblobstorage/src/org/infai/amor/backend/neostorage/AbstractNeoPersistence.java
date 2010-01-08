/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.neostorage;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.impl.DynamicEObjectImpl;
import org.infai.amor.backend.internal.NeoProvider;
import org.infai.amor.backend.internal.impl.NeoObjectFactory;
import org.neo4j.api.core.*;
import org.neo4j.api.core.Traverser.Order;

/**
 * 
 */
public abstract class AbstractNeoPersistence extends NeoObjectFactory implements Constants {

    /**
     * Map eobject -> neo4j node
     */
    Map<EObject, Node> nodeCache;
    Map<String, Node> classifierCache;

    /**
     * @param neo
     */
    public AbstractNeoPersistence(final NeoProvider neo) {
        super(neo);
        nodeCache = new HashMap<EObject, Node>();
        classifierCache = new HashMap<String, Node>();
    }

    /**
     * Create a node. Optionally the node gets a position ordering value.
     * 
     * @return Node
     */
    protected Node createNode() {
        return getNeo().createNode();
    }

    /**
     * Creates a node and binds it by a specified relationship to the given node.
     * 
     * @param node
     *            the node
     * @param relType
     *            the relationship type
     * @param ordered
     *            <code>true</code> if node will be created with ordering position value
     * @return the created and bound node
     */
    protected Node createNodeWithRelationship(final Node node, final EcoreRelationshipType relType, final boolean ordered) {
        final Node anotherNode = createNode();
        node.createRelationshipTo(anotherNode, relType);
        return anotherNode;
    }

    /**
     * Find the node corresponding to the persisted {@link EPackage} with the given namespace uri.
     * 
     * @param elementName
     * @return
     */
    protected Node determineEcoreClassifierNode(final String elementName) {
        if (!classifierCache.containsKey(elementName)) {
            // find model
            final Node ecoreMetamodel = getModelNode(EcorePackage.eNS_URI);
            if (null == ecoreMetamodel) {
                return null;
            }

            // traverse from model to all classifiers
            for (final Node node : ecoreMetamodel.traverse(Order.BREADTH_FIRST, StopEvaluator.END_OF_GRAPH, ReturnableEvaluator.ALL_BUT_START_NODE, EcoreRelationshipType.CONTAINS, Direction.OUTGOING)) {
                if (node.hasProperty(NAME)) {
                    final String name = (String) node.getProperty(NAME);
                    if (name.equals(elementName)) {
                        classifierCache.put(elementName, node);
                        break;
                    }
                }
            }
        }
        return classifierCache.get(elementName);
    }

    /**
     * Find the neo4j start node for a model.
     * 
     * @param nsUri
     *            the model's namespace uri
     * @return neo4j node for this model
     */
    protected Node getModelNode(final String nsURI) {
        final Iterable<Relationship> rels = getFactoryNode(EcoreRelationshipType.RESOURCES).getRelationships(Direction.OUTGOING);
        for (final Relationship rel : rels) {
            final Node model = rel.getEndNode();
            if (model.getProperty(NS_URI).equals(nsURI)) {
                return model;
            }
        }
        return null;
    }

    /**
     * Find neo4j node within the cache.
     * 
     * @param element
     *            emf element
     * @return corresponding ne4j node
     */
    protected Node getNodeFor(final EObject element) {
        final Node node = nodeCache.get(element);
        if (node == null && element instanceof DynamicEObjectImpl) {
            // TODO is there a catch here? does it work every time?
            final Node node2 = nodeCache.get(element.eClass());
            if (node2 != null) {
                return node2;
            }
            /*
             * DynamicEObjects might get created several times, sadly they do not overwrite hashCode() and equals(...) so we need
             * to do so manually :(
             */
            for (final EObject eo : nodeCache.keySet()) {
                if (eo instanceof DynamicEObjectImpl) {
                    // System.out.println(eo.eClass() + ", " + element.eClass().eResource().getURI());
                    if (element.eClass().getName().equals(eo.eClass().getName())) {
                        return nodeCache.get(eo);
                    }
                }
            }
        }
        return node;
    }

    /**
     * @return the registry
     */
    public Map<EObject, Node> getRegistry() {
        return nodeCache;
    }

    /**
     * Set a property iff value != null.
     * 
     * @param node
     * @param key
     * @param value
     */
    protected void set(final Node node, final String key, final Object value) {
        if (value != null) {
            node.setProperty(key, value);
        }
    }

    /**
     * Set a property iff value != null else use value=dflt.
     * 
     * @param node
     * @param key
     * @param value
     */
    protected void set(final Node node, final String key, final Object value, final Object dflt) {
        if (value != null) {
            node.setProperty(key, value);
        } else {
            node.setProperty(key, dflt);
        }
    }

    /**
     * Setter for cache map.
     * 
     * @param registry
     *            the registry to set
     */
    public void setRegistry(final Map<EObject, Node> registry) {
        this.nodeCache = registry;
    }

}
