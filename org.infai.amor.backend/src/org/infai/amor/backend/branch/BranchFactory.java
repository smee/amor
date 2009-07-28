/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.branch;

import org.infai.amor.backend.Branch;

/**
 * @author sdienst
 * 
 */
public interface BranchFactory {
    /**
     * Create a (sub)branch with the provided name. If parent==null, creates a new main branch.
     * 
     * @param parent
     *            parent branch or null
     * @param name
     *            name of the new branch
     * @return the new branch
     */
    Branch createBranch(Branch parent, String name);

    /**
     * Find the branch with the given name.
     * 
     * @param name
     *            name of the branch. Must not be null
     * @return the specified branch
     */
    Branch getBranch(String name);

    // /**
    // * Merge branch source into branch target.
    // *
    // * @param target
    // * the target branch of this merge
    // * @param source
    // * the branch that contains the changes that shall be merged TODO what to return?
    // */
    // void mergeInto(Branch target, Branch source);
}
