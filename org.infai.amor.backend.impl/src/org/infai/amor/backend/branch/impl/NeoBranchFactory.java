/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.branch.impl;

import org.infai.amor.backend.Branch;
import org.infai.amor.backend.branch.BranchFactory;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;

/**
 * @author sdienst
 * 
 */
public class NeoBranchFactory implements BranchFactory {
    /**
     *  
     */
    private enum BranchRelationShips implements RelationshipType {
        BRANCHES_REF, BRANCH
    }

    private final NeoService neo;

    public NeoBranchFactory(final NeoService service) {
        this.neo = service;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.branch.BranchFactory#createBranch(org.infai.amor.backend.Branch, java.lang.String)
     */
    @Override
    public Branch createBranch(final Branch parent, final String name) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.branch.BranchFactory#getBranch(java.lang.String)
     */
    @Override
    public Branch getBranch(final String name) {
        final Node referenceNode = neo.getReferenceNode();
        referenceNode.getr
        return null;
    }
}
