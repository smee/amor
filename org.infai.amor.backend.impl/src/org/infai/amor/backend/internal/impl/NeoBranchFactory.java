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

import org.infai.amor.backend.CommitTransaction;
import org.infai.amor.backend.Revision;
import org.infai.amor.backend.internal.BranchFactory;
import org.infai.amor.backend.internal.NeoProvider;
import org.neo4j.api.core.Direction;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.Transaction;

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
    public NeoBranch createBranch(final Revision origin, final String name) {
        final Transaction tx = getNeo().beginTx();
        try {

            final Node factoryNode = getFactoryNode();
            NeoBranch result = null;
            if (origin == null) {
                // check if there already is any branch with the given name
                final NeoBranch branch = getBranch(name);
                if (branch != null) {
                    // yes, just return it
                    result = branch;
                } else {
                    // no, create a new main branch
                    final NeoBranch newBranch = new NeoBranch(getNeo().createNode(), name);
                    factoryNode.createRelationshipTo(newBranch.getNode(), NeoRelationshipType.getRelationshipType("branch"));
                    result = newBranch;
                }
            } else {
                // create a new subbranch
                final NeoBranch newBranch = new NeoBranch(getNeo().createNode(), name);
                newBranch.setOriginRevision((NeoRevision) origin);
                // set head revision to origin
                newBranch.getNode().createRelationshipTo(((NeoRevision) origin).getNode(), NeoRelationshipType.getRelationshipType(NeoBranch.HEADREVISION));

                factoryNode.createRelationshipTo(newBranch.getNode(), NeoRelationshipType.getRelationshipType("branch"));
                result = newBranch;
            }
            tx.success();
            return result;
        } finally {
            tx.finish();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.BranchFactory#createRevision(org.infai.amor.backend.Branch,
     * org.infai.amor.backend.CommitTransaction)
     */
    @Override
    public NeoRevision createRevision(final CommitTransaction transaction) {
        final NeoBranch neobranch = (NeoBranch) transaction.getBranch();
        final NeoRevision oldHeadRevision = (NeoRevision) transaction.getBranch().getHeadRevision();
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

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.BranchFactory#getBranch(java.lang.String)
     */
    @Override
    public NeoBranch getBranch(final String name) {
        final Transaction tx = getNeo().beginTx();
        try {
            NeoBranch result = null;
            final Iterable<Relationship> rs = getFactoryNode().getRelationships(NeoRelationshipType.getRelationshipType("branch"), Direction.OUTGOING);
            // iterate over all branches
            for (final NeoBranch branch : new NeoRelationshipIterable<NeoBranch>(rs) {
                @Override
                public NeoBranch narrow(final Relationship r) {
                    return new NeoBranch(r.getEndNode());
                }
            }) {
                if (branch.getName().equals(name)) {
                    result = branch;
                    break;
                }
            }
            tx.success();
            return result;
        } finally {
            tx.finish();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.BranchFactory#getBranches()
     */
    @Override
    public Iterable<NeoBranch> getBranches() {
        final Transaction tx = getNeo().beginTx();
        try {
            Iterable<NeoBranch> result = null;
            final Iterable<Relationship> rs = getFactoryNode().getRelationships(NeoRelationshipType.getRelationshipType("branch"));

            result = new NeoRelationshipIterable<NeoBranch>(rs) {
                @Override
                public NeoBranch narrow(final Relationship r) {
                    return new NeoBranch(r.getEndNode());
                }
            };
            tx.success();
            return result;
        } finally {
            tx.finish();
        }
    }

    /**
     * @return
     */
    private Node getFactoryNode() {
        return getFactoryNode(NeoRelationshipType.getRelationshipType("branches"));
    }
}
