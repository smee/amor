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

import org.infai.amor.backend.*;
import org.infai.amor.backend.neo.NeoObject;
import org.infai.amor.backend.neo.NeoProvider;
import org.neo4j.graphdb.*;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * @author sdienst
 * 
 */
public class NeoRevision extends NeoObject implements InternalRevision {
    private static Logger logger = Logger.getLogger(NeoRevision.class.getName());

    private static final String COMMITMESSAGE = "message";

    private static final String COMMITTIME = "commitTime";
    private static final String PREVIOUSREVISION = "previousRev";
    private static final String USER = "username";
    private static final String REVISIONID = "revId";
    private static final String MODELLOCATION = "modellocation";

    public NeoRevision(final NeoProvider np, final long revisionId, final NeoRevision previousRevision) {
        super(np);
        getNode().setProperty(REVISIONID, revisionId);
        if (previousRevision != null) {
            getNode().createRelationshipTo(previousRevision.getNode(), DynamicRelationshipType.withName(PREVIOUSREVISION));
        }
    }

    /**
     * Constructor for reading infos from an existing node.
     * 
     * @param node
     */
    public NeoRevision(final NeoProvider np, final Node node) {
        super(np, node);
    }

    private void dumpOutRels(final Node n){
        for (final Relationship rel : n.getRelationships(Direction.OUTGOING)) {
            logger.fine(rel.getType().name());
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
     * Every neorevision has upto three nodes, each pointing to {@link ModelLocation}s for added, changed or deleted models
     * @param ct
     * @return
     */
    private Node getModelChangeNode(final ChangeType ct) {
        final Relationship rel = getNode().getSingleRelationship(DynamicRelationshipType.withName(MODELLOCATION+ct.name()), Direction.OUTGOING);
        if(rel == null){
            final Node node = createNode();
            getNode().createRelationshipTo(node, DynamicRelationshipType.withName(MODELLOCATION+ct.name()));
            return node;
        }else{
            return rel.getEndNode();
        }
    }

    /**
     * Get internal storage specific informations about this stored model instance.
     * 
     * @param modelPath
     * @return
     */
    public ModelLocation getModelLocation(final String modelPath){
        final Iterable<Relationship> rels = Iterables.concat(
                getNode().getRelationships(DynamicRelationshipType.withName(MODELLOCATION+ChangeType.ADDED.name()), Direction.OUTGOING),
                getNode().getRelationships(DynamicRelationshipType.withName(MODELLOCATION+ChangeType.CHANGED.name()), Direction.OUTGOING));

        for (final Relationship rel : rels) {
            final Node refNode = rel.getEndNode();
            for (final Relationship locRel : refNode.getRelationships(DynamicRelationshipType.withName(MODELLOCATION), Direction.OUTGOING)) {
                if (modelPath.equals(locRel.getEndNode().getProperty(NeoModelLocation.RELATIVE_PATH))) {
                    return new NeoModelLocation(getNeoProvider(), locRel.getEndNode());
                }
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
    public Collection<ModelLocation> getModelReferences(final ChangeType... ct) {
        // dumpOutRels(getNode());
        final Collection<ModelLocation> modelRefs = Lists.newArrayList();

        for (final ChangeType type : ct) {
            for (final Relationship rel : getNode().getRelationships(DynamicRelationshipType.withName(MODELLOCATION + type.name()), Direction.OUTGOING)) {
                final Node n = rel.getEndNode();
                // dumpOutRels(n);
                for (final Relationship rel2 : n.getRelationships(DynamicRelationshipType.withName(MODELLOCATION), Direction.OUTGOING)) {
                    final ModelLocation mloc = new NeoModelLocation(getNeoProvider(), rel2.getEndNode());
                    modelRefs.add(mloc);
                }
            }
        }
        return modelRefs;
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
            return new NeoRevision(getNeoProvider(), rel.getEndNode());
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

    private void rememberModelAction(ModelLocation loc, final ChangeType ct) {
        if (!(loc instanceof NeoObject)) {
            loc = new NeoModelLocation(getNeoProvider(), createNode(), loc);
        }
        final Node node = getModelChangeNode(ct);
        node.createRelationshipTo(((NeoObject) loc).getNode(), DynamicRelationshipType.withName(MODELLOCATION));
        // dumpOutRels(getNode());
    }

    /**
     * @param message
     */
    public void setCommitMessage(final String message) {
        getNode().setProperty(COMMITMESSAGE, message);
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.internal.InternalRevision#setTimestamp(long)
     */
    @Override
    public void setTimestamp(final long currentTimeMillis) {
        getNode().setProperty(COMMITTIME, currentTimeMillis);
    }

    /**
     * @param username
     */
    public void setUser(final String username) {
        getNode().setProperty(USER, username);
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

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.impl.InternalRevision#touchedModel(org.infai.amor.backend.internal.ModelLocation)
     */
    public void touchedModel(final ModelLocation loc) {
        rememberModelAction(loc, loc.getChangeType());
    }

}
