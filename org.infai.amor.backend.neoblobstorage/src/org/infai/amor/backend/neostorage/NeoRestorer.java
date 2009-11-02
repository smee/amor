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

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EGenericType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.ETypeParameter;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.infai.amor.backend.internal.NeoProvider;
import org.neo4j.api.core.Direction;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.api.core.ReturnableEvaluator;
import org.neo4j.api.core.StopEvaluator;
import org.neo4j.api.core.Traverser.Order;

public class NeoRestorer extends AbstractNeoPersistence {
    static class OrderedNodeIterable implements Iterable<Node> {
        private final Node node;
        private final RelationshipType relType;
        private final Direction direction;

        public OrderedNodeIterable(final Node node, final RelationshipType relType, final Direction direction) {
            this.node = node;
            this.relType = relType;
            this.direction = direction;
        }

        @Override
        public Iterator<Node> iterator() {
            final Set<Node> nodes = new TreeSet<Node>(NODE_POSITION_COMPARATOR);
            nodes.addAll(node.traverse(Order.BREADTH_FIRST, StopEvaluator.DEPTH_ONE, ReturnableEvaluator.ALL_BUT_START_NODE, relType, direction).getAllNodes());
            return nodes.iterator();
        }
    }

    private static Logger logger = Logger.getLogger(NeoRestorer.class.getName());

    /**
     * Order nodes by node id, returns them in the order of insertion.
     */
    private static final Comparator<Node> NODE_POSITION_COMPARATOR = new Comparator<Node>() {

        @Override
        public int compare(final Node first, final Node second) {
            final long firstId = first.getId(), secondId = second.getId();

            if (firstId < secondId) {
                return -1;
            } else if (firstId > secondId) {
                return 1;
            }
            return 0;
        }
    };

    private Map<Node, EObject> cache;

    /**
     * @param neo
     */
    public NeoRestorer(final NeoProvider neo) {
        super(neo);
    }

    /**
     * Find the topmost package node for the package specified by <code>nsUri</code>.
     * 
     * @param nsUri
     * @return
     */
    private EPackage findTopLevelPackage(final String nsUri) {
        final Node ePackageNode = determineEcoreClassifierNode(EcorePackage.Literals.EPACKAGE.getName());
        for (final Node pkgNode : ePackageNode.traverse(Order.BREADTH_FIRST, StopEvaluator.DEPTH_ONE, ReturnableEvaluator.ALL_BUT_START_NODE, EcoreRelationshipType.INSTANCE, Direction.OUTGOING)) {
            if (nsUri.equals(pkgNode.getProperty(NS_URI))) {
                Node container = pkgNode;
                while (container.hasRelationship(EcoreRelationshipType.CONTAINS, Direction.INCOMING)) {
                    container = container.getSingleRelationship(EcoreRelationshipType.CONTAINS, Direction.INCOMING).getStartNode();
                }
                return (EPackage) load((String) container.getProperty(NS_URI));
            }
        }
        return null;
    }

    private Iterable<Node> getOrderedNodes(final Node node, final RelationshipType relType, final Direction direction) {
        return new OrderedNodeIterable(node, relType, direction);
    }

    /**
     * 
     */
    private void initMembers() {
        this.cache = new HashMap<Node, EObject>();
        this.classifierCache = new HashMap<String, Node>();
        this.nodeCache = new HashMap<EObject, Object>();
    }

    /**
     * @param startNode
     *            toplevel node for a persisted model
     * @return
     */
    public EObject load(final Node startNode) {
        initMembers();
        return loadModel(startNode);
    }

    /**
     * Load a model.
     * 
     * @param nsUri
     * @return
     */
    public EObject load(final String nsUri) {
        logger.fine("loading model " + nsUri);

        // initMembers();

        final Node modelNode = getModelNode(nsUri);
        if (null == modelNode) {
            return findTopLevelPackage(nsUri);
        } else {
            return loadModel(modelNode);
        }

    }

