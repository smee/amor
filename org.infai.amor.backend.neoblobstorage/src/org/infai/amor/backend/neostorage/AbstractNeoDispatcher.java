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

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.impl.EStringToStringMapEntryImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.infai.amor.backend.Model;
import org.infai.amor.backend.internal.NeoProvider;
import org.infai.amor.backend.internal.impl.NeoModelLocation;
import org.neo4j.graphdb.*;

/**
 * Abstract implementation that iterates a model and calls the relevant {@link EMFDispatcher} methods for every type. This class
 * is not threadsafe!
 * 
 * @author sdienst
 * 
 */
public abstract class AbstractNeoDispatcher extends AbstractNeoPersistence implements EMFDispatcher {
    protected NeoModelLocation currentModelLocation;

    // TODO use org.eclipse.emf.ecore.util.EcoreSwitch
    /**
     * @param neo
     */
    public AbstractNeoDispatcher(final NeoProvider neo) {
        super(neo);
    }

    /**
     * Dispatch the current element to its handler.
     * <p>
     * The order of dispatch types dependends on type hierarchy. Special types <b>must</b> be checked earlier then generally
     * types.
     * <p>
     * 
     * @param element
     *            Element to dispatch
     * @param nodeCache
     *            Node cache
     */
    @Override
    public void dispatch(final EObject element) {
        // System.out.println(element.toString());
        if (element instanceof EPackage) {
            store((EPackage) element);
        } else if (element instanceof EClass) {
            store((EClass) element);
        } else if (element instanceof EAttribute) {
            store((EAttribute) element);
        } else if (element instanceof EOperation) {
            store((EOperation) element);
        } else if (element instanceof EReference) {
            store((EReference) element);
        } else if (element instanceof EEnum) {
            store((EEnum) element);
        } else if (element instanceof EEnumLiteral) {
            store((EEnumLiteral) element);
        } else if (element instanceof EDataType) {
            store((EDataType) element);
        } else if (element instanceof EGenericType) {
            store((EGenericType) element);
        } else if (element instanceof EParameter) {
            store((EParameter) element);
        } else if (element instanceof EAnnotation) {
            store((EAnnotation) element);
        } else if (element instanceof ETypeParameter) {
            store((ETypeParameter) element);
        } else if (element instanceof EStringToStringMapEntryImpl) {
            store((EStringToStringMapEntryImpl) element);
        } else {
            store(element);
        }
    }

    /**
     * Find the root node, that points to all contents of a {@link Model}.
     * 
     * @param model
     * @return
     */
    private Node findModelLocationNode(final Model model) {
        // TODO meh, might have several items in its econtents
        final Node node = getNodeFor(model.getContent().get(0));
        if (node != null) {
            final Relationship relationship = node.getSingleRelationship(EcoreRelationshipType.MODEL_CONTENT, Direction.INCOMING);
            if (relationship != null) {
                return relationship.getStartNode();
            }
        }
        return createNode();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.neostorage.EMFDispatcher#store(org.infai.amor.backend.Model)
     */
    @Override
    public NeoModelLocation store(final Model model) {
        final Node rootNode = findModelLocationNode(model);
        currentModelLocation = new NeoModelLocation(getNeoProvider(), rootNode);

        // System.out.println("using rootnode " + rootNode);
        for (final EObject eo : model.getContent()) {
            // provide the current resource uri for deresolving absolute proxy uris later on
            currentResourceUri = eo.eResource().getURI();
            dispatch(eo);
            // link from modellocation root node to this content
            rootNode.createRelationshipTo(getNodeFor(eo), EcoreRelationshipType.MODEL_CONTENT);

            for (final TreeIterator<EObject> it = eo.eAllContents(); it.hasNext();) {
                final EObject eoSub = it.next();
                // System.out.println("storing " + eoSub);
                dispatch(eoSub);
            }
        }
        return currentModelLocation;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.storage.neo.EMFDispatcher#store(org.eclipse.emf.ecore.resource.Resource)
     */
    @Override
    public void store(final Resource resource) {
        // traverse the model tree and dispatch by element type
        for (final TreeIterator<EObject> iter = resource.getAllContents(); iter.hasNext();) {
            dispatch(iter.next());
        }
    }
}
