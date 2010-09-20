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

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Logger;

import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.infai.amor.backend.ModelLocation;
import org.infai.amor.backend.internal.NeoProvider;
import org.infai.amor.backend.neo.NeoModelLocation;
import org.infai.amor.backend.resources.AmorResourceSetImpl;
import org.infai.amor.backend.util.EcoreModelHelper;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.Traverser.Order;

import com.google.common.collect.Lists;

public class NeoRestorer extends AbstractNeoPersistence {
    static class OrderedNodeIterable implements Iterable<Node> {
        private final Node node;
        private final Object[] options;

        public OrderedNodeIterable(final Node node, final Object... options) {
            this.node = node;
            this.options = options;
        }

        @Override
        public Iterator<Node> iterator() {
            final Set<Node> nodes = new TreeSet<Node>(NODE_POSITION_COMPARATOR);
            nodes.addAll(node.traverse(Order.BREADTH_FIRST, StopEvaluator.DEPTH_ONE, ReturnableEvaluator.ALL_BUT_START_NODE, options).getAllNodes());
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

    private ResourceSetImpl resourceSet;

    /**
     * @param neo
     */
    public NeoRestorer(final NeoProvider neo) {
        super(neo);
        initMembers();
    }

    /**
     * Find the relative path to the file this proxy uri relates to. The path has the same root as {@link #currentResourceUri}.
     * @param proxy
     * @return
     */
    protected URI deresolve(final URI proxyUri) {
        URI pseudoAbsCurrentUri = org.eclipse.emf.common.util.URI.createURI("file://dummy/" + currentResourceUri);
        URI pseudoAbsProxyUri = org.eclipse.emf.common.util.URI.createURI("file://dummy/" + proxyUri);
        return pseudoAbsProxyUri.deresolve(pseudoAbsCurrentUri);
    }


    /**
     * @param name
     * @return
     */
    private EDataType fetchEcoreDataTypeViaReflection(final String name) {
        final EcorePackage ecorePackage = EcoreFactory.eINSTANCE.getEcorePackage();
        try {
            final Method method = ecorePackage.getClass().getMethod("get"+name, null);
            logger.finest(String.format("trying to fetch ecore element named '%s' via method '%s'", name, method));
            return (EDataType) method.invoke(ecorePackage, null);
        } catch (final Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private Iterable<Node> getOrderedNodes(final Node node, final RelationshipType relType, final Direction direction) {
        return new OrderedNodeIterable(node, relType, direction);
    }

    private Iterable<Node> getOrderedNodes(final Node node, final RelationshipType relType, final Direction direction, final RelationshipType relType2, final Direction direction2) {
        return new OrderedNodeIterable(node, relType, direction, relType2, direction2);
    }

    /**
     * 
     */
    private void initMembers() {
        this.cache = new HashMap<Node, EObject>();
        this.classifierCache = new HashMap<String, Node>();
        this.resourceSet = new AmorResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getContentTypeToFactoryMap().put("*", new XMIResourceFactoryImpl());
    }

    /**
     * @param modelLocation.getModelHead()
     *            toplevel node for a persisted model
     * @return
     */
    public List<EObject> load(final NeoModelLocation modelLocation) {
        // initMembers();
        // FIXME when checking out model with reference to another object of a different package,
        // FIXME the reference will point to the eclass instead of the eobject :(
        final List<EObject> result = Lists.newArrayList();
        final Resource resource = resourceSet.createResource(org.eclipse.emf.common.util.URI.createURI(modelLocation.getRelativePath()));
        // FIXME wtf, we are overwriting currentResourceUri every time a dependency model gets restored?
        currentResourceUri = resource.getURI();

        final Node rootNode = modelLocation.getModelHead();

        for (Node modelHeadNode : new OrderedNodeIterable(rootNode, EcoreRelationshipType.MODEL_CONTENT, Direction.OUTGOING)) {
            // debug(modelHeadNode);
            // TODO why do we need to do this for instance models again?
            final Relationship isInstanceModelRel = modelHeadNode.getSingleRelationship(EcoreRelationshipType.CONTAINS, Direction.INCOMING);
            if (isInstanceModelRel != null) {
                modelHeadNode = isInstanceModelRel.getOtherNode(modelHeadNode);
            }
            result.add(loadModel(modelHeadNode));
        }
        resource.getContents().addAll(result);
        return result;
    }

    /**
     * @param modelNode
     * @return
     */
    private EObject loadModel(final Node modelNode) {
        EcoreFactory.eINSTANCE.eClass();
        // restore all models, that the referenced model depends on
        // this fills our cache of eobjects
        for (final Node referenced : getOrderedNodes(modelNode, EcoreRelationshipType.DEPENDS, Direction.OUTGOING, EcoreRelationshipType.INSTANCE_MODEL, Direction.INCOMING)) {
            final String uri = getString(referenced, NS_URI);
            if (uri == null) {
                if (getString(referenced, ModelLocation.EXTERNAL_URI) != null) {
                    final NeoModelLocation location = new NeoModelLocation(null, referenced);
                    if (resourceSet.getResource(org.eclipse.emf.common.util.URI.createURI(location.getRelativePath()), false) == null) {
                        // final Resource resource =
                        final Resource resource = resourceSet.createResource(deresolve(org.eclipse.emf.common.util.URI.createURI(location.getRelativePath())));
                        List<EObject> loadedEObjects = load(location);
                        // register all loaded packages
                        resourceSet.getPackageRegistry().putAll(EcoreModelHelper.createPackageNamespaceMap(loadedEObjects));
                        resource.getContents().addAll(loadedEObjects);
                    }
                }
                continue;
            }
            Resource resource = resourceSet.getResource(org.eclipse.emf.common.util.URI.createURI(uri), false);
            if (resource == null) {
                resource = resourceSet.createResource(org.eclipse.emf.common.util.URI.createURI(uri));
                logger.finer("restoring package " + uri);
                resource.getContents().add(loadModel(referenced));
            }
        }
        final EObject restored = restore(modelNode);
        return restored;
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
        annotation.setSource(getString(node, SOURCE));

        // relationships: entries
        final EMap<String, String> entries = annotation.getDetails();
        for (final Node aNode : getOrderedNodes(node, EcoreRelationshipType.CONTAINS, Direction.OUTGOING)) {
            final String key = aNode.hasProperty(KEY_IS_NULL) ? null : getString(aNode, KEY);
            final String val = aNode.hasProperty(VALUE_IS_NULL) ? null : getString(aNode, VALUE);

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
        attribute.setChangeable(getBool(node, CHANGEABLE));
        // container is set automatically
        attribute.setDerived(getBool(node, DERIVED));
        attribute.setID(getBool(node, ID));
        attribute.setLowerBound(getInt(node, LOWER_BOUND));
        attribute.setName(getString(node, NAME));
        attribute.setOrdered(getBool(node, ORDERED));
        attribute.setTransient(getBool(node, TRANSIENT));
        attribute.setUnique(getBool(node, UNIQUE));
        attribute.setUnsettable(getBool(node, UNSETTABLE));
        attribute.setUpperBound(getInt(node, UPPER_BOUND));
        attribute.setVolatile(getBool(node, VOLATILE));

        if (node.hasProperty(DEFAULT_VALUE_LITERAL)) {
            attribute.setDefaultValueLiteral(getString(node, DEFAULT_VALUE_LITERAL));
        }
        // relationships
        // type
        final Node genericTypeNode = node.getSingleRelationship(EcoreRelationshipType.GENERIC_TYPE, Direction.OUTGOING).getEndNode();
        attribute.setEGenericType(restoreEGenericType(genericTypeNode));

        // attribute type is set automatically

        // CONTAINS relationship
        for (final Node aNode : getOrderedNodes(node, EcoreRelationshipType.CONTAINS, Direction.OUTGOING)) {
            final Node metaNode = aNode.getSingleRelationship(EcoreRelationshipType.INSTANCE, Direction.INCOMING).getStartNode();

            if (EAnnotation.class.getSimpleName().equals(getString(metaNode, NAME))) {
                attribute.getEAnnotations().add(restoreEAnnotation(aNode));
            }
        }

        return attribute;
    }

    private EClass restoreEClass(final Node node) {
        if (null != cache.get(node)) {
            return (EClass) cache.get(node);
        }
        // TODO need to restore package as well, if we haven't yet
        final EClass aClass = EcoreFactory.eINSTANCE.createEClass();

        cache.put(node, aClass);

        if (node.hasProperty("proxyUri")) {
            ((InternalEObject) aClass).eSetProxyURI(org.eclipse.emf.common.util.URI.createURI(getString(node, "proxyUri")));
        } else {

            // properties
            aClass.setName(getString(node, NAME));

            logger.finest("restoring eclass " + aClass.getName());

            if (node.hasProperty(INSTANCE_TYPE_NAME)) {
                aClass.setInstanceTypeName(getString(node, INSTANCE_TYPE_NAME));
            }

            aClass.setInterface(getBool(node, INTERFACE));
            aClass.setAbstract(getBool(node, ABSTRACT));

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
        final String metaNodeName = getString(metaNode, NAME);
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

        final String name = getString(node, NAME);
        final Node ecoreClassifierNode = determineEcoreClassifierNode(name);
        if (ecoreClassifierNode != null) {
            // this is a ecore datatype
            final EDataType dt = fetchEcoreDataTypeViaReflection(name);
            cache.put(node, dt);
            return dt;
        }
        final EDataType newDatatype = EcoreFactory.eINSTANCE.createEDataType();

        // properties
        newDatatype.setName(name);

        if (node.hasProperty(SERIALIZABLE)) {
            newDatatype.setSerializable(getBool(node, SERIALIZABLE));
        }
        if (node.hasProperty(INSTANCE_TYPE_NAME)) {
            newDatatype.setInstanceTypeName(getString(node, INSTANCE_TYPE_NAME));
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
        anEnum.setName(getString(node, NAME));

        if (node.hasProperty(SERIALIZABLE)) {
            anEnum.setSerializable(getBool(node, SERIALIZABLE));
        }

        if (node.hasProperty(INSTANCE_TYPE_NAME)) {
            anEnum.setInstanceTypeName(getString(node, INSTANCE_TYPE_NAME));
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
        enumLiteral.setName(getString(node, NAME));
        enumLiteral.setLiteral(getString(node, LITERAL));
        enumLiteral.setValue(getInt(node, VALUE));

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
        final String className = getString(classNode, NAME);

        // get the meta (sub-)package node
        final Node packageNode = classNode.getSingleRelationship(EcoreRelationshipType.CONTAINS, Direction.INCOMING).getStartNode();

        // dynamically create the EObject
        final EPackage modelPackage = restoreEPackage(packageNode);
        final EClass eClass = (EClass) modelPackage.getEClassifier(className);
        final EFactory factory = modelPackage.getEFactoryInstance();
        final EObject object = factory.create(eClass);

        cache.put(objectNode, object);
        logger.finest("restoring object of type " + className);

        if (objectNode.hasProperty("proxyUri")) {
            final URI proxyUri = org.eclipse.emf.common.util.URI.createURI((String) objectNode.getProperty("proxyUri"));
            ((InternalEObject) object).eSetProxyURI(proxyUri);
            logger.finest("using proxy to " + proxyUri);
        } else {
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
                    Object featureValue = featureNode.getProperty(VALUE);

                    if (getBool(featureNode, ISBIGDECIMAL)) {
                        // restore bigdecimal
                        featureValue = new BigDecimal((String) featureValue);
                    }
                    logger.finest(String.format("  attr '%s' of type '%s'", attribute.getName(), attribute.getEType().getName()));

                    if (attribute.isMany()) {
                        @SuppressWarnings("unchecked")
                        final List<Object> attributes = (List<Object>) object.eGet(attribute);
                        attributes.add(featureValue);
                    } else {
                        object.eSet(attribute, featureValue);
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
                    logger.finest(String.format("  ref '%s' to type '%s'", reference.getName(), reference.getEReferenceType().getName()));
                    if (reference.isMany()) {
                        @SuppressWarnings("unchecked")
                        final List<Object> references = (List<Object>) object.eGet(reference);
                        references.add(restoreEObject(featureNode.getSingleRelationship(relType, Direction.OUTGOING).getEndNode()));
                    } else {
                        object.eSet(reference, restoreEObject(featureNode.getSingleRelationship(relType, Direction.OUTGOING).getEndNode()));
                    }
                }
            }
        }
        return object;
    }

    private EOperation restoreEOperation(final Node node) {
        final EOperation operation = EcoreFactory.eINSTANCE.createEOperation();

        // properties
        operation.setName(getString(node, NAME));
        operation.setOrdered(getBool(node, ORDERED));
        operation.setUnique(getBool(node, UNIQUE));
        operation.setLowerBound(getInt(node, LOWER_BOUND));
        operation.setUpperBound(getInt(node, UPPER_BOUND));

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

        aPackage.setName(getString(node, NAME));
        aPackage.setNsURI(getString(node, NS_URI));
        aPackage.setNsPrefix(getString(node, NS_PREFIX));

        for (final Node aNode : getOrderedNodes(node, EcoreRelationshipType.CONTAINS, Direction.OUTGOING)) {
            // omit EClass
            // debug(aNode);
            if (aNode.hasProperty(NAME) && EClass.class.getSimpleName().equals(aNode.getProperty(NAME))) {
                aPackage.getEClassifiers().add(restoreEClass(aNode));
                continue;
            }

            final Node metaNode = aNode.getSingleRelationship(EcoreRelationshipType.INSTANCE, Direction.INCOMING).getStartNode();
            final Object metaNodeName = metaNode.getProperty(NAME);
            //            System.out.println("restoring " + aPackage.getName() + ": " + metaNodeName);
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
        parameter.setName(getString(node, NAME));
        parameter.setOrdered(getBool(node, ORDERED));
        // many and required can not be set
        parameter.setUnique(getBool(node, UNIQUE));
        parameter.setLowerBound(getInt(node, LOWER_BOUND));
        parameter.setUpperBound(getInt(node, UPPER_BOUND));

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
        reference.setChangeable(getBool(node, CHANGEABLE));
        // container is set automatically
        reference.setContainment(getBool(node, CONTAINMENT));
        reference.setDerived(getBool(node, DERIVED));
        reference.setLowerBound(getInt(node, LOWER_BOUND));
        reference.setName(getString(node, NAME));
        reference.setOrdered(getBool(node, ORDERED));
        reference.setResolveProxies(getBool(node, RESOLVE_PROXIES));
        reference.setTransient(getBool(node, TRANSIENT));
        reference.setUnique(getBool(node, UNIQUE));
        reference.setUnsettable(getBool(node, UNSETTABLE));
        reference.setUpperBound(getInt(node, UPPER_BOUND));
        reference.setVolatile(getBool(node, VOLATILE));

        if (node.hasProperty(DEFAULT_VALUE_LITERAL)) {
            reference.setDefaultValueLiteral(getString(node, DEFAULT_VALUE_LITERAL));
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
        typeParameter.setName(getString(node, NAME));

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
