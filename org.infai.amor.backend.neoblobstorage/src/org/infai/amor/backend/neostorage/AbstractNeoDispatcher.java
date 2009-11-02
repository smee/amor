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
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
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
import org.eclipse.emf.ecore.resource.Resource;
import org.infai.amor.backend.internal.NeoProvider;

/**
 * Abstract implementation that iterates a model and calls the relevant {@link EMFDispatcher} methods for every type.
 * 
 * @author sdienst
 * 
 */
public abstract class AbstractNeoDispatcher extends AbstractNeoPersistence implements EMFDispatcher {

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
            // fallback, will this ever be reached?
            store(element);
        }
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
