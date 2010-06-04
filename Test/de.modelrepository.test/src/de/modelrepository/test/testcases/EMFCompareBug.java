/*******************************************************************************
 * Copyright (c) 2010 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package de.modelrepository.test.testcases;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.diff.metamodel.DiffModel;
import org.eclipse.emf.compare.diff.service.DiffService;
import org.eclipse.emf.compare.epatch.Epatch;
import org.eclipse.emf.compare.epatch.diff.DiffEpatchService;
import org.eclipse.emf.compare.match.metamodel.MatchModel;
import org.eclipse.emf.compare.match.service.MatchService;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.Test;

/**
 * @author sdienst
 *
 */
public class EMFCompareBug {

    // /**
    // * @param rs
    // * @return
    // */
    // private List<Resource> extractParsedClasses(ResourceSet rs) {
    // List<Resource> result = new ArrayList<Resource>();
    // for(Resource res:rs.getResources()) {
    // if("file".equals(res.getURI().scheme())) {
    // result.add(res);
    // }
    // }
    // return result;
    // }
    //
    // @Test
    // @Ignore
    // public void shouldSerializeModels() throws Exception {
    // // given
    // JavaToEMFParser parser = new JavaToEMFParser();
    // // when
    // parser.parseAndSerializeAllJavaFiles(new File("res/in/emfcomparebug"), new Vector<File>(), new
    // File("res/out/emfcomparebug"));
    // }
    // @Test
    // @Ignore
    // public void shouldBeAbleToCreateEpatch() throws Exception {
    // // given
    // JavaToEMFParser parser = new JavaToEMFParser();
    // // when
    // ResourceSet rs = parser.parseAllJavaFiles(new File("res/in/emfcomparebug"), new Vector<File>());
    // List<Resource> resources = extractParsedClasses(rs);
    //
    // ModelComparator comparator = new ModelComparator();
    // DiffModel diffModel = comparator.compare(resources.get(0).getContents().get(0), resources.get(1).getContents().get(0));
    // ModelUtil.describeDiff(diffModel.getOwnedElements(), 0);
    // // then this should succeed without throwing an exception
    // Epatch epatch = comparator.getEpatch();
    // }

    @Test
    public void shouldCreateEpatchForMovedElement() throws Exception {
        // given
        ResourceSet rs = new ResourceSetImpl();
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
        // ...loaded metamodel
        Resource meta = rs.getResource(URI.createFileURI("res/in/movebug/MoveBug.ecore"), true);
        meta.load(null);

        // register package nsuri
        EPackage pckage = (EPackage) meta.getContents().get(0);
        rs.getPackageRegistry().put(pckage.getNsURI(), pckage);
        // ...loaded models
        Resource originModel = rs.getResource(URI.createFileURI("res/in/movebug/Original.xmi"), true);
        originModel.load(null);
        Resource changedModel = rs.getResource(URI.createFileURI("res/in/movebug/Moved.xmi"), true);
        changedModel.load(null);

        // when
        MatchModel matchModel = MatchService.doResourceMatch(originModel, changedModel, null);
        DiffModel diffModel = DiffService.doDiff(matchModel);
        // then
        // ... epatch creation fails because of move between different containers
        Epatch epatch = DiffEpatchService.createEpatch(matchModel, diffModel, "buggyPatch");
    }
}
