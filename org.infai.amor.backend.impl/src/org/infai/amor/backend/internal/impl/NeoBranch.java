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

import org.infai.amor.backend.Branch;
import org.infai.amor.backend.Revision;
import org.neo4j.api.core.Node;

/**
 * @author sdienst
 * 
 */
public class NeoBranch extends NeoObject implements Branch {

    private static final String BRANCHNAME = "branchname";
    private static final String CREATIONDATE = "creationdate";

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

    /**
     * Constructor for branches loaded from neo.
     * 
     * @param node
     */
    public NeoBranch(final Node node) {
        super(node);
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
        throw new UnsupportedOperationException();
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
     * @see org.infai.amor.backend.Branch#getRevision(long)
     */
    @Override
    public Revision getRevision(final long revisionNumber) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Branch#getOriginRevision()
     */
    @Override
    public Revision getOriginRevision() {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Branch#getRevisions()
     */
    @Override
    public Iterable<Revision> getRevisions() {
        throw new UnsupportedOperationException();
    }

}
