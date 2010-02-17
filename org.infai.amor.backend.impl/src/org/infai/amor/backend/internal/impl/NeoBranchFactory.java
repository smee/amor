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
import org.infai.amor.backend.Revision;
import org.infai.amor.backend.internal.BranchFactory;
import org.infai.amor.backend.internal.NeoProvider;
import org.neo4j.graphdb.*;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;

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
                    final NeoBranch newBranch = new NeoBranch(getNeoProvider(), name);
                    factoryNode.createRelationshipTo(newBranch.getNode(), DynamicRelationshipType.withName("branch"));
                    result = newBranch;
                }
            } else {
                // create a new subbranch
                final NeoBranch newBranch = new NeoBranch(getNeoProvider(), name);
                newBranch.setOriginRevision((NeoRevision) origin);
                // set head revision to origin
                newBranch.setHeadRevisionTo((NeoRevision) origin);

                factoryNode.createRelationshipTo(newBranch.getNode(), DynamicRelationshipType.withName("branch"));
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
    public NeoRevision createRevision(final Branch origin, final long revisionId) {
        final NeoBranch neobranch = (NeoBranch) origin;
        final NeoRevision oldHeadRevision = neobranch.getHeadRevision();
        final NeoRevision newRevision = new NeoRevision(getNeoProvider(), revisionId, oldHeadRevision);

        // set the new head revision of this branch
        neobranch.setHeadRevisionTo(newRevision);
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
            final Iterable<NeoBranch> branches = getNeoBranches();
            for (final NeoBranch branch : branches) {
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
            final Iterable<NeoBranch> result = getNeoBranches();
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
        return getFactoryNode(DynamicRelationshipType.withName("branches"));
    }

    /**
     * @return
     */
    private Iterable<NeoBranch> getNeoBranches() {
        final Iterable<Relationship> rs = getFactoryNode().getRelationships(DynamicRelationshipType.withName("branch"));
        // iterate over all branches

        final Iterable<NeoBranch> branches = Iterables.transform(rs, new Function<Relationship, NeoBranch>() {
            @Override
            public NeoBranch apply(final Relationship r) {
                return new NeoBranch(getNeoProvider(), r.getEndNode());
            }
        });
        return branches;
    }
}
