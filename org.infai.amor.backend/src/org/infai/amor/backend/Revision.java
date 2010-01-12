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

import java.util.Collection;
import java.util.Date;

import org.eclipse.emf.common.util.URI;

/**
 * @author sdienst
 * 
 */
public interface Revision {
    enum ChangeType {
        ADDED, CHANGED, DELETED
    }
    /**
     * Commit message.
     * 
     * @return
     */
    String getCommitMessage();

    /**
     * Commit time, represents the point in time, where {@link Repository#commitTransaction(Transaction)} got called for this
     * revision.
     * 
     * @return
     */
    Date getCommitTimestamp();

    /**
     * Get a collection of uris for every touched model of this revision.
     * <p>
     * 
     * @return
     */
    Collection<URI> getModelReferences(ChangeType... ct);

    /**
     * Get the parent revision of this one.
     * 
     * @return a revision or null if this rev. is the first one
     */
    Revision getPreviousRevision();

    /**
     * Get the unique revision id.
     * 
     * @return
     */
    long getRevisionId();

    /**
     * Name of the user that created this revision.
     * 
     * @return
     */
    String getUser();
}
