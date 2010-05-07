/*******************************************************************************
 * Copyright (c) 2010 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.api;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;


/**
 * @author sdienst
 *
 */
public class RevisionInfo implements Serializable{
    /**
     * 
     */
    private static final long serialVersionUID = -6101899194332716076L;
    Collection<String> added, removed, changed;
    private final String username;
    private final String message;
    private final long timestamp;

    public RevisionInfo(String username, String message, long timestamp, Collection<String> added, Collection<String> changed, Collection<String> removed) {
        this.username = username;
        this.message = message;
        this.timestamp = timestamp;
        this.added = added;
        this.changed = changed;
        this.removed = removed;
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.api.RevisionInfo#getAddedModels()
     */
    public Collection<String> getAddedModels() {
        return Collections.unmodifiableCollection(added);
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.api.RevisionInfo#getChangeedModels()
     */
    public Collection<String> getChangedModels() {
        return Collections.unmodifiableCollection(changed);
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.api.RevisionInfo#getMessage()
     */
    public String getMessage() {
        return message;
    }


    /* (non-Javadoc)
     * @see org.infai.amor.backend.api.RevisionInfo#getRemovedModels()
     */
    public Collection<String> getRemovedModels() {
        return Collections.unmodifiableCollection(removed);
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.api.RevisionInfo#getTimestamp()
     */
    public long getTimestamp() {
        return timestamp;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.api.RevisionInfo#getUsername()
     */
    public String getUsername() {
        return username;
    }

}