    /**
     * @param modelNode
     * @return
     */
    private EObject loadModel(final Node modelNode) {
        final ResourceSet rs = new ResourceSetImpl();
        rs.getResourceFactoryRegistry().getContentTypeToFactoryMap().put("*", new XMIResourceFactoryImpl());

        // restore all models, that the referenced model depends on
        for (final Node referenced : modelNode.traverse(Order.BREADTH_FIRST, StopEvaluator.END_OF_GRAPH, ReturnableEvaluator.ALL_BUT_START_NODE, EcoreRelationshipType.DEPENDS, Direction.OUTGOING, EcoreRelationshipType.INSTANCE_MODEL, Direction.INCOMING)) {
            final String uri = (String) referenced.getProperty(NS_URI);
            final XMIResource resource = (XMIResource) rs.createResource(org.eclipse.emf.common.util.URI.createURI(uri));
            resource.getContents().add(load(uri));
        }
        return restore(modelNode);
    }

    /**
     * Loads an EObject (m1 model) from the given node in nodespace.
     * 
     * @param modelNode
     *            the model node
     * @param cache
     *            Node cache
     * @return the model to load
     */
    private EObject loadObject(final Node modelNode) {
        final Node objectNode = modelNode.getSingleRelationship(EcoreRelationshipType.CONTAINS, Direction.OUTGOING).getEndNode();

        // determine an EObject
        return restoreEObject(objectNode);
    }

    /**
     * Recreate the emf model from our neo4j persistence.
     * 
     * @param node
     *            root node of the model
     * @param cache
     *            cache
     * @return an emf object
     */
    private EObject restore(final Node node) {
        if (null == node) {
            return null;
        }
        if (node.hasRelationship(EcoreRelationshipType.INSTANCE, Direction.INCOMING)) {
            // it's a m2 model
            return restoreEPackage(node);
        } else {
            // m1 model
            return loadObject(node);
        }
    }

    private EAnnotation restoreEAnnotation(final Node node) {
        final EAnnotation annotation = EcoreFactory.eINSTANCE.createEAnnotation();

        // properties
        annotation.setSource((String) node.getProperty(SOURCE));

        // relationships: entries
        final EMap<String, String> entries = annotation.getDetails();
        for (final Node aNode : getOrderedNodes(node, EcoreRelationshipType.CONTAINS, Direction.OUTGOING)) {
            final String key = aNode.hasProperty(KEY_IS_NULL) ? null : (String) aNode.getProperty(KEY);
            final String val = aNode.hasProperty(VALUE_IS_NULL) ? null : (String) aNode.getProperty(VALUE);

            entries.put(key, val);
        }
        return annotation;
    }

    private EAttribute restoreEAttribute(final Node node) {
        if (null != cache.get(node)) {
            return (EAttribute) cache.get(node);
        }

        final EAttribute attribute = EcoreFactory.eINSTANCE.createEAttribute();

        cache.put(node, attribute);

        // properties
        attribute.setChangeable((Boolean) node.getProperty(CHANGEABLE));
        // container is set automatically
        attribute.setDerived((Boolean) node.getProperty(DERIVED));
        attribute.setID((Boolean) node.getProperty(ID));
        attribute.setLowerBound((Integer) node.getProperty(LOWER_BOUND));
        attribute.setName((String) node.getProperty(NAME));
        attribute.setOrdered((Boolean) node.getProperty(ORDERED));
        attribute.setTransient((Boolean) node.getProperty(TRANSIENT));
        attribute.setUnique((Boolean) node.getProperty(UNIQUE));
        attribute.setUnsettable((Boolean) node.getProperty(UNSETTABLE));
        attribute.setUpperBound((Integer) node.getProperty(UPPER_BOUND));
        attribute.setVolatile((Boolean) node.getProperty(VOLATILE));

        if (node.hasProperty(DEFAULT_VALUE_LITERAL)) {
            attribute.setDefaultValueLiteral((String) node.getProperty(DEFAULT_VALUE_LITERAL));
        }
        // relationships
        // type
        final Node genericTypeNode = node.getSingleRelationship(EcoreRelationshipType.GENERIC_TYPE, Direction.OUTGOING).getEndNode();
        attribute.setEGenericType(restoreEGenericType(genericTypeNode));

        // attribute type is set automatically

        // CONTAINS relationship
        for (final Node aNode : getOrderedNodes(node, EcoreRelationshipType.CONTAINS, Direction.OUTGOING)) {
            final Node metaNode = aNode.getSingleRelationship(EcoreRelationshipType.INSTANCE, Direction.INCOMING).getStartNode();

            if (EAnnotation.class.getSimpleName().equals(metaNode.getProperty(NAME))) {
                attribute.getEAnnotations().add(restoreEAnnotation(aNode));
            }
        }

        return attribute;
    }

