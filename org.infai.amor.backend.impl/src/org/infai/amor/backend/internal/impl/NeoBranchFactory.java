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

import org.infai.amor.backend.Branch;
import org.infai.amor.backend.CommitTransaction;
import org.infai.amor.backend.Revision;
import org.infai.amor.backend.internal.BranchFactory;
import org.infai.amor.backend.internal.NeoProvider;
import org.neo4j.api.core.Direction;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;

/**
 * BranchFactory based on a neo4j graph database. Assumes that there is no running transaction yet, creates a new one for every
 * call of {@link BranchFactory} methods.
 * 
 * @author sdienst
 * 
 */
public class NeoBranchFactory extends NeoObjectFactory implements BranchFactory {

    /**
     * @param neo
     */
    public NeoBranchFactory(final NeoProvider neo) {
        super(neo);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.BranchFactory#createBranch(org.infai.amor.backend.Branch, java.lang.String)
     */
    @Override
    public Branch createBranch(final Revision origin, final String name) {
        final Node factoryNode = getFactoryNode();

        if (origin == null) {
            // check if there already is any branch with the given name
            final Branch branch = getBranch(name);
            if (branch != null) {
                // yes, just return it
                return branch;
            } else {
                // no, create a new main branch
                final NeoBranch newBranch = new NeoBranch(getNeo().createNode(), name);
                factoryNode.createRelationshipTo(newBranch.getNode(), NeoRelationshipType.getRelationshipType("branch"));
                return newBranch;
            }
        } else {
            // create a new subbranch
            final NeoBranch newBranch = new NeoBranch(getNeo().createNode(), name);
            newBranch.setOriginRevision((NeoRevision) origin);
            // set head revision to origin
            newBranch.getNode().createRelationshipTo(((NeoRevision) origin).getNode(), NeoRelationshipType.getRelationshipType(NeoBranch.HEADREVISION));

            factoryNode.createRelationshipTo(newBranch.getNode(), NeoRelationshipType.getRelationshipType("branch"));
            return newBranch;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.BranchFactory#createRevision(org.infai.amor.backend.Branch,
     * org.infai.amor.backend.CommitTransaction)
     */
    @Override
    public Revision createRevision(final Branch branch, final CommitTransaction transaction) {
        if (branch == null) {
            throw new IllegalArgumentException("Can't create a revision without a branch");
        } else {
            final NeoBranch neobranch = (NeoBranch) branch;
            final NeoRevision oldHeadRevision = (NeoRevision) branch.getHeadRevision();
            final NeoRevision newRevision = new NeoRevision(getNeo().createNode(), transaction.getRevisionId(), transaction.getCommitMessage(), oldHeadRevision);
            // is there a head revision of this branch?
            final Relationship oldHeadRel = neobranch.getNode().getSingleRelationship(NeoRelationshipType.getRelationshipType(NeoBranch.HEADREVISION), Direction.OUTGOING);
            if (oldHeadRel != null) {
                oldHeadRel.delete();
            }
            // set the new head revision of this branch
            neobranch.getNode().createRelationshipTo(newRevision.getNode(), NeoRelationshipType.getRelationshipType(NeoBranch.HEADREVISION));
            return newRevision;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.BranchFactory#getBranch(java.lang.String)
     */
    @Override
    public Branch getBranch(final String name) {
        final Iterable<Relationship> rs = getFactoryNode().getRelationships(NeoRelationshipType.getRelationshipType("branch"), Direction.OUTGOING);
        // iterate over all branches
        for (final NeoBranch branch : new NeoRelationshipIterable<NeoBranch>(rs) {
            @Override
            public NeoBranch narrow(final Relationship r) {
                return new NeoBranch(r.getEndNode());
            }
        }) {
            if (branch.getName().equals(name)) {
                return branch;
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.BranchFactory#getBranches()
     */
    @Override
    public Iterable<Branch> getBranches() {
        final Iterable<Relationship> rs = getFactoryNode().getRelationships(NeoRelationshipType.getRelationshipType("branch"));

        return new NeoRelationshipIterable<Branch>(rs) {
            @Override
            public NeoBranch narrow(final Relationship r) {
                return new NeoBranch(r.getEndNode());
            }
        };
    }

    /**
     * @return
     */
    private Node getFactoryNode() {
        return getFactoryNode(NeoRelationshipType.getRelationshipType("branches"));
    }
}
