/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.internal.impl;

import java.util.Collection;
import java.util.Date;

import org.eclipse.emf.common.util.URI;
import org.infai.amor.backend.Revision;
import org.neo4j.api.core.Direction;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;

/**
 * @author sdienst
 * 
 */
public class NeoRevision extends NeoObject implements Revision {

    private static final String COMMITMESSAGE = "message";
    private static final String COMMITTIME = "commitTime";
    private static final String PREVIOUSREVISION = "previousRev";
    private static final String USER = "username";
    private static final String REVISIONID = "revId";

    public NeoRevision(final Node node) {
        super(node);
    }

    public NeoRevision(final Node node, final long revisionId, final String commitMessage, final String username, final NeoRevision previousRevision) {
        super(node);
        getNode().setProperty(REVISIONID, revisionId);
        getNode().setProperty(COMMITMESSAGE, commitMessage);
        getNode().setProperty(COMMITTIME, System.currentTimeMillis());
        getNode().setProperty(USER, username);
        if (previousRevision != null) {
            getNode().createRelationshipTo(previousRevision.getNode(), NeoRelationshipType.getRelationshipType(PREVIOUSREVISION));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        if (getRevisionId() != ((NeoRevision) obj).getRevisionId()) {
            return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Revision#getCommitMessage()
     */
    @Override
    public String getCommitMessage() {
        return (String) getNode().getProperty(COMMITMESSAGE);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Revision#getCommitTimestamp()
     */
    @Override
    public Date getCommitTimestamp() {
        return new Date((Long) getNode().getProperty(COMMITTIME));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Revision#getModelReferences()
     */
    @Override
    public Collection<URI> getModelReferences() {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Revision#getPreviousRevision()
     */
    @Override
    public NeoRevision getPreviousRevision() {
        final Relationship rel = getNode().getSingleRelationship(NeoRelationshipType.getRelationshipType(PREVIOUSREVISION), Direction.OUTGOING);
        if (rel != null) {
            return new NeoRevision(rel.getEndNode());
        } else {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Revision#getRevisionId()
     */
    @Override
    public long getRevisionId() {
        return (Long) getNode().getProperty(REVISIONID);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        final long revisionId = getRevisionId();

        result = prime * result + (int) (revisionId ^ (revisionId >>> 32));
        return result;
    }

}
