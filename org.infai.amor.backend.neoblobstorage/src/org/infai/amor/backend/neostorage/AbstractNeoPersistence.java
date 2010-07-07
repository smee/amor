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
import java.util.logging.Logger;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.impl.DynamicEObjectImpl;
import org.infai.amor.backend.internal.NeoProvider;
import org.infai.amor.backend.neo.NeoObjectFactory;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.Traverser.Order;

/**
 * 
 */
public abstract class AbstractNeoPersistence extends NeoObjectFactory implements Constants {

    private static Logger logger = Logger.getLogger(AbstractNeoPersistence.class.getName());
    /**
     * Map eobject -> neo4j node
     */
    private Map<EObject, Node> nodeCache;
    Map<String, Node> classifierCache;
    protected org.eclipse.emf.common.util.URI currentResourceUri = null;

    /**
     * Get {@link Boolean} property value from this node's properties.
     * 
     * @param node
     * @param key
     * @return
     */
    protected static Boolean getBool(final Node node, final String key){
        return (Boolean)node.getProperty(key);
    }

    /**
     * Get {@link Integer} property value from this node's properties.
     * 
     * @param node
     * @param key
     * @return
     */
    protected static Integer getInt(final Node node, final String key) {
        return (Integer) node.getProperty(key);
    }

    /**
     * Get {@link String} property value from this node's properties.
     * 
     * @param node
     * @param key
     * @return
     */
    protected static String getString(final Node node, final String key){
        if (!node.hasProperty(key)) {
            return null;
        }
        return (String)node.getProperty(key);
    }

    public static String nodeStats(final Node n){
        final StringBuilder sb = new StringBuilder(n.toString());

        for(final String p:n.getPropertyKeys()) {
            sb.append(p+": "+n.getProperty(p)).append("\n");
        }
        for (final Relationship rel : n.getRelationships(Direction.INCOMING)) {
            sb.append("IN " + rel.getType()).append("\n");
        }
        for (final Relationship rel : n.getRelationships(Direction.OUTGOING)) {
            sb.append("OUT " + rel.getType()).append("\n");
        }
        return sb.append("\n").toString();
    }

