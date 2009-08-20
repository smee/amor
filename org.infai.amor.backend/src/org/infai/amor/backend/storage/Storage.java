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

import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.infai.amor.backend.ChangedModel;
import org.infai.amor.backend.CommitTransaction;
import org.infai.amor.backend.Model;
import org.infai.amor.backend.Repository;
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
     */
    void checkin(ChangedModel model, CommitTransaction tr) throws IOException;

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
    void checkin(Model model, CommitTransaction tr) throws IOException;

    /**
     * Restore a {@link Model} with the exact same contents given by the model referenced via the path. The path parameter
     * describes the same relative model location that was used for the former checkin.
     * 
     * @param path
     *            a model specific relative path
     * @param revisionId
     *            id of the revision to check out
     * @return a {@link Model}
     * @throws IOException
     *             for every internal reading errors
     */
    Model checkout(IPath path, long revisionId) throws IOException;

    /**
     * @see Repository#view(URI)
     * @param path
     * @param revisionId
     * @return
     * @throws IOException
     */
    EObject view(IPath path, long revisionId) throws IOException;

}
