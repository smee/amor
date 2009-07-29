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
    public Branch createBranch(final Branch parent, final String name) {
        final Transaction tx = getNeo().beginTx();
        try {
            return createBranchIntern(parent, name);
        } finally {
            tx.success();
            tx.finish();
        }

    }

    /**
     * - create a new branch
     * 
     * @param parent
     * @param name
     * @return
     */
    private Branch createBranchIntern(final Branch parent, final String name) {
        final Node factoryNode = getFactoryNode();

        if (parent == null) {
            // check if there already is any branch with the given name
            final Branch branch = getBranchIntern(name);
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
            final Node parentNode = ((NeoBranch) parent).getNode();

            parentNode.createRelationshipTo(newBranch.getNode(), NeoRelationshipType.getRelationshipType("subBranch"));
            // factoryNode.createRelationshipTo(newBranch.getNode(), NeoRelationshipType.getRelationshipType("branch"));
            return newBranch;
        }
    }

    /**
     * @return
     */
    private Node getFactoryNode() {
        return getFactoryNode(NeoRelationshipType.getRelationshipType("branches"));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.BranchFactory#getBranch(java.lang.String)
     */
    @Override
    public Branch getBranch(final String name) {
        final Transaction tx = getNeo().beginTx();
        try {
            return getBranchIntern(name);
        } finally {
            tx.success();
            tx.finish();
        }
    }

    /**
     * @param name
     * @return
     */
    private Branch getBranchIntern(final String name) {
        final Iterable<Relationship> rs = getFactoryNode().getRelationships(NeoRelationshipType.getRelationshipType("branch"), Direction.OUTGOING);
        // iterate over all main branches
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
        final Transaction tx = getNeo().beginTx();
        try {
            return getBranchesIntern();
        } finally {
            tx.success();
            tx.finish();
        }
    }

    /**
     * @return
     */
    private Iterable<Branch> getBranchesIntern() {
        final Iterable<Relationship> rs = getFactoryNode().getRelationships(NeoRelationshipType.getRelationshipType("branch"));

        return new NeoRelationshipIterable<Branch>(rs) {
            @Override
            public NeoBranch narrow(final Relationship r) {
                return new NeoBranch(r.getEndNode());
            }
        };
    }

}