    private EClass restoreEClass(final Node node) {
        if (null != cache.get(node)) {
            return (EClass) cache.get(node);
        }

        final EClass aClass = EcoreFactory.eINSTANCE.createEClass();

        cache.put(node, aClass);

        // properties
        aClass.setName((String) node.getProperty(NAME));

        if (node.hasProperty(INSTANCE_TYPE_NAME)) {
            aClass.setInstanceTypeName((String) node.getProperty(INSTANCE_TYPE_NAME));
        }

        aClass.setAbstract((Boolean) node.getProperty(INTERFACE));
        aClass.setAbstract((Boolean) node.getProperty(ABSTRACT));

        // relationships
        // supertypes
        if (node.hasRelationship(EcoreRelationshipType.SUPER, Direction.OUTGOING)) {
            for (final Node superNode : getOrderedNodes(node, EcoreRelationshipType.SUPER, Direction.OUTGOING)) {
                final EClass superType = (EClass) restoreEClassifier(superNode);
                if (superType != null) {
                    aClass.getESuperTypes().add(superType);
                }
            }
        }

        // CONTAINS relationship
        for (final Node aNode : getOrderedNodes(node, EcoreRelationshipType.CONTAINS, Direction.OUTGOING)) {
            final Node metaNode = aNode.getSingleRelationship(EcoreRelationshipType.INSTANCE, Direction.INCOMING).getStartNode();

            final Object metaNodeName = metaNode.getProperty(NAME);
            if (EAnnotation.class.getSimpleName().equals(metaNodeName)) {
                aClass.getEAnnotations().add(restoreEAnnotation(aNode));
            } else if (EAttribute.class.getSimpleName().equals(metaNodeName)) {
                aClass.getEStructuralFeatures().add(restoreEAttribute(aNode));
            } else if (EReference.class.getSimpleName().equals(metaNodeName)) {
                aClass.getEStructuralFeatures().add(restoreEReference(aNode));
            } else if (EOperation.class.getSimpleName().equals(metaNodeName)) {
                aClass.getEOperations().add(restoreEOperation(aNode));
            } else if (ETypeParameter.class.getSimpleName().equals(metaNodeName)) {
                final ETypeParameter typeParameter = restoreETypeParameter(aNode);
                if (!aClass.getETypeParameters().contains(typeParameter)) {
                    aClass.getETypeParameters().add(typeParameter);
                }
            }
        }

        return aClass;
    }

    private EClassifier restoreEClassifier(final Node node) {
        final EClassifier classifier = (EClassifier) cache.get(node);
        if (null != classifier) {
            return classifier;
        }

        // omit EClass: it has no incoming INSTANCE relationship
        if (!node.hasRelationship(EcoreRelationshipType.INSTANCE, Direction.INCOMING) && EClass.class.getSimpleName().equals(node.getProperty(NAME))) {
            return restoreEClass(node);
        }

        final Node metaNode = node.getSingleRelationship(EcoreRelationshipType.INSTANCE, Direction.INCOMING).getStartNode();
        final String metaNodeName = (String) metaNode.getProperty(NAME);
        if (EClass.class.getSimpleName().equals(metaNodeName)) {
            return restoreEClass(node);
        } else if (EEnum.class.getSimpleName().equals(metaNodeName)) {
            return restoreEEnum(node);
        } else if (EDataType.class.getSimpleName().equals(metaNodeName)) {
            return restoreEDataType(node);
        }
        return null;
    }

