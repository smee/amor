/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.diff.metamodel.AttributeChange;
import org.eclipse.emf.compare.diff.metamodel.ComparisonResourceSnapshot;
import org.eclipse.emf.compare.diff.metamodel.DiffElement;
import org.eclipse.emf.compare.diff.metamodel.DiffFactory;
import org.eclipse.emf.compare.diff.metamodel.DiffModel;
import org.eclipse.emf.compare.diff.metamodel.DifferenceKind;
import org.eclipse.emf.compare.diff.metamodel.ModelElementChangeLeftTarget;
import org.eclipse.emf.compare.diff.metamodel.ModelElementChangeRightTarget;
import org.eclipse.emf.compare.diff.service.DiffService;
import org.eclipse.emf.compare.epatch.Epatch;
import org.eclipse.emf.compare.epatch.diff.DiffEpatchService;
import org.eclipse.emf.compare.match.MatchOptions;
import org.eclipse.emf.compare.match.metamodel.MatchModel;
import org.eclipse.emf.compare.match.service.MatchService;
import org.eclipse.emf.compare.util.ModelUtils;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceFactoryImpl;

/**
 * Just some static helper methods for model io, asserts etc.
 * 
 * @author sdienst
 * 
 */
public class ModelUtil {
    static {
        // init persistence mappings for ecore and xmi
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xml", new XMLResourceFactoryImpl());
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("filesystem", new XMIResourceFactoryImpl());

    }

    /**
     * @param content
     * @param content2
     */
    public static void assertModelEqual(final EObject orig, final EObject changed) {
        try {
            // we assume the very same metamodel, no matter where it was loaded from
            final Map<String, Object> options = new HashMap<String, Object>();
            options.put(MatchOptions.OPTION_DISTINCT_METAMODELS, true);
            final MatchModel match = MatchService.doMatch(orig, changed, options);
            final DiffModel diff = DiffService.doDiff(match, false);

            final List<DiffElement> differences = new ArrayList<DiffElement>(diff.getOwnedElements());
            // saveDiff(diff, match);
            // describeDiff(differences, 0);
            // if there are no differences, there is still an empty change, bug in emfcompare?
            assertTrue(differences.isEmpty() || differences.get(0).getSubDiffElements().isEmpty());
        } catch (final InterruptedException e) {
            fail();
        }

    }

    /**
     * @param de
     * @return
     */
    public static String createDescriptionOf(final DiffElement de) {
        final DifferenceKind kind = de.getKind();
        if (de instanceof ModelElementChangeLeftTarget) {
            return kind + ": " + ((ModelElementChangeLeftTarget) de).getLeftElement().toString();
        } else if (de instanceof ModelElementChangeRightTarget) {
            return kind + ": " + ((ModelElementChangeRightTarget) de).getRightElement().toString();
        } else if (de instanceof AttributeChange) {
            return kind + ": " + ((AttributeChange) de).getLeftElement().toString() + " -> " + ((AttributeChange) de).getRightElement().toString();
        } else {
            return kind.toString();
        }
    }

    /**
     * @param origModel
     * @param changedModel
     * @return
     * @throws InterruptedException
     */
    public static Epatch createEpatch(final EObject origModel, final EObject changedModel) throws InterruptedException{
        final MatchModel match = MatchService.doMatch(origModel, changedModel, null);
        final DiffModel diff = DiffService.doDiff(match, false);
        return DiffEpatchService.createEpatch(match, diff, "testpatch");
    }

    /**
     * @param differences
     */
    public static void describeDiff(final List<DiffElement> differences, final int depth) {
        for (final DiffElement de : differences) {
            System.out.print(StringUtils.repeat(" ", depth));
            final String diffDescription = ModelUtil.createDescriptionOf(de);
            System.out.println(diffDescription);
            final EList<DiffElement> subDiffs = de.getSubDiffElements();
            if (!subDiffs.isEmpty()) {
                describeDiff(subDiffs, depth + 1);
            }
        }
    }

    /**
     * @param relativePath
     * @return
     * @throws IOException
     */
    public static EObject readInputModel(final String relativePath) throws IOException {
        return readInputModel(relativePath, new ResourceSetImpl());
    }

    /**
     * @param string
     * @return
     * @throws IOException
     */
    public static EObject readInputModel(final String relativePath, final ResourceSet rs) throws IOException {
        return readInputModels(relativePath, rs).get(0);
    }

    /**
     * @param relativePath
     * @return
     * @throws IOException
     */
    public static List<EObject> readInputModels(final String relativePath) throws IOException {
        return readInputModels(relativePath, new ResourceSetImpl());
    }

    /**
     * @param string
     * @return
     * @throws IOException
     */
    public static List<EObject> readInputModels(final String relativePath, final ResourceSet rs) throws IOException {

        String file = ModelUtil.class.getClassLoader().getResource(relativePath).toExternalForm();
        file = file.substring("file:/".length());

        final Resource resource = rs.getResource(URI.createFileURI(file), true);
        resource.load(null);

        // register packages
        for (final EObject eObject : resource.getContents()) {
            if (eObject instanceof EPackage && !((EPackage) eObject).getNsURI().equals(EcorePackage.eNS_URI)) {
                rs.getPackageRegistry().put(((EPackage) eObject).getNsURI(), eObject);
            }
        }
        return resource.getContents();
    }

    /**
     * @param diff
     * @param match
     */
    public static void saveDiff(final DiffModel diff, final MatchModel match) {
        final ComparisonResourceSnapshot snapshot = DiffFactory.eINSTANCE.createComparisonResourceSnapshot();
        snapshot.setDate(Calendar.getInstance().getTime());
        snapshot.setMatch(match);
        snapshot.setDiff(diff);
        try {
            ModelUtils.save(snapshot, "result.emfdiff");
        } catch (final IOException e) {
            e.printStackTrace();
        } //$NON-NLS-1$

    }

    /**
     * @param input
     * @throws IOException
     */
    public static void storeViaXml(final EObject... input) throws IOException {
        final ResourceSetImpl rs = new ResourceSetImpl();
        for (final EObject eo : input) {
            final Resource res = rs.createResource(URI.createFileURI("foo/" + eo.hashCode() + ".xml"));
            res.getContents().add(eo);
            res.save(null);
        }
    }

}