    /**
     * Set a property iff value != null.
     * 
     * @param node
     * @param key
     * @param value
     */
    protected static void set(final Node node, final String key, final Object value) {
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
    protected static void set(final Node node, final String key, final Object value, final Object dflt) {
        if (value != null) {
            node.setProperty(key, value);
        } else {
            node.setProperty(key, dflt);
        }
    }


    /**
     * @param neo
     */
    public AbstractNeoPersistence(final NeoProvider neo) {
        super(neo);
        nodeCache = new HashMap<EObject, Node>();
        classifierCache = new HashMap<String, Node>();
    }

    /**
     * Add mapping to the {@link EObject}-> {@link Node} cache.
     * 
     * @param eo
     * @param node
     */
    protected void cache(final EObject eo, final Node node) {
        nodeCache.put(eo, node);
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
     * @param aNode
     */
    protected void debug(final Node n) {
        System.out.println(AbstractNeoPersistence.nodeStats(n));

    }

    /**
     * Find the relative path to the file this proxy uri relates to. The path has the same root as {@link #currentResourceUri}.
     * @param proxy
     * @return
     */
    private String deresolve(final EObject proxy) {
        final URI pseudoAbsUri = org.eclipse.emf.common.util.URI.createURI("file://local/" + currentResourceUri);
        final URI proxyUri = ((InternalEObject) proxy).eProxyURI();

        final String deresolved = proxyUri.resolve(pseudoAbsUri).toString().substring("file://local/".length());

        return deresolved;
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
     * Finds the neo4j node for the given {@link EClassifier}.
     * 
     * @param element
     *            the {@link EClassifier}
     * @return the classifier node
     */
    protected Node findClassifierNode(final EClassifier element) {
        final Node node = getNodeFor(element);
        if (node != null) {
            return node;
        }

        final Node packageNode = findPackageNode(element.getEPackage());
        if (null == packageNode) {
            throw new IllegalStateException("The package with namespace uri [" + element.getEPackage().getNsURI() + "] could not be found!");
        }
        // traverse contents of this package
        for (final Node aNode : packageNode.traverse(Order.BREADTH_FIRST, StopEvaluator.DEPTH_ONE, ReturnableEvaluator.ALL_BUT_START_NODE, EcoreRelationshipType.CONTAINS, Direction.OUTGOING)) {
            if (aNode.hasProperty(NAME)) {
                final String classifierName = (String) aNode.getProperty(NAME);
                if (classifierName.equals(element.getName())) {
                    // // meta relationship?
                    final Relationship metaRel = aNode.getSingleRelationship(EcoreRelationshipType.INSTANCE, Direction.INCOMING);
                    if (metaRel == null) {
                        // only in ecore there are, sometimes, no meta relationships
                        cache(element, aNode);
                        return aNode;
                    }
                    final Node metaNode = metaRel.getStartNode();
                    final Object metaNodeName = metaNode.getProperty(NAME);
                    if (EcorePackage.Literals.ECLASS.getName().equals(metaNodeName) || EcorePackage.Literals.EDATA_TYPE.getName().equals(metaNodeName) || EcorePackage.Literals.EENUM.getName().equals(metaNodeName)) {
                        // cache the element node
                        cache(element, aNode);
                        return aNode;
                    }
                }
            }
        }

        return null;
    }

    /**
     * @param relationships
     * @param nsUri
     * @param nsURI2
     * @return
     */
    private Node findNode(Iterable<Relationship> relationships, String nodeProperty, String value) {
        for(Relationship rel: relationships){
            Node node = rel.getEndNode();
            String propertyValue = getString(node,nodeProperty);
            if(propertyValue.equals(value)) {
                return node;
            }
        }
        return null;
    }

    /**
     * Searches the neo4j node corresponding to the given {@link EPackage}.
     * <p>
     * 
     * @param aPackage
     *            {@link EPackage} to search
     * @return the package node
     */
    private Node findPackageNode(final EPackage aPackage) {
        final Node ePackageNode = determineEcoreClassifierNode(EcorePackage.Literals.EPACKAGE.getName());

        for (final Node pkgNode : ePackageNode.traverse(Order.BREADTH_FIRST, StopEvaluator.DEPTH_ONE, ReturnableEvaluator.ALL_BUT_START_NODE, EcoreRelationshipType.INSTANCE, Direction.OUTGOING)) {
            if (aPackage.getNsURI().equals(pkgNode.getProperty(NS_URI))) {
                return pkgNode;
            }
        }
        return null;
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
            String noteNsUri = getString(model,NS_URI);
            if (noteNsUri.equals(nsURI)) {
                return model;
            }else if(noteNsUri.length()>0 && nsURI.startsWith(noteNsUri)){
                // subpackage
                return findNode(model.getRelationships(EcoreRelationshipType.CONTAINS, Direction.OUTGOING),NS_URI,nsURI);
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
        // System.out.println(element.hashCode() + ": " + EcoreUtil.getURI(element));
        final Node node = nodeCache.get(element);
        if (node != null){
            return node;
        }else
            if (element.eIsProxy()) {
                // TODO find node by traversing known models
                // create a proxy node
                final Node proxyNode = createNode();
                set(proxyNode, NAME, "ProxyNode");
                // make proxy uri relative to current resource's uri
                final String relativeProxyUri = deresolve(element);
                // final String relativeProxyUri = ((InternalEObject) element).eProxyURI().toString();
                set(proxyNode, "proxyUri", relativeProxyUri);
                final Node classNode = findClassifierNode(element.eClass());
                classNode.createRelationshipTo(proxyNode, EcoreRelationshipType.INSTANCE);

                logger.finest("storing proxy to " + relativeProxyUri);

                cache(element, proxyNode);
                return proxyNode;
            }else  if (element instanceof DynamicEObjectImpl) {
                // DynamicEObjects might get created several times, sadly they do not overwrite hashCode() and equals(...) so we need
                // to
                // do so manually :(
                return getNodeFor(element.eClass());
            } else {
                // throw new IllegalStateException("Could not find neo4j node for " + element);
                return null;
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