    private EDataType restoreEDataType(final Node node) {
        if (null != cache.get(node)) {
            return (EDataType) cache.get(node);
        }

        final EDataType newDatatype = EcoreFactory.eINSTANCE.createEDataType();

        // properties
        newDatatype.setName((String) node.getProperty(NAME));

        if (node.hasProperty(SERIALIZABLE)) {
            newDatatype.setSerializable((Boolean) node.getProperty(SERIALIZABLE));
        }
        if (node.hasProperty(INSTANCE_TYPE_NAME)) {
            newDatatype.setInstanceTypeName((String) node.getProperty(INSTANCE_TYPE_NAME));
        }

        // relationships
        for (final Node aNode : getOrderedNodes(node, EcoreRelationshipType.CONTAINS, Direction.OUTGOING)) {
            final Node metaNode = aNode.getSingleRelationship(EcoreRelationshipType.INSTANCE, Direction.INCOMING).getStartNode();

            if (ETypeParameter.class.getSimpleName().equals(metaNode.getProperty(NAME))) {
                final ETypeParameter typeParameter = restoreETypeParameter(aNode);
                if (!newDatatype.getETypeParameters().contains(typeParameter)) {
                    newDatatype.getETypeParameters().add(typeParameter);
                }
            }
            if (EAnnotation.class.getSimpleName().equals(metaNode.getProperty(NAME))) {
                newDatatype.getEAnnotations().add(restoreEAnnotation(aNode));
            }
        }

        cache.put(node, newDatatype);
        return newDatatype;
    }

    private EEnum restoreEEnum(final Node node) {
        if (null != cache.get(node)) {
            return (EEnum) cache.get(node);
        }

        final EEnum anEnum = EcoreFactory.eINSTANCE.createEEnum();

        cache.put(node, anEnum);

        // properties
        anEnum.setName((String) node.getProperty(NAME));

        if (node.hasProperty(SERIALIZABLE)) {
            anEnum.setSerializable((Boolean) node.getProperty(SERIALIZABLE));
        }

        if (node.hasProperty(INSTANCE_TYPE_NAME)) {
            anEnum.setInstanceTypeName((String) node.getProperty(INSTANCE_TYPE_NAME));
        }

        // relationships
        for (final Node aNode : getOrderedNodes(node, EcoreRelationshipType.CONTAINS, Direction.OUTGOING)) {
            final Node metaNode = aNode.getSingleRelationship(EcoreRelationshipType.INSTANCE, Direction.INCOMING).getStartNode();

            if (EAnnotation.class.getSimpleName().equals(metaNode.getProperty(NAME))) {
                anEnum.getEAnnotations().add(restoreEAnnotation(aNode));
            }
            if (EEnumLiteral.class.getSimpleName().equals(metaNode.getProperty(NAME))) {
                anEnum.getELiterals().add(restoreEEnumLiteral(aNode));
            }
        }

        return anEnum;
    }

    private EEnumLiteral restoreEEnumLiteral(final Node node) {
        if (cache.containsKey(node)) {
            return (EEnumLiteral) cache.get(node);
        }

        final EEnumLiteral enumLiteral = EcoreFactory.eINSTANCE.createEEnumLiteral();
        cache.put(node, enumLiteral);

        // properties
        enumLiteral.setName((String) node.getProperty(NAME));
        enumLiteral.setLiteral((String) node.getProperty(LITERAL));
        enumLiteral.setValue((Integer) node.getProperty(VALUE));

        // CONTAINS relationship
        for (final Node aNode : getOrderedNodes(node, EcoreRelationshipType.CONTAINS, Direction.OUTGOING)) {
            final Node metaNode = aNode.getSingleRelationship(EcoreRelationshipType.INSTANCE, Direction.INCOMING).getStartNode();

            if (EAnnotation.class.getSimpleName().equals(metaNode.getProperty(NAME))) {
                enumLiteral.getEAnnotations().add(restoreEAnnotation(aNode));
            }
        }

        return enumLiteral;
    }

