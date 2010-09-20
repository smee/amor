/*******************************************************************************
 * Copyright (c) 2010 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.merging;

import static org.junit.Assert.assertTrue;

import org.eclipse.emf.compare.diff.merge.service.MergeService;
import org.eclipse.emf.compare.diff.metamodel.DiffModel;
import org.eclipse.emf.compare.diff.service.DiffService;
import org.eclipse.emf.compare.match.internal.statistic.NameSimilarity;
import org.eclipse.emf.compare.match.metamodel.MatchModel;
import org.eclipse.emf.compare.match.service.MatchService;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.infai.amor.backend.resources.AmorResourceSetImpl;
import org.infai.amor.test.ModelUtil;
import org.junit.Test;

/**
 * @author sdienst 3way merge is buggy.... see https://bugs.eclipse.org/bugs/show_bug.cgi?id=296442
 */
@SuppressWarnings("restriction")
public class MergeTests {
    @Test
    public void shouldBeSimilar() throws Exception {
        assertTrue(NameSimilarity.nameSimilarityMetric("secondRenamed.txt", "secondrenamed.txt") > 0.9);
    }

    @Test
    public void shouldSignalMergeError() throws Exception {
        /*
         * MatchService seems to find a conflict for this simple scenario only if the name attribute hasn't changed too much. :(
         * Else the change gets matched as one deletion and one addition.
         */
        final ResourceSet rs = new AmorResourceSetImpl();
        // given
        ModelUtil.readInputModel("testmodels/filesystem.ecore", rs);
        final EObject parentModel = ModelUtil.readInputModel("testmodels/fs/conflicts/simplefilesystem_v1.filesystem", rs);
        final EObject changedModel1 = ModelUtil.readInputModel("testmodels/fs/conflicts/simplefilesystem_conflict_1.filesystem", rs);
        final EObject changedModel2 = ModelUtil.readInputModel("testmodels/fs/conflicts/simplefilesystem_conflict_2.filesystem", rs);
        // when
        final MatchModel matchModel = MatchService.doMatch(changedModel1, changedModel2, parentModel, null);
        final DiffModel diffModel = DiffService.doDiff(matchModel, true);
        // then
        ModelUtil.describeDiff(diffModel.getOwnedElements(), 0);
        MergeService.merge(diffModel.getOwnedElements(), false);
        System.out.println(ModelUtil.storeViaXml(changedModel2));
    }

}
