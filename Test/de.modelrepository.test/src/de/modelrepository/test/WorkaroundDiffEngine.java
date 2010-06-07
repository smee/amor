/*******************************************************************************
 * Copyright (c) 2010 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package de.modelrepository.test;

import org.eclipse.emf.compare.diff.engine.GenericDiffEngine;
import org.eclipse.emf.compare.diff.metamodel.DiffGroup;
import org.eclipse.emf.compare.match.metamodel.Match2Elements;
import org.eclipse.emf.compare.match.metamodel.Match3Elements;

/**
 * @author sdienst
 *
 */
public class WorkaroundDiffEngine extends GenericDiffEngine {
    @Override
    protected void checkMoves(DiffGroup root, Match3Elements matchElement) {
    }

    protected void checkMoves(DiffGroup root, Match2Elements matchElement) {
    }
}