    private EGenericType restoreEGenericType(final Node node) {
        if (null != cache.get(node)) {
            return (EGenericType) cache.get(node);
        }

        final EGenericType genericType = EcoreFactory.eINSTANCE.createEGenericType();

        if (node.hasRelationship(EcoreRelationshipType.TYPE, Direction.OUTGOING)) {
            // determine top level package node of the classifier
            final Node classifierNode = node.getSingleRelationship(EcoreRelationshipType.TYPE, Direction.OUTGOING).getEndNode();
            final EClassifier classifier = restoreEClassifier(classifierNode);
            genericType.setEClassifier(classifier);
        }
        // get all available type arguments
        for (final Node typeArgumentNode : getOrderedNodes(node, EcoreRelationshipType.GENERIC_TYPE_ARGUMENT, Direction.OUTGOING)) {
            genericType.getETypeArguments().add(restoreEGenericType(typeArgumentNode));
        }

        cache.put(node, genericType);

        return genericType;
    }

    private EObject restoreEObject(final Node objectNode) {
        if (null != cache.get(objectNode)) {
            return cache.get(objectNode);
        }

        // get the meta EClass node
        final Node classNode = objectNode.getSingleRelationship(EcoreRelationshipType.INSTANCE, Direction.INCOMING).getStartNode();
        final String className = (String) classNode.getProperty(NAME);

        // get the meta (sub-)package node
        final Node packageNode = classNode.getSingleRelationship(EcoreRelationshipType.CONTAINS, Direction.INCOMING).getStartNode();

        // dynamically create the EObject
        final EPackage modelPackage = restoreEPackage(packageNode);
        final EClass eClass = (EClass) modelPackage.getEClassifier(className);
        final EFactory factory = modelPackage.getEFactoryInstance();
        final EObject object = factory.create(eClass);

        cache.put(objectNode, object);

        // set structural features
        for (final Node featureNode : getOrderedNodes(objectNode, EcoreRelationshipType.CONTAINS, Direction.OUTGOING)) {
            // find out if the saved feature is a reference or a containment reference
            // or an attribute
            final boolean hasRef = featureNode.hasRelationship(EcoreRelationshipType.REFERENCES, Direction.OUTGOING);
            final boolean hasRefCont = featureNode.hasRelationship(EcoreRelationshipType.REFERENCES_AS_CONTAINMENT, Direction.OUTGOING);
            if (!hasRef && !hasRefCont) {
                // set attribute and its values
                final Node metaNode = featureNode.getSingleRelationship(EcoreRelationshipType.INSTANCE, Direction.INCOMING).getStartNode();
                final EAttribute attribute = restoreEAttribute(metaNode);
                if (attribute.isMany()) {
                    @SuppressWarnings("unchecked")
                    final List<Object> attributes = (List<Object>) object.eGet(attribute);
                    attributes.add(featureNode.getProperty(VALUE));
                } else {
                    object.eSet(attribute, featureNode.getProperty(VALUE));
                }
            } else {
                EcoreRelationshipType relType = null;
                if (hasRef) {
                    relType = EcoreRelationshipType.REFERENCES;
                } else if (hasRefCont) {
                    relType = EcoreRelationshipType.REFERENCES_AS_CONTAINMENT;
                }
                // set reference
                final Node metaNode = featureNode.getSingleRelationship(EcoreRelationshipType.INSTANCE, Direction.INCOMING).getStartNode();
                final EReference reference = restoreEReference(metaNode);
                if (reference.isMany()) {
                    @SuppressWarnings("unchecked")
                    final List<Object> references = (List<Object>) object.eGet(reference);
                    references.add(restoreEObject(featureNode.getSingleRelationship(relType, Direction.OUTGOING).getEndNode()));
                } else {
                    object.eSet(reference, restoreEObject(featureNode.getSingleRelationship(relType, Direction.OUTGOING).getEndNode()));
                }
            }
        }
        return object;
    }

