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

import java.util.Collections;

import org.eclipse.emf.compare.diff.engine.GenericDiffEngine;
import org.eclipse.emf.compare.diff.metamodel.DiffGroup;
import org.eclipse.emf.compare.diff.metamodel.DiffModel;
import org.eclipse.emf.compare.epatch.Epatch;
import org.eclipse.emf.compare.epatch.diff.DiffEpatchService;
import org.eclipse.emf.compare.match.metamodel.*;
import org.eclipse.emf.compare.match.service.MatchService;
import org.eclipse.emf.ecore.EObject;

/**
 * @author sdienst
 *
 */
public class SampleEpatchCreator {
    /**
     * Workaround for bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=315738
     * 
     * @author sdienst
     * 
     */
    public static class WorkaroundDiffEngine extends GenericDiffEngine {
        @Override
        protected void checkMoves(DiffGroup root, Match3Elements matchElement) {
        }
        @Override
        protected void checkMoves(DiffGroup root, Match2Elements matchElement) {
        }
    }

    public static Epatch createEPatch(EObject modelVersion1, EObject modelVersion2) throws InterruptedException {
        MatchModel match = MatchService.doMatch(modelVersion1, modelVersion2, Collections.<String, Object> emptyMap());
        // DO NOT USE DiffService.doDiff(...), EPatch doesn't handle reference moves correctly
        // DiffModel diff = DiffService.doDiff(match, false);
        DiffModel diffModel = new WorkaroundDiffEngine().doDiff(match, false);
        return DiffEpatchService.createEpatch(match, diffModel, "some descriptive patch name");
    }
}
