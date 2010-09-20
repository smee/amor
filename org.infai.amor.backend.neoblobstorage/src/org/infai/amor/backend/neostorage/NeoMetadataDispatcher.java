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

import java.util.logging.Logger;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.impl.EStringToStringMapEntryImpl;
import org.infai.amor.backend.internal.NeoProvider;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.Traverser.Order;

/**
 * Import references to other models, meta classes etc.
 */
public class NeoMetadataDispatcher extends AbstractNeoDispatcher {
    private static Logger logger = Logger.getLogger(NeoMetadataDispatcher.class.getName());
    /**
     * @param neo
     */
    public NeoMetadataDispatcher(final NeoProvider neo) {
        super(neo);
    }

    /**
     * @param from
     * @param to
     */
    private void addDependency(final Node from, final Node to) {
        // find toplevel container
        final Node fromContainer = findToplevelContainer(from);


        // find toplevel typecontainer
        Node toContainer = findToplevelContainer(to);

        if (!fromContainer.equals(toContainer)) {
            if(toContainer.hasProperty("proxyUri")){
                final String proxyUri = (String) toContainer.getProperty("proxyUri");
                // find node for proxies
                // TODO create versioned node that references most recent model according to the checkout revision
                final Node tempNode = findEPackageByFilename(proxyUri.substring(0, proxyUri.indexOf('#')));
                if (tempNode != null) {
                    toContainer = tempNode;
                }
            }
            boolean foundDependency = false;
            for (final Relationship rel : fromContainer.getRelationships(EcoreRelationshipType.DEPENDS, Direction.OUTGOING)) {
                if (rel.getEndNode() == toContainer) {
                    foundDependency = true;
                    break;
                }
            }
            if (!foundDependency) {
                fromContainer.createRelationshipTo(toContainer, EcoreRelationshipType.DEPENDS);
            }
        }
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
                        cache(feature, aNode);
                        return aNode;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Create the transitive enclosure via traversing incoming relationships of type {@link EcoreRelationshipType#CONTAINS} to
     * find the topmost container of the given node.
     * 
     * @param node
     * @return
     */
    private Node findToplevelContainer(final Node node) {
        Node container = node;
        while (container.hasRelationship(EcoreRelationshipType.CONTAINS, Direction.INCOMING)) {
            container = container.getSingleRelationship(EcoreRelationshipType.CONTAINS, Direction.INCOMING).getStartNode();
        }
        return container;
    }

    /**
     * Set meta element.
     * 
     * @param element
     *            Element to assign with its meta model element
     * @param clazz
     *            Ecore class of meta model element
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
     */
    private void setTypeElement(final EGenericType element) {
        // set EType
        final EClassifier eType = element.getEClassifier();
        if (eType != null) {
            final Node eTypeNode = findClassifierNode(eType);
            if (null == eTypeNode) {
                throw new IllegalStateException("The type element " + eType + " could not be found!");
            }
            addDependency(getNodeFor(element), eTypeNode);

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
        logger.finest("storing eclass " + element.getName());
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
        logger.finest("(2)storing object of type " + element.eClass().getName());

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
                logger.finest(String.format("      storing attr '%s'='%s'", attribute.getName(), attributeValue));
                final Node attributeMetaNode = findFeatureNode(attribute);
                if (attribute.isMany()) {
                    for (final Object singleValue : (EList<?>) attributeValue) {
                        final Node attributeNode = createNodeWithRelationship(eObjectNode, EcoreRelationshipType.CONTAINS, true);
                        attributeNode.setProperty(VALUE, singleValue);

                        attributeMetaNode.createRelationshipTo(attributeNode, EcoreRelationshipType.INSTANCE);
                    }
                } else {
                    final Node featureNode = createNodeWithRelationship(eObjectNode, EcoreRelationshipType.CONTAINS, true);
                    set(featureNode, VALUE, attributeValue);

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
            final boolean isContainment = eReference.isContainment();
            final RelationshipType relType = isContainment ? EcoreRelationshipType.REFERENCES_AS_CONTAINMENT : EcoreRelationshipType.REFERENCES;
            // TODO find the package the referenced class is contained in to add a dependency to this package
            final Object referenceValue = element.eGet(eReference);
            if (referenceValue != null) {
                logger.finest(String.format("      storing ref '%s'='%s'", eReference.getName(), referenceValue));
                final Node eReferenceMetaNode = findFeatureNode(eReference);
                if (eReference.isMany()) {
                    for (final Object singleReference : (EList<?>) referenceValue) {
                        final Node referenceNode = createNodeWithRelationship(eObjectNode, EcoreRelationshipType.CONTAINS, true);
                        referenceNode.createRelationshipTo(getNodeFor((EObject) singleReference), relType);

                        // set meta relationship
                        eReferenceMetaNode.createRelationshipTo(referenceNode, EcoreRelationshipType.INSTANCE);
                    }
                } else {
                    final Node referenceNode = createNodeWithRelationship(eObjectNode, EcoreRelationshipType.CONTAINS, true);
                    referenceNode.createRelationshipTo(getNodeFor((EObject) referenceValue), relType);

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
        final EList<EClassifier> exceptions = element.getEExceptions();
        for (final EClassifier exception : exceptions) {
            getNodeFor(element).createRelationshipTo(getNodeFor(exception), EcoreRelationshipType.EXCEPTION);
        }
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
        logger.finest("storing epackage " + element.getName());
        // relationships
        final EPackage container = element.getESuperPackage();
        if (null == container && !element.getNsURI().equals(EcorePackage.eNS_URI)) {
            // custom m2 model
            final Node metamodelNode = findEPackageByNamespaceUri(EcorePackage.eNS_URI);
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
    public void store(final EReference reference) {
        // eReference -> EReference
        setMetaElement(reference, EReference.class);

        if (!reference.isContainment()) {
            addDependency(getNodeFor(reference), getNodeFor(reference.getEReferenceType()));
        }
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