    private EOperation restoreEOperation(final Node node) {
        final EOperation operation = EcoreFactory.eINSTANCE.createEOperation();

        // properties
        operation.setName((String) node.getProperty(NAME));
        operation.setOrdered((Boolean) node.getProperty(ORDERED));
        operation.setUnique((Boolean) node.getProperty(UNIQUE));
        operation.setLowerBound((Integer) node.getProperty(LOWER_BOUND));
        operation.setUpperBound((Integer) node.getProperty(UPPER_BOUND));

        // relationships

        // exceptions
        for (final Node eNode : getOrderedNodes(node, EcoreRelationshipType.EXCEPTION, Direction.OUTGOING)) {
            operation.getEExceptions().add(restoreEClassifier(eNode));
        }

        // type
        if (node.hasRelationship(EcoreRelationshipType.GENERIC_TYPE, Direction.OUTGOING)) {
            final Node genericTypeNode = node.getSingleRelationship(EcoreRelationshipType.GENERIC_TYPE, Direction.OUTGOING).getEndNode();
            operation.setEGenericType(restoreEGenericType(genericTypeNode));
        }

        // CONTAINS relationships
        for (final Node aNode : getOrderedNodes(node, EcoreRelationshipType.CONTAINS, Direction.OUTGOING)) {
            final Node metaNode = aNode.getSingleRelationship(EcoreRelationshipType.INSTANCE, Direction.INCOMING).getStartNode();

            final Object metaNodeName = metaNode.getProperty(NAME);
            if (metaNodeName.equals(EAnnotation.class.getSimpleName())) {
                operation.getEAnnotations().add(restoreEAnnotation(aNode));
            } else if (metaNodeName.equals(EParameter.class.getSimpleName())) {
                operation.getEParameters().add(restoreEParameter(aNode));
            } else if (metaNodeName.equals(ETypeParameter.class.getSimpleName())) {
                final ETypeParameter typeParameter = restoreETypeParameter(aNode);
                if (!operation.getETypeParameters().contains(typeParameter)) {
                    operation.getETypeParameters().add(typeParameter);
                }
            }

        }
        return operation;
    }

    private EPackage restoreEPackage(final Node node) {
        if (cache.containsKey(node)) {
            return (EPackage) cache.get(node);
        }

        final EPackage aPackage = EcoreFactory.eINSTANCE.createEPackage();
        cache.put(node, aPackage);

        aPackage.setName((String) node.getProperty(NAME));
        aPackage.setNsURI((String) node.getProperty(NS_URI));
        aPackage.setNsPrefix((String) node.getProperty(NS_PREFIX));

        for (final Node aNode : getOrderedNodes(node, EcoreRelationshipType.CONTAINS, Direction.OUTGOING)) {
            // omit EClass
            if (aNode.hasProperty(NAME) && EClass.class.getSimpleName().equals(aNode.getProperty(NAME))) {
                aPackage.getEClassifiers().add(restoreEClass(aNode));
                continue;
            }

            final Node metaNode = aNode.getSingleRelationship(EcoreRelationshipType.INSTANCE, Direction.INCOMING).getStartNode();
            final Object metaNodeName = metaNode.getProperty(NAME);
            if (EAnnotation.class.getSimpleName().equals(metaNodeName)) {
                aPackage.getEAnnotations().add(restoreEAnnotation(aNode));
            } else if (EClass.class.getSimpleName().equals(metaNodeName)) {
                aPackage.getEClassifiers().add(restoreEClass(aNode));
            } else if (EDataType.class.getSimpleName().equals(metaNodeName)) {
                aPackage.getEClassifiers().add(restoreEDataType(aNode));
            } else if (EEnum.class.getSimpleName().equals(metaNodeName)) {
                aPackage.getEClassifiers().add(restoreEEnum(aNode));
            } else if (EPackage.class.getSimpleName().equals(metaNodeName)) {
                aPackage.getESubpackages().add(restoreEPackage(aNode));
            }
        }
        return aPackage;
    }

    private EParameter restoreEParameter(final Node node) {
        final EParameter parameter = EcoreFactory.eINSTANCE.createEParameter();

        // properties
        parameter.setName((String) node.getProperty(NAME));
        parameter.setOrdered((Boolean) node.getProperty(ORDERED));
        // many and required can not be set
        parameter.setUnique((Boolean) node.getProperty(UNIQUE));
        parameter.setLowerBound((Integer) node.getProperty(LOWER_BOUND));
        parameter.setUpperBound((Integer) node.getProperty(UPPER_BOUND));

        // relationships
        // type
        final Node genericTypeNode = node.getSingleRelationship(EcoreRelationshipType.GENERIC_TYPE, Direction.OUTGOING).getEndNode();
        parameter.setEGenericType(restoreEGenericType(genericTypeNode));

        // CONTAINS relationship
        for (final Node aNode : getOrderedNodes(node, EcoreRelationshipType.CONTAINS, Direction.OUTGOING)) {
            final Node metaNode = aNode.getSingleRelationship(EcoreRelationshipType.INSTANCE, Direction.INCOMING).getStartNode();

            if (EAnnotation.class.getSimpleName().equals(metaNode.getProperty(NAME))) {
                parameter.getEAnnotations().add(restoreEAnnotation(aNode));
            }
        }

        return parameter;
    }

