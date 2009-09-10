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
import java.util.logging.Logger;

import org.eclipse.emf.common.util.URI;
import org.infai.amor.backend.Revision;
import org.infai.amor.backend.internal.NeoProvider;
import org.infai.amor.backend.internal.storage.neo.ModelLocation;
import org.neo4j.api.core.Direction;
import org.neo4j.api.core.DynamicRelationshipType;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;

import com.google.common.collect.Lists;

/**
 * @author sdienst
 * 
 */
public class NeoRevision extends NeoObject implements Revision {
    private static Logger logger = Logger.getLogger(NeoRevision.class.getName());

    private static final String COMMITMESSAGE = "message";

    private static final String COMMITTIME = "commitTime";
    private static final String PREVIOUSREVISION = "previousRev";
    private static final String USER = "username";
    private static final String REVISIONID = "revId";
    private static final String MODEL = "model";

    public NeoRevision(final NeoProvider np, final long revisionId, final String commitMessage, final String username, final NeoRevision previousRevision) {
        super(np);
        getNode().setProperty(REVISIONID, revisionId);
        getNode().setProperty(COMMITMESSAGE, commitMessage);
        getNode().setProperty(COMMITTIME, System.currentTimeMillis());
        getNode().setProperty(USER, username);
        if (previousRevision != null) {
            getNode().createRelationshipTo(previousRevision.getNode(), DynamicRelationshipType.withName(PREVIOUSREVISION));
        }
    }

    /**
     * Constructor for reading infos from an existing node.
     * 
     * @param node
     */
    public NeoRevision(final Node node) {
        super(node);
    }

    /**
     * For internal usage only, not part of the external interface! Add references to added model nodes.
     * 
     * @param modelPath
     * 
     * @param uri
     * @param loc
     */
    public void addModel(final URI externaluri, final ModelLocation loc) {
        if (loc != null) {
            getNode().createRelationshipTo(loc.getNode(), DynamicRelationshipType.withName(MODEL));
        } else {
            logger.warning(String.format("Could not store reference to added model '%s' in revision %d, model got persisted outside of neo!", externaluri, getRevisionId()));
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

    /**
     * @param modelPath
     * @return
     */
    public ModelLocation getModelLocation(final String modelPath) {
        for (final Relationship rel : getNode().getRelationships(DynamicRelationshipType.withName(MODEL), Direction.OUTGOING)) {
            if (modelPath.equals(rel.getEndNode().getProperty(ModelLocation.RELATIVE_PATH))) {
                return new ModelLocation(rel.getEndNode());
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Revision#getModelReferences()
     */
    @Override
    public Collection<URI> getModelReferences() {
        final Collection<URI> modelUris = Lists.newArrayList();
        for (final Relationship rel : getNode().getRelationships(DynamicRelationshipType.withName(MODEL), Direction.OUTGOING)) {
            modelUris.add(new ModelLocation(rel.getEndNode()).getExternalUri());
        }
        return modelUris;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Revision#getPreviousRevision()
     */
    @Override
    public NeoRevision getPreviousRevision() {
        final Relationship rel = getNode().getSingleRelationship(DynamicRelationshipType.withName(PREVIOUSREVISION), Direction.OUTGOING);
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
     * @see org.infai.amor.backend.Revision#getUser()
     */
    @Override
    public String getUser() {
        return (String) getNode().getProperty(USER);
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

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "NeoRevision [commitMessage=" + getCommitMessage() + ", commitTimestamp=" + getCommitTimestamp() + ", revisionId=" + getRevisionId() + ", user=" + getUser() + "]";
    }

}
