/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

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
import org.eclipse.emf.compare.match.metamodel.MatchModel;
import org.eclipse.emf.compare.match.service.MatchService;
import org.eclipse.emf.compare.util.ModelUtils;
import org.eclipse.emf.ecore.EObject;
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

    }

    /**
     * @param content
     * @param content2
     */
    public static void assertModelEqual(final EObject orig, final EObject changed) {
        try {
            final MatchModel match = MatchService.doMatch(orig, changed, Collections.<String, Object> emptyMap());
            final DiffModel diff = DiffService.doDiff(match, false);

            //            System.out.println("Merging difference to args[1].\n"); //$NON-NLS-1$
            final List<DiffElement> differences = new ArrayList<DiffElement>(diff.getOwnedElements());
            // saveDiff(diff, match);
            // describeDiff(differences, 0);
            // FIXME if it's equals there is still an empty change, bug in emfcompare?
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
     * @param string
     * @return
     * @throws IOException
     */
    public static EObject readInputModel(final String relativePath) throws IOException {
        final ResourceSet rs = new ResourceSetImpl();

        final Resource resource = rs.createResource(URI.createFileURI(new File(relativePath).getAbsolutePath()));
        resource.load(null);
        return resource.getContents().get(0);
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

}