    private EReference restoreEReference(final Node node) {
        if (null != cache.get(node)) {
            return (EReference) cache.get(node);
        }

        final EReference reference = EcoreFactory.eINSTANCE.createEReference();

        cache.put(node, reference);

        // properties
        reference.setChangeable((Boolean) node.getProperty(CHANGEABLE));
        // container is set automatically
        reference.setContainment((Boolean) node.getProperty(CONTAINMENT));
        reference.setDerived((Boolean) node.getProperty(DERIVED));
        reference.setLowerBound((Integer) node.getProperty(LOWER_BOUND));
        reference.setName((String) node.getProperty(NAME));
        reference.setOrdered((Boolean) node.getProperty(ORDERED));
        reference.setResolveProxies((Boolean) node.getProperty(RESOLVE_PROXIES));
        reference.setTransient((Boolean) node.getProperty(TRANSIENT));
        reference.setUnique((Boolean) node.getProperty(UNIQUE));
        reference.setUnsettable((Boolean) node.getProperty(UNSETTABLE));
        reference.setUpperBound((Integer) node.getProperty(UPPER_BOUND));
        reference.setVolatile((Boolean) node.getProperty(VOLATILE));

        if (node.hasProperty(DEFAULT_VALUE_LITERAL)) {
            reference.setDefaultValueLiteral((String) node.getProperty(DEFAULT_VALUE_LITERAL));
        }
        // relationships
        // type
        final Node genericTypeNode = node.getSingleRelationship(EcoreRelationshipType.GENERIC_TYPE, Direction.OUTGOING).getEndNode();
        reference.setEGenericType(restoreEGenericType(genericTypeNode));

        // keys
        for (final Node keyNode : getOrderedNodes(node, EcoreRelationshipType.E_KEY, Direction.OUTGOING)) {
            reference.getEKeys().add(restoreEAttribute(keyNode));
        }

        // opposite
        final Relationship oppositeRelationship = node.getSingleRelationship(EcoreRelationshipType.OPPOSITE, Direction.OUTGOING);
        if (null != oppositeRelationship) {
            reference.setEOpposite(restoreEReference(oppositeRelationship.getEndNode()));
            restoreEReference(oppositeRelationship.getEndNode()).setEOpposite(reference);
        }

        // CONTAINS relationship
        for (final Node aNode : getOrderedNodes(node, EcoreRelationshipType.CONTAINS, Direction.OUTGOING)) {
            final Node metaNode = aNode.getSingleRelationship(EcoreRelationshipType.INSTANCE, Direction.INCOMING).getStartNode();

            if (EAnnotation.class.getSimpleName().equals(metaNode.getProperty(NAME))) {
                reference.getEAnnotations().add(restoreEAnnotation(aNode));
            }
        }

        return reference;
    }

    private ETypeParameter restoreETypeParameter(final Node node) {
        if (cache.containsKey(node)) {
            return (ETypeParameter) cache.get(node);
        }
        final ETypeParameter typeParameter = EcoreFactory.eINSTANCE.createETypeParameter();
        cache.put(node, typeParameter);

        // properties
        typeParameter.setName((String) node.getProperty(NAME));

        // CONTAINS relationship
        for (final Node aNode : getOrderedNodes(node, EcoreRelationshipType.CONTAINS, Direction.OUTGOING)) {
            final Node metaNode = aNode.getSingleRelationship(EcoreRelationshipType.INSTANCE, Direction.INCOMING).getStartNode();

            if (EAnnotation.class.getSimpleName().equals(metaNode.getProperty(NAME))) {
                typeParameter.getEAnnotations().add(restoreEAnnotation(aNode));
            }
        }

        cache.put(node, typeParameter);

        return typeParameter;
    }
}
