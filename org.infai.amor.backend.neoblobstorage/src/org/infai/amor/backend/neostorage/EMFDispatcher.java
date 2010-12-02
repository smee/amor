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

import java.io.IOException;

import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.impl.EStringToStringMapEntryImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.infai.amor.backend.Model;
import org.infai.amor.backend.Revision;
import org.infai.amor.backend.internal.impl.NeoModelLocation;

/**
 * Handle the different emf elements. Kind of dynamic dispatch by type.
 */
public interface EMFDispatcher {
    /**
     * Dispatch an element to the most specific method handling this type. If only java had multi methods...
     * 
     * 
     * @throws IOException
     *             if the model could not be saved
     */
    void dispatch(EObject element) throws IOException;

    /**
     * Store an <code>EAnnotation</code>.
     * 
     * @param element
     *            <code>EAnnotation</code>
     */
    void store(EAnnotation element);

    /**
     * Store an <code>EAttribute</code>.
     * 
     * @param element
     *            <code>EAttribute</code>
     */
    void store(EAttribute element);

    /**
     * Store an <code>EClass</code>.
     * 
     * @param element
     *            <code>EClass</code>
     */
    void store(EClass element);

    /**
     * Store an <code>EDataType</code>.
     * 
     * @param element
     *            <code>EDataType</code>
     */
    void store(EDataType element);

    /**
     * Store an <code>EEnum</code>.
     * 
     * @param element
     *            <code>EEnum</code>
     */
    void store(EEnum element);

    /**
     * Store an <code>EEnumLiteral</code>.
     * 
     * @param element
     *            <code>EEnumLiteral</code>
     */
    void store(EEnumLiteral element);

    /**
     * Store an <code>EGenericType</code>.
     * <p>
     * 
     * @param element
     *            <code>EGenericType</code>
     */
    void store(EGenericType element);

    /**
     * Store an element of a M1 model.
     * 
     * @param element
     */
    void store(EObject element);

    /**
     * Store an <code>EOperation</code>.
     * 
     * @param element
     *            <code>EOperation</code>
     */
    void store(EOperation element);

    /**
     * Store an <code>EPackage</code>.
     * 
     * @param element
     *            <code>EPackage</code>
     */
    void store(EPackage element);

    /**
     * Store an <code>EParameter</code>.
     * <p>
     * {@link EParameter}s are ordered!
     * 
     * @param element
     *            <code>EParameter</code>
     */
    void store(EParameter element);

    /**
     * Store an <code>EReference</code>.
     * 
     * @param element
     *            <code>EReference</code>
     */
    void store(EReference element);

    /**
     * Store an <code>EStringToStringMapEntryImpl</code>.
     * 
     * @param element
     *            <code>EStringToStringMapEntryImpl</code>
     */
    void store(EStringToStringMapEntryImpl element);

    /**
     * Store an <code>ETypeParameter</code> (generic type parameters).
     * <p>
     * 
     * @param element
     *            <code>ETypeParameter</code>
     */
    void store(ETypeParameter element);

    /**
     * Store a {@link Model}.
     * @param model
     */
    NeoModelLocation store(Model model, Revision rev);

    /**
     * Store a {@link Resource}.
     * 
     * @param resource
     *            Resource to store
     * @deprecated
     */
    @Deprecated
    void store(Resource resource);
}
