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

import java.util.Date;
import java.util.Iterator;

import org.infai.amor.backend.Branch;
import org.infai.amor.backend.Revision;
import org.neo4j.api.core.Direction;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;

/**
 * @author sdienst
 * 
 */
public class NeoBranch extends NeoObject implements Branch {

    static final String BRANCHNAME = "branchname";
    static final String CREATIONDATE = "creationdate";
    static final String HEADREVISION = "mostRecentRevision";
    static final String STARTREVISION = "startRevision";

    /**
     * Constructor for branches loaded from neo.
     * 
     * @param node
     */
    public NeoBranch(final Node node) {
        super(node);
    }

    /**
     * Constructor for entirely new branches.
     * 
     * @param node
     * @param name
     */
    public NeoBranch(final Node node, final String name) {
        super(node);
        getNode().setProperty(BRANCHNAME, name);
        getNode().setProperty(CREATIONDATE, new Date().getTime());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Branch#getCreationTime()
     */
    @Override
    public Date getCreationTime() {
        return new Date((Long) getNode().getProperty(CREATIONDATE));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Branch#getHeadRevision()
     */
    @Override
    public Revision getHeadRevision() {
        final Relationship rel = getNode().getSingleRelationship(NeoRelationshipType.getRelationshipType(HEADREVISION), Direction.OUTGOING);
        if (rel == null) {
            return null;
        } else {
            return new NeoRevision(rel.getEndNode());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Branch#getName()
     */
    @Override
    public String getName() {
        return (String) getNode().getProperty(BRANCHNAME);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Branch#getOriginRevision()
     */
    @Override
    public Revision getOriginRevision() {
        final Relationship rel = getNode().getSingleRelationship(NeoRelationshipType.getRelationshipType(STARTREVISION), Direction.OUTGOING);
        if (rel == null) {
            return null;
        } else {
            return new NeoRevision(rel.getEndNode());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Branch#getRevision(long)
     */
    @Override
    public Revision getRevision(final long revisionNumber) {
        for (final Revision rev : getRevisions()) {
            if (rev.getRevisionId() == revisionNumber) {
                return rev;
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Branch#getRevisions()
     */
    @Override
    public Iterable<Revision> getRevisions() {
        return new Iterable<Revision>() {
            Revision currentRev = getHeadRevision();

            @Override
            public Iterator<Revision> iterator() {
                return new Iterator<Revision>() {

                    @Override
                    public boolean hasNext() {
                        return currentRev != null && currentRev.getPreviousRevision() != null;
                    }

                    @Override
                    public Revision next() {
                        final Revision result = currentRev;
                        if (currentRev != null) {
                            currentRev = currentRev.getPreviousRevision();
                        }
                        return result;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    public void setOriginRevision(final NeoRevision rev) {
        if (getOriginRevision() != null) {
            throw new IllegalStateException("Attempt to change origin revision of a branch");
        } else {
            getNode().createRelationshipTo(rev.getNode(), NeoRelationshipType.getRelationshipType(STARTREVISION));
        }
    }

}
