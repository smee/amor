/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.storage;

import java.io.IOException;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.xml.type.internal.DataValue.URI.MalformedURIException;
import org.infai.amor.backend.ChangedModel;
import org.infai.amor.backend.CommitTransaction;
import org.infai.amor.backend.Model;
import org.infai.amor.backend.Repository;
import org.infai.amor.backend.Response;
import org.infai.amor.backend.exception.TransactionListener;

/**
 * Pluggabe storage that knows how to (re)store models in a persistent fashion.
 * 
 * @author sdienst
 * 
 */
public interface Storage extends TransactionListener {

    /**
     * Checkin a changed model.
     * 
     * @param model
     *            a changed model
     * @param branch
     *            the branch to use
     * @param tr
     *            the current transaction
     * @return
     */
    Response checkin(ChangedModel model, CommitTransaction tr) throws IOException;

    /**
     * Add a {@link Model} that is new to the backend
     * 
     * @param model
     *            a model
     * @param branch
     *            the branch to use
     * @param tr
     *            the current transaction
     * @return information about success or error conditions
     * @throws IOException
     */
    Response checkin(Model model, CommitTransaction tr) throws IOException;

    /**
     * Restore a {@link Model} with the exact same contents given by the model referenced via the uri.
     * 
     * @param uri
     *            an amor uri specifying exactly one versioned model
     * @return a {@link Model}
     * @throws MalformedURIException
     *             for URIs that do not address a versioned model
     */
    Model checkout(URI uri) throws MalformedURIException;

    /**
     * @see Repository#view(URI)
     * @param uri
     * @return
     * @throws MalformedURIException
     */
    EObject view(URI uri) throws MalformedURIException;

}