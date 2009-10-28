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
import org.infai.amor.backend.internal.ModelLocation;
import org.infai.amor.backend.internal.NeoProvider;
import org.infai.amor.backend.internal.storage.neo.NeoModelLocation;
import org.neo4j.api.core.Direction;
import org.neo4j.api.core.DynamicRelationshipType;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;

import com.google.common.collect.Iterables;
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
    private static final String MODELLOCATION = "modellocation";

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
                    return new NeoModelLocation(locRel.getEndNode());
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
    public Collection<URI> getModelReferences(final ChangeType ct) {
        dumpOutRels(getNode());
        final Collection<URI> modelUris = Lists.newArrayList();
        for (final Relationship rel : getNode().getRelationships(DynamicRelationshipType.withName(MODELLOCATION + ct.name()), Direction.OUTGOING)) {
            final Node n = rel.getEndNode();
            dumpOutRels(n);
            for (final Relationship rel2 : n.getRelationships(DynamicRelationshipType.withName(MODELLOCATION), Direction.OUTGOING)) {
                final ModelLocation mloc = new NeoModelLocation(rel2.getEndNode());
                modelUris.add(mloc.getExternalUri());
            }
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

    private void rememberModelAction(ModelLocation loc, final ChangeType ct) {
        if (!(loc instanceof NeoModelLocation)) {
            loc = new NeoModelLocation(getNeoProvider(), createNode(), loc);
        }
        final Node node = getModelChangeNode(ct);
        node.createRelationshipTo(((NeoObject) loc).getNode(), DynamicRelationshipType.withName(MODELLOCATION));
        dumpOutRels(getNode());
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

    /**
     * For internal usage only, not part of the external interface! Add references to added model nodes.
     * 
     * @param loc
     */
    public void touchedModel(final ModelLocation loc) {
        rememberModelAction(loc, loc.getChangeType());
    }

}
