/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend;

import java.util.Date;

/**
 * @author sdienst
 * 
 */
public interface Branch {
    /**
     * Find the most recent revision where the model addressed by <code>relativePath</code> was added/changed.
     * If there is no such revision, return null.
     * @param relativePath a relative path to a model as given by {@link Model#getPersistencePath()}
     * @return
     */
    Revision findRevisionOf(String relativePath);

    /**
     * Get creation time of this branch.
     * 
     * @return
     */
    Date getCreationTime();

    /**
     * Get the most recent revision of this branch. Might be null if this branch has no revisions yet.
     * 
     * @return
     */
    Revision getHeadRevision();

    /**
     * @return the name of this branch
     */
    String getName();

    /**
     * Get the revision this branch started at. Might be null if this branch is the main branch.
     * 
     * @return
     */
    Revision getOriginRevision();

    /**
     * @param extractRevision
     * @return
     */
    Revision getRevision(long revisionNumber);

    /**
     * Get all revisions of this branch, starting at {@link #getOriginRevision()} upto {@link #getHeadRevision()}.
     * 
     * @return
     */
    Iterable<? extends Revision> getRevisions();
}
