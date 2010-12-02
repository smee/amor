/*******************************************************************************
 * Copyright (c) 2010 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.test;

import java.io.IOException;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

/**
 * @author sdienst
 *
 */
public class Snippet {
    private static void addPackages(final EPackage epckg, final EPackage.Registry packageRegistry) {
        packageRegistry.put(epckg.getNsURI(), epckg);
        for (final EPackage subpck : epckg.getESubpackages()) {
            addPackages(subpck,packageRegistry);
        }

    }

    /**
     * Parameter: several model files, ecores first, instances after Example: bflow.ecore oepc.ecore sample_ext.xmi external.xmi
     * 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        ResourceSet rs = new ResourceSetImpl();
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());

        for(String file:args){
            List<EObject> modelContents = readInputModels(file, rs);
            System.out.println(modelContents.size()+" model objects loaded from "+file);
            System.out.println(modelContents);

        }
    }

    public static List<EObject> readInputModels(final String relativePath, final ResourceSet rs) throws IOException {
        final Resource resource = rs.getResource(URI.createFileURI(relativePath), true);
        resource.load(null);

        // register packages
        for (final EObject eObject : resource.getContents()) {
            if (eObject instanceof EPackage) {
                final EPackage epckg = (EPackage) eObject;
                if (!epckg.getNsURI().equals(EcorePackage.eNS_URI)) {
                    addPackages(epckg, rs.getPackageRegistry());
                }
            }
        }
        return resource.getContents();
    }
}

