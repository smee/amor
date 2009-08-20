/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.internal.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.custommonkey.xmlunit.XMLAssert;
import org.eclipse.core.runtime.Path;
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
import org.infai.amor.backend.Branch;
import org.infai.amor.backend.CommitTransaction;
import org.infai.amor.backend.Model;
import org.infai.amor.backend.impl.CommitTransactionImpl;
import org.infai.amor.backend.internal.impl.ModelImpl;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * @author sdienst
 * 
 */
public class BlobStorageTest {

    /**
     * 
     */
    private static final String BRANCHNAME = "testBranch";
    private BlobStorage storage;
    private Mockery context;
    private File tempDir;

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
    private static String createDescriptionOf(final DiffElement de) {
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
    private static void describeDiff(final List<DiffElement> differences, final int depth) {
        for (final DiffElement de : differences) {
            System.out.print(StringUtils.repeat(" ", depth));
            final String diffDescription = createDescriptionOf(de);
            System.out.println(diffDescription);
            final EList<DiffElement> subDiffs = de.getSubDiffElements();
            if (!subDiffs.isEmpty()) {
                describeDiff(subDiffs, depth + 1);
            }
        }
    }

    /**
     * @param diff
     * @param match
     */
    private static void saveDiff(final DiffModel diff, final MatchModel match) {
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

    private CommitTransaction createTransaction(final String branchname, final long revisionId) {
        final Branch branch = context.mock(Branch.class);
        context.checking(new Expectations() {
            {
                allowing(branch).getName();
                will(returnValue(BRANCHNAME));
            }
        });
        return new CommitTransactionImpl(branch, 55, null);
    }

    /**
     * @param string
     * @return
     * @throws IOException
     */
    private EObject readInputModel(final String string) throws IOException {
        final ResourceSet rs = new ResourceSetImpl();

        final Resource resource = rs.createResource(URI.createFileURI(new File(string).getAbsolutePath()));
        resource.load(null);
        return resource.getContents().get(0);
    }

    @Before
    public void setUp() throws IOException {
        tempDir = File.createTempFile("storage", "temp");
        tempDir.delete();
        tempDir.mkdirs();

        storage = new BlobStorage(tempDir, BRANCHNAME);
        context = new Mockery();
        // init persistence mappings for ecore and xmi
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xml", new XMLResourceFactoryImpl());
    }

    @Test
    public void shouldEqualSavedAndRestoredModel() throws Exception {
        // given
        final Model m = new ModelImpl(readInputModel("testmodels/base.ecore"), "testmodels/base.ecore");
        final CommitTransaction tr = createTransaction(BRANCHNAME, 88);

        // when
        storage.startTransaction(tr);
        storage.checkin(m, tr);
        final Model checkedout = storage.checkout(m.getPersistencePath(), tr.getRevisionId());

        // then
        // TODO compare via emfcompare or xmlunit
        assertModelEqual(m.getContent(), checkedout.getContent());
    }

    @Test
    public void shouldSaveModelWithoutChanges() throws IOException, SAXException {
        // read model
        final Model m = new ModelImpl(readInputModel("testmodels/base.ecore"), "testmodels/base.ecore");
        // start commit transaction
        final CommitTransaction tr = createTransaction(BRANCHNAME, 55);
        storage.startTransaction(tr);
        // store it into branch testBranch and revision 55
        storage.checkin(m, tr);

        final File storedFile = new File(tempDir, "testBranch/55/testmodels/base.ecore");
        assertTrue(storedFile.exists());

        // compare both models as xml documents
        XMLAssert.assertXMLEqual(new BufferedReader(new FileReader("testmodels/base.ecore")), new BufferedReader(new FileReader(storedFile)));
    }

    @Test
    public void testCreatesLocalDirectories() {
        final CommitTransaction tr = createTransaction(BRANCHNAME, 55);
        final URI fileUri = storage.createStorageUriFor(new Path("testmodels/dummymodel.xmi"), tr.getRevisionId(), false);
        assertEquals(new File(tempDir, "testBranch/55/testmodels").toURI().toString(), fileUri.toString());
    }
}
