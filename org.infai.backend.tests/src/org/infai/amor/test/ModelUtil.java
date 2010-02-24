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

import java.io.*;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.diff.metamodel.*;
import org.eclipse.emf.compare.diff.service.DiffService;
import org.eclipse.emf.compare.epatch.Epatch;
import org.eclipse.emf.compare.epatch.diff.DiffEpatchService;
import org.eclipse.emf.compare.match.MatchOptions;
import org.eclipse.emf.compare.match.metamodel.MatchModel;
import org.eclipse.emf.compare.match.service.MatchService;
import org.eclipse.emf.compare.util.ModelUtils;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.EPackage.Registry;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.Resource.Diagnostic;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.*;

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

    private static final Logger logger = Logger.getLogger(ModelUtil.class.getName());

    /**
     * @param epckg
     * @param packageRegistry
     */
    private static void addPackages(final EPackage epckg, final Registry packageRegistry) {
        packageRegistry.put(epckg.getNsURI(), epckg);
        for (final EPackage subpck : epckg.getESubpackages()) {
            addPackages(subpck,packageRegistry);
        }

    }

    /**
     * @param content
     * @param content2
     */
    public static void assertModelEqual(final EObject orig, final EObject changed) {
        assertModelEqual(orig.eResource(), changed.eResource());
    }

    public static void assertModelEqual(final Resource orig, final Resource changed) {
        try {
            // we assume the very same metamodel, no matter where it was loaded from
            final Map<String, Object> options = new HashMap<String, Object>();
            options.put(MatchOptions.OPTION_DISTINCT_METAMODELS, true);

            final MatchModel match = MatchService.doResourceMatch(orig, changed, options);
            final DiffModel diff = DiffService.doDiff(match, false);

            final List<DiffElement> differences = new ArrayList<DiffElement>(stripOrderChanges(diff.getOwnedElements()));

            saveDiff(diff, match);
            describeDiff(differences, 0);
            // if there are no differences, there is still an empty change, bug in emfcompare?
            assertTrue(differences.isEmpty() || differences.get(0).getSubDiffElements().isEmpty());
        } catch (final InterruptedException e) {
            fail();
        }
    }

    public static void copyFile(final File in, final File out) throws IOException {
        final FileChannel inChannel = new FileInputStream(in).getChannel();
        final FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (final IOException e) {
            throw e;
        } finally {
            if (inChannel != null) {
                inChannel.close();
            }
            if (outChannel != null) {
                outChannel.close();
            }
        }
    }

    /**
     * @param file
     * @return
     */
    private static String copyToTempDirectory(final String file) {
        try {
            final String ending = file.substring(file.lastIndexOf('.'));
            final File tempfile = File.createTempFile("model", ending);
            copyFile(new File(file), tempfile);
            return tempfile.getAbsolutePath();
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
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
     * @param file
     * @return
     */
    private static String getAbsolutePathToTestModel(final String relativePath) {
        String file = relativePath;
        final URL url = ModelUtil.class.getClassLoader().getResource(relativePath);
        if (url != null) {
            file = url.toExternalForm();
            file = file.substring("file:/".length());
        }
        return file;
    }

    /**
     * @param eResource
     */
    private static void logResourceErrors(final Resource res) {
        if(res==null || res.getErrors().isEmpty()){
            return;
        }
        logger.info("There are errors in resource " + res);
        for (final Diagnostic diag : res.getErrors()) {
            logger.info(diag.toString());
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
        return readInputModels(relativePath, rs,false);
    }

    public static List<EObject> readInputModels(final String relativePath, final ResourceSet rs, final boolean simulateRemote) throws IOException {

        String file = getAbsolutePathToTestModel(relativePath);
        if (simulateRemote) {
            file = copyToTempDirectory(file);
        }
        final Resource resource = rs.getResource(URI.createFileURI(file), true);
        resource.load(null);

        // register packages
        for (final EObject eObject : resource.getContents()) {
            if (eObject instanceof EPackage) {
                final EPackage epckg = (EPackage) eObject;
                if (!epckg.getNsURI().equals(EcorePackage.eNS_URI)) {
                    addPackages(epckg,rs.getPackageRegistry());
                }
            }
        }
        return resource.getContents();

    }

    /**
     * Load test model into string.
     * 
     * @param relativePath
     *            to test model
     * @return
     */
    public static String readModel(final String relativePath){
        final String absPath = getAbsolutePathToTestModel(relativePath);
        final StringBuilder sb =new StringBuilder();
        BufferedReader br=null;
        try {
            br = new BufferedReader(new FileReader(absPath));
            String line = null;
            while((line = br.readLine())!=null){
                sb.append(line).append("\n");
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }finally{
            if(br!=null) {
                try {
                    br.close();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sb.toString();
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
            ModelUtils.save(snapshot, "foo/result.emfdiff");
        } catch (final IOException e) {
            e.printStackTrace();
        } //$NON-NLS-1$

    }

    /**
     * @param input
     * @return
     * @throws IOException
     */
    public static List<String> storeViaXml(final EObject... input) throws IOException {
        final List<String> result = new ArrayList<String>();
        final ResourceSetImpl rs = new ResourceSetImpl();

        final Map<String, String> options = new HashMap<String, String>();
        options.put(XMLResource.OPTION_ENCODING, "UTF8");
        options.put(XMLResource.OPTION_PROCESS_DANGLING_HREF, XMLResource.OPTION_PROCESS_DANGLING_HREF_RECORD);

        for (final EObject eo : input) {
            final String relativePath = "foo/" + eo.hashCode() + ".xmi";
            result.add(relativePath);
            final Resource res = rs.createResource(URI.createFileURI(relativePath));
            res.getContents().add(eo);
            res.save(options);
            logResourceErrors(eo.eResource());
            // TODO throw some exception to fail tests
        }
        return result;
    }

    public static void storeViaXml(final List<EObject> model, final String relPath) throws IOException {
        final ResourceSetImpl rs = new ResourceSetImpl();

        final Map<String, String> options = new HashMap<String, String>();
        options.put(XMLResource.OPTION_ENCODING, "UTF8");
        options.put(XMLResource.OPTION_PROCESS_DANGLING_HREF, XMLResource.OPTION_PROCESS_DANGLING_HREF_RECORD);

        final Resource res = rs.createResource(URI.createFileURI("foo/" + System.currentTimeMillis() + "/" + relPath));
        res.getContents().addAll(model);
        res.save(options);
        for(final EObject eo:model) {
            logResourceErrors(eo.eResource());
        }
    }

    /**
     * {@link ReferenceOrderChange} is a irrelevant change for our test cases. Remove all differences of this kind.
     * 
     * @param ownedElements
     * @return
     */
    private static Collection<DiffElement> stripOrderChanges(final EList<DiffElement> ownedElements) {
        final Collection<DiffElement> res = new ArrayList<DiffElement>();
        for(final DiffElement de: ownedElements){
            if (!(de instanceof ReferenceOrderChange)) {
                res.addAll(stripOrderChanges(de.getSubDiffElements()));
            }
        }
        return res;
    }

}
