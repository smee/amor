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
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.ETypeParameter;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.impl.EStringToStringMapEntryImpl;
import org.infai.amor.backend.internal.NeoProvider;
import org.neo4j.api.core.Direction;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.ReturnableEvaluator;
import org.neo4j.api.core.StopEvaluator;
import org.neo4j.api.core.Traverser.Order;

/**
 * Import references to other models, meta classes etc.
 */
public class NeoMetadataDispatcher extends AbstractNeoDispatcher {
    /**
     * @param neo
     */
    public NeoMetadataDispatcher(final NeoProvider neo) {
        super(neo);
    }

    /**
     * Finds the neo4j node for the given {@link EClassifier}.
     * 
     * @param element
     *            the {@link EClassifier}
     * @return the classifier node
     */
    private Node findClassifierNode(final EClassifier element) {
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
                        nodeCache.put(element, aNode);
                        return aNode;
                    }
                    final Node metaNode = metaRel.getStartNode();
                    final Object metaNodeName = metaNode.getProperty(NAME);
                    if (EcorePackage.Literals.ECLASS.getName().equals(metaNodeName) || EcorePackage.Literals.EDATA_TYPE.getName().equals(metaNodeName) || EcorePackage.Literals.EENUM.getName().equals(metaNodeName)) {
                        // cache the element node
                        nodeCache.put(element, aNode);
                        return aNode;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Search for the neo4j node corresponding to the given EStructuralFeature.
     * 
     * @param feature
     *            the {@link EStructuralFeature}
     * @return the structural feature node
     */
    private Node findFeatureNode(final EStructuralFeature feature) {
        final Node node = getNodeFor(feature);
        if (node != null) {
            return node;
        }

        // determine the container EClassifier of this element
        final Node classNode = findClassifierNode(feature.getEContainingClass());

        for (final Node aNode : classNode.traverse(Order.BREADTH_FIRST, StopEvaluator.DEPTH_ONE, ReturnableEvaluator.ALL_BUT_START_NODE, EcoreRelationshipType.CONTAINS, Direction.OUTGOING)) {
            if (aNode.hasProperty(NAME)) {
                final String name = (String) aNode.getProperty(NAME);
                if (name.equals(feature.getName())) {
                    // meta relationship?
                    final Node metaNode = aNode.getSingleRelationship(EcoreRelationshipType.INSTANCE, Direction.INCOMING).getStartNode();
                    if (!EcorePackage.Literals.EOPERATION.getName().equals(metaNode.getProperty(NAME))) {
                        // cache the element node
                        nodeCache.put(feature, aNode);
                        return aNode;
                    }
                }
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
     * Set meta element.
     * 
     * @param element
     *            Element to assign with its meta model element
     * @param clazz
     *            Ecore class of meta model element
     * @param nodeCache
     *            Node cache
     */
    private void setMetaElement(final EObject element, final Class<? extends EObject> clazz) {
        final String mapEntryName = EcorePackage.Literals.ESTRING_TO_STRING_MAP_ENTRY.getName();
        final String className = clazz.getSimpleName().contains(mapEntryName) ? mapEntryName : clazz.getSimpleName();

        final Node node = determineEcoreClassifierNode(className);
        if (null == node) {
            throw new IllegalStateException("The meta element " + clazz.getSimpleName() + " could not be found!");
        }

        // omit reflexive relationships for self describing elements (like EClass)
        final Node target = getNodeFor(element);
        if (node.equals(target)) {
            return;
        }

        node.createRelationshipTo(target, EcoreRelationshipType.INSTANCE);
    }

    /**
     * Set the super elements.
     * 
     * @param element
     *            Element to assign with its super elements.
     * @param nodeCache
     *            Node cache
     */
    private void setSuperElements(final EClass element) {
        final Node node = getNodeFor(element);

        final EList<EClass> supertypes = element.getESuperTypes();
        for (final EClass supertype : supertypes) {
            node.createRelationshipTo(getNodeFor(supertype), EcoreRelationshipType.SUPER);
        }
    }

    /**
     * Set classifier.
     * 
     * @param element
     *            EGenericType to assign with its classifier
     * @param nodeCache
     *            Node cache
     */
    private void setTypeElement(final EGenericType element) {
        // set EType
        final EClassifier eType = element.getEClassifier();
        if (eType != null) {
            final Node eTypeNode = findClassifierNode(eType);
            if (null == eTypeNode) {
                throw new IllegalStateException("The type element " + eType + " could not be found!");
            }
            // find toplevel container
            Node container = getNodeFor(element);
            while (container.hasRelationship(EcoreRelationshipType.CONTAINS, Direction.INCOMING)) {
                container = container.getSingleRelationship(EcoreRelationshipType.CONTAINS, Direction.INCOMING).getStartNode();
            }

            // find toplevel typecontainer
            Node typeContainer = eTypeNode;
            while (typeContainer.hasRelationship(EcoreRelationshipType.CONTAINS, Direction.INCOMING)) {
                typeContainer = typeContainer.getSingleRelationship(EcoreRelationshipType.CONTAINS, Direction.INCOMING).getStartNode();
            }

            if (!container.equals(typeContainer)) {
                if (!container.hasRelationship(EcoreRelationshipType.DEPENDS, Direction.OUTGOING)) {
                    container.createRelationshipTo(typeContainer, EcoreRelationshipType.DEPENDS);
                }
            }

            getNodeFor(element).createRelationshipTo(eTypeNode, EcoreRelationshipType.TYPE);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.storage.neo.EMFDispatcher#store(org.eclipse.emf.ecore.EAnnotation)
     */
    @Override
    public void store(final EAnnotation element) {
        // eAnnotation -> EAnnotation
        setMetaElement(element, EAnnotation.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.storage.neo.EMFDispatcher#store(org.eclipse.emf.ecore.EAttribute)
     */
    @Override
    public void store(final EAttribute element) {
        // eAttribute -> EAttribute
        setMetaElement(element, EAttribute.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.storage.neo.EMFDispatcher#store(org.eclipse.emf.ecore.EClass)
     */
    @Override
    public void store(final EClass element) {
        // set inheritance relationship.
        setSuperElements(element);

        // eClass -> EClass
        setMetaElement(element, EClass.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.storage.neo.EMFDispatcher#store(org.eclipse.emf.ecore.EDataType)
     */
    @Override
    public void store(final EDataType element) {
        // eDataType -> EDataType
        setMetaElement(element, EDataType.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.storage.neo.EMFDispatcher#store(org.eclipse.emf.ecore.EEnum)
     */
    @Override
    public void store(final EEnum element) {
        // eEnum -> EEnum
        setMetaElement(element, EEnum.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.storage.neo.EMFDispatcher#store(org.eclipse.emf.ecore.EEnumLiteral)
     */
    @Override
    public void store(final EEnumLiteral element) {
        // eEnumLiteral -> EEnumLiteral
        setMetaElement(element, EEnumLiteral.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.storage.neo.EMFDispatcher#store(org.eclipse.emf.ecore.EGenericType)
     */
    @Override
    public void store(final EGenericType element) {
        setMetaElement(element, EGenericType.class);

        for (final EGenericType typeArgument : element.getETypeArguments()) {
            getNodeFor(element).createRelationshipTo(getNodeFor(typeArgument), EcoreRelationshipType.GENERIC_TYPE_ARGUMENT);
        }
        setTypeElement(element);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.storage.neo.EMFDispatcher#store(org.eclipse.emf.ecore.EObject)
     */
    @Override
    public void store(final EObject element) {
        final Node eObjectNode = getNodeFor(element);

        // link to class
        final Node classNode = findClassifierNode(element.eClass());
        classNode.createRelationshipTo(eObjectNode, EcoreRelationshipType.INSTANCE);

        // store attributes, TODO create new node per attribute or store as properties?
        for (final EAttribute attribute : element.eClass().getEAllAttributes()) {
            // skip transient attributes, they must not be persisted
            if (attribute.isTransient() || attribute.isDerived()) {
                continue;
            }
            final Object attributeValue = element.eGet(attribute);
            if (attributeValue != null) {
                final Node attributeMetaNode = findFeatureNode(attribute);
                if (attribute.isMany()) {
                    for (final Object singleValue : (EList<?>) attributeValue) {
                        final Node attributeNode = createNodeWithRelationship(eObjectNode, EcoreRelationshipType.CONTAINS, true);
                        attributeNode.setProperty(VALUE, singleValue);

                        attributeMetaNode.createRelationshipTo(attributeNode, EcoreRelationshipType.INSTANCE);
                    }
                } else {
                    final Node featureNode = createNodeWithRelationship(eObjectNode, EcoreRelationshipType.CONTAINS, true);
                    featureNode.setProperty(VALUE, attributeValue);

                    // set meta relationship
                    attributeMetaNode.createRelationshipTo(featureNode, EcoreRelationshipType.INSTANCE);
                }
            }
        }

        // set references
        for (final EReference eReference : element.eClass().getEAllReferences()) {
            if (eReference.isTransient() || eReference.isDerived()) {
                continue;
            }
            final Object referenceValue = element.eGet(eReference);
            if (referenceValue != null) {
                final Node eReferenceMetaNode = findFeatureNode(eReference);
                if (eReference.isMany()) {
                    for (final Object singleReference : (EList<?>) referenceValue) {
                        final Node referenceNode = createNodeWithRelationship(eObjectNode, EcoreRelationshipType.CONTAINS, true);
                        referenceNode.createRelationshipTo(getNodeFor((EObject) singleReference), eReference.isContainment() ? EcoreRelationshipType.REFERENCES_AS_CONTAINMENT : EcoreRelationshipType.REFERENCES);

                        // set meta relationship
                        eReferenceMetaNode.createRelationshipTo(referenceNode, EcoreRelationshipType.INSTANCE);
                    }
                } else {
                    final Node referenceNode = createNodeWithRelationship(eObjectNode, EcoreRelationshipType.CONTAINS, true);
                    referenceNode.createRelationshipTo(getNodeFor((EObject) referenceValue), eReference.isContainment() ? EcoreRelationshipType.REFERENCES_AS_CONTAINMENT : EcoreRelationshipType.REFERENCES);

                    // set meta relationship
                    eReferenceMetaNode.createRelationshipTo(referenceNode, EcoreRelationshipType.INSTANCE);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.storage.neo.EMFDispatcher#store(org.eclipse.emf.ecore.EOperation)
     */
    @Override
    public void store(final EOperation element) {
        // eOperation -> EOperation
        setMetaElement(element, EOperation.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.storage.neo.EMFDispatcher#store(org.eclipse.emf.ecore.EPackage)
     */
    @Override
    public void store(final EPackage element) {
        // relationships
        final EPackage container = element.getESuperPackage();
        if (null == container && !element.getNsURI().equals(EcorePackage.eNS_URI)) {
            // custom m2 model
            final Node metamodelNode = getModelNode(EcorePackage.eNS_URI);
            if (null == metamodelNode) {
                throw new IllegalStateException("First save the meta model with nsURI: " + EcorePackage.eNS_URI);
            }
            metamodelNode.createRelationshipTo(getNodeFor(element), EcoreRelationshipType.INSTANCE_MODEL);
        }

        // ePackage -> EPackage
        setMetaElement(element, EPackage.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.storage.neo.EMFDispatcher#store(org.eclipse.emf.ecore.EParameter)
     */
    @Override
    public void store(final EParameter element) {
        // eParameter -> EParameter
        setMetaElement(element, EParameter.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.storage.neo.EMFDispatcher#store(org.eclipse.emf.ecore.EReference)
     */
    @Override
    public void store(final EReference element) {
        // eReference -> EReference
        setMetaElement(element, EReference.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.infai.amor.backend.internal.storage.neo.EMFDispatcher#store(org.eclipse.emf.ecore.impl.EStringToStringMapEntryImpl)
     */
    @Override
    public void store(final EStringToStringMapEntryImpl element) {
        setMetaElement(element, EStringToStringMapEntryImpl.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.storage.neo.EMFDispatcher#store(org.eclipse.emf.ecore.ETypeParameter)
     */
    @Override
    public void store(final ETypeParameter element) {
        // eTypeParameter -> ETypeParameter
        setMetaElement(element, ETypeParameter.class);
    }
}
