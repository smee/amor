/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.internal.storage.neo;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EGenericType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.ETypeParameter;
import org.eclipse.emf.ecore.impl.EStringToStringMapEntryImpl;
import org.infai.amor.backend.internal.NeoProvider;
import org.neo4j.api.core.Node;

/**
 * Import all {@link EObject}s into neo.
 */
public class NeoMappingDispatcher extends AbstractNeoDispatcher {

    /**
     * @param neo
     */
    public NeoMappingDispatcher(final NeoProvider neo) {
        super(neo);
    }

    /**
     * @param container
     * @param to
     */
    private void addContains(final EObject container, final Node to) {
        getNodeFor(container).createRelationshipTo(to, EcoreRelationshipType.CONTAINS);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.storage.neo.EMFDispatcher#store(org.eclipse.emf.ecore.EAnnotation)
     */
    @Override
    public void store(final EAnnotation element) {
        final Node node = createNode();

        set(node, SOURCE, element.getSource(), "");
        // TODO add contents
        addContains(element.getEModelElement(), node);
        nodeCache.put(element, node);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.storage.neo.EMFDispatcher#store(org.eclipse.emf.ecore.EAttribute)
     */
    @Override
    public void store(final EAttribute element) {
        final Node node = createNode();
        set(node, CHANGEABLE, element.isChangeable());
        set(node, ID, element.isID());
        set(node, DERIVED, element.isDerived());
        set(node, TRANSIENT, element.isTransient());
        set(node, LOWER_BOUND, element.getLowerBound());
        set(node, NAME, element.getName());
        set(node, ORDERED, element.isOrdered());
        set(node, UNIQUE, element.isUnique());
        set(node, UNSETTABLE, element.isUnsettable());
        set(node, UPPER_BOUND, element.getUpperBound());
        set(node, VOLATILE, element.isVolatile());
        set(node, DEFAULT_VALUE_LITERAL, element.getDefaultValueLiteral());

        addContains(element.getEContainingClass(), node);
        nodeCache.put(element, node);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.storage.neo.EMFDispatcher#store(org.eclipse.emf.ecore.EClass)
     */
    @Override
    public void store(final EClass element) {
        final Node node = createNode();
        set(node, NAME, element.getName());
        set(node, ABSTRACT, element.isAbstract());
        set(node, INTERFACE, element.isInterface());
        set(node, INSTANCE_TYPE_NAME, element.getInstanceTypeName());
        set(node, DEFAULT_VALUE, element.getDefaultValue());

        addContains(element.getEPackage(), node);
        nodeCache.put(element, node);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.storage.neo.EMFDispatcher#store(org.eclipse.emf.ecore.EDataType)
     */
    @Override
    public void store(final EDataType element) {
        final Node node = createNode();
        set(node, NAME, element.getName());
        set(node, SERIALIZABLE, element.isSerializable());
        set(node, INSTANCE_TYPE_NAME, element.getInstanceTypeName());
        set(node, INSTANCE_CLASS_NAME, element.getInstanceClassName());
        set(node, DEFAULT_VALUE, element.getDefaultValue());

        addContains(element.getEPackage(), node);
        nodeCache.put(element, node);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.storage.neo.EMFDispatcher#store(org.eclipse.emf.ecore.EEnum)
     */
    @Override
    public void store(final EEnum element) {
        final Node node = createNode();
        set(node, NAME, element.getName());
        set(node, SERIALIZABLE, element.isSerializable());
        set(node, INSTANCE_TYPE_NAME, element.getInstanceTypeName());

        addContains(element.getEPackage(), node);
        nodeCache.put(element, node);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.storage.neo.EMFDispatcher#store(org.eclipse.emf.ecore.EEnumLiteral)
     */
    @Override
    public void store(final EEnumLiteral element) {
        final Node node = createNode();
        set(node, NAME, element.getName());
        set(node, VALUE, element.getValue());

        set(node, LITERAL, element.getLiteral());

        // relationships
        final EEnum container = element.getEEnum();
        addContains(container, node);

        if (element.equals(container.getDefaultValue())) {
            final Node containerNode = getNodeFor(container);
            node.createRelationshipTo(containerNode, EcoreRelationshipType.DEFAULT_VALUE);
        }
        nodeCache.put(element, node);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.storage.neo.EMFDispatcher#store(org.eclipse.emf.ecore.EGenericType)
     */
    @Override
    public void store(final EGenericType element) {
        final Node genericTypeNode = createNode();

        final EObject container = element.eContainer();
        addContains(container, genericTypeNode);
        getNodeFor(container).createRelationshipTo(genericTypeNode, EcoreRelationshipType.GENERIC_TYPE);

        nodeCache.put(element, genericTypeNode);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.storage.neo.EMFDispatcher#store(org.eclipse.emf.ecore.EObject)
     */
    @Override
    public void store(final EObject element) {
        final Node node = createNode();

        // relationships
        final EObject container = element.eContainer();
        if (null == container) {
            final Node modelNode = createNode();

            // bind dummy node to MODELS
            final String metaNsUri = element.eClass().getEPackage().getNsURI();
            set(modelNode, NS_URI, metaNsUri + " [" + element.eResource().getURI() + "]");
            set(modelNode, URI, String.valueOf(element.eResource().getURI()));
            getFactoryNode(EcoreRelationshipType.RESOURCES).createRelationshipTo(modelNode, EcoreRelationshipType.RESOURCE);

            // bind node to dummy container node
            modelNode.createRelationshipTo(node, EcoreRelationshipType.CONTAINS);
            // bind dummy container node to its meta model
            EPackage topLevelPackage = element.eClass().getEPackage();
            while (null != topLevelPackage.getESuperPackage()) {
                topLevelPackage = topLevelPackage.getESuperPackage();
            }
            final Node metaPackageNode = getModelNode(topLevelPackage.getNsURI());
            metaPackageNode.createRelationshipTo(modelNode, EcoreRelationshipType.INSTANCE_MODEL);
        }

        nodeCache.put(element, node);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.storage.neo.EMFDispatcher#store(org.eclipse.emf.ecore.EOperation)
     */
    @Override
    public void store(final EOperation element) {
        final Node node = createNode();
        set(node, NAME, element.getName());
        set(node, ORDERED, element.isOrdered());
        set(node, UNIQUE, element.isUnique());
        set(node, LOWER_BOUND, element.getLowerBound());
        set(node, UPPER_BOUND, element.getUpperBound());

        addContains(element.getEContainingClass(), node);

        final EList<EClassifier> exceptions = element.getEExceptions();
        for (final EClassifier exception : exceptions) {
            node.createRelationshipTo(getNodeFor(exception), EcoreRelationshipType.EXCEPTION);
        }

        nodeCache.put(element, node);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.storage.neo.EMFDispatcher#store(org.eclipse.emf.ecore.EPackage)
     */
    @Override
    public void store(final EPackage element) {
        // properties
        final Node node = createNode();
        set(node, NAME, element.getName());
        set(node, NS_URI, element.getNsURI());
        set(node, NS_PREFIX, element.getNsPrefix(), "");

        // relationships
        final EPackage container = element.getESuperPackage();
        if (null == container) {
            // model node
            getFactoryNode(EcoreRelationshipType.RESOURCES).createRelationshipTo(node, EcoreRelationshipType.RESOURCE);
        } else {
            // sub package node
            addContains(container, node);
        }

        nodeCache.put(element, node);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.storage.neo.EMFDispatcher#store(org.eclipse.emf.ecore.EParameter)
     */
    @Override
    public void store(final EParameter element) {
        final Node node = createNode();
        set(node, NAME, element.getName());
        set(node, LOWER_BOUND, element.getLowerBound());
        set(node, UPPER_BOUND, element.getUpperBound());
        set(node, ORDERED, element.isOrdered());
        set(node, REQUIRED, element.isRequired());
        set(node, UNIQUE, element.isUnique());
        set(node, MANY, element.isMany());

        addContains(element.getEOperation(), node);
        nodeCache.put(element, node);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.storage.neo.EMFDispatcher#store(org.eclipse.emf.ecore.EReference)
     */
    @Override
    public void store(final EReference element) {
        final Node node = createNode();
        set(node, CHANGEABLE, element.isChangeable());
        set(node, CONTAINER, element.isContainer());
        set(node, CONTAINMENT, element.isContainment());
        set(node, LOWER_BOUND, element.getLowerBound());
        set(node, NAME, element.getName());
        set(node, ORDERED, element.isOrdered());
        set(node, RESOLVE_PROXIES, element.isResolveProxies());
        set(node, UNIQUE, element.isUnique());
        set(node, UNSETTABLE, element.isUnsettable());
        set(node, UPPER_BOUND, element.getUpperBound());
        set(node, VOLATILE, element.isVolatile());
        set(node, DERIVED, element.isDerived());
        set(node, TRANSIENT, element.isTransient());

        final String defaultValueLiteral = element.getDefaultValueLiteral();
        if (null != defaultValueLiteral) {
            set(node, DEFAULT_VALUE_LITERAL, defaultValueLiteral);
        }

        addContains(element.getEContainingClass(), node);

        final EList<EAttribute> eKeys = element.getEKeys();
        for (final EAttribute eKey : eKeys) {
            node.createRelationshipTo(getNodeFor(eKey), EcoreRelationshipType.E_KEY);
        }

        final EReference opposite = element.getEOpposite();
        if (null != opposite && null != getNodeFor(opposite)) {
            node.createRelationshipTo(getNodeFor(opposite), EcoreRelationshipType.OPPOSITE);
        }

        nodeCache.put(element, node);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.infai.amor.backend.internal.storage.neo.EMFDispatcher#store(org.eclipse.emf.ecore.impl.EStringToStringMapEntryImpl)
     */
    @Override
    public void store(final EStringToStringMapEntryImpl element) {
        final Node node = createNode();

        final String key = element.getKey();
        final String val = element.getValue();

        if (null == key) {
            set(node, KEY_IS_NULL, true);
        } else {
            set(node, KEY, element.getKey());
        }

        if (null == val) {
            set(node, VALUE_IS_NULL, true);
        } else {
            set(node, VALUE, element.getValue());
        }

        addContains(element.eContainer(), node);
        nodeCache.put(element, node);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.storage.neo.EMFDispatcher#store(org.eclipse.emf.ecore.ETypeParameter)
     */
    @Override
    public void store(final ETypeParameter element) {
        final Node node = createNode();
        set(node, NAME, element.getName());

        addContains(element.eContainer(), node);
        nodeCache.put(element, node);
    }
}
