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

import java.util.*;

import org.infai.amor.backend.Branch;
import org.infai.amor.backend.neo.NeoObject;
import org.infai.amor.backend.neo.NeoProvider;
import org.neo4j.graphdb.*;

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
    public NeoBranch(final NeoProvider np,final Node node) {
        super(np,node);
    }

    /**
     * Constructor for entirely new branches.
     * 
     * @param np
     * @param name
     */
    public NeoBranch(final NeoProvider np, final String name) {
        super(np);
        set(BRANCHNAME, name);
        set(CREATIONDATE, new Date().getTime());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Branch#getCreationTime()
     */
    @Override
    public Date getCreationTime() {
        return new Date((Long) get(CREATIONDATE));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Branch#getHeadRevision()
     */
    @Override
    public NeoRevision getHeadRevision() {
        final Relationship rel = getNode().getSingleRelationship(DynamicRelationshipType.withName(HEADREVISION), Direction.OUTGOING);
        if (rel == null) {
            return null;
        } else {
            return new NeoRevision(getNeoProvider(), rel.getEndNode());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Branch#getName()
     */
    @Override
    public String getName() {
        return (String) get(BRANCHNAME);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Branch#getOriginRevision()
     */
    @Override
    public NeoRevision getOriginRevision() {
        final Relationship rel = getNode().getSingleRelationship(DynamicRelationshipType.withName(STARTREVISION), Direction.OUTGOING);
        if (rel == null) {
            return null;
        } else {
            return new NeoRevision(getNeoProvider(), rel.getEndNode());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Branch#getRevision(long)
     */
    @Override
    public NeoRevision getRevision(final long revisionNumber) {
        for (final NeoRevision rev : getRevisions()) {
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
    public Iterable<NeoRevision> getRevisions() {
        return new Iterable<NeoRevision>() {
            NeoRevision nextRevision = getHeadRevision();
            final NeoRevision originRev = getOriginRevision();

            @Override
            public Iterator<NeoRevision> iterator() {
                return new Iterator<NeoRevision>() {

                    @Override
                    public boolean hasNext() {
                        return nextRevision != null;
                    }

                    @Override
                    public NeoRevision next() {
                        if (nextRevision == null) {
                            throw new NoSuchElementException();
                        }
                        final NeoRevision currentRevision = nextRevision;
                        nextRevision = nextRevision.getPreviousRevision();
                        // stop if we would reach the origin revision of this branch
                        if (nextRevision != null && originRev != null && originRev.getRevisionId() == currentRevision.getRevisionId()) {
                            nextRevision = null;
                        }
                        return currentRevision;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    /**
     * @param rev
     */
    protected void setHeadRevisionTo(final NeoRevision rev){
        final Relationship oldHeadRel = getNode().getSingleRelationship(DynamicRelationshipType.withName(NeoBranch.HEADREVISION), Direction.OUTGOING);
        if (oldHeadRel != null) {
            oldHeadRel.delete();
        }
        getNode().createRelationshipTo(rev.getNode(), DynamicRelationshipType.withName(NeoBranch.HEADREVISION));
    }

    public void setOriginRevision(final NeoRevision rev) {
        if (getOriginRevision() != null) {
            throw new IllegalStateException("Attempt to change origin revision of a branch");
        } else {
            getNode().createRelationshipTo(rev.getNode(), DynamicRelationshipType.withName(STARTREVISION));
        }
    }

}
