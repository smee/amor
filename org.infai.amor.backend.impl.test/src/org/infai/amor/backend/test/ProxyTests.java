/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.test;

import static com.google.common.collect.Iterables.any;
import static com.google.common.collect.Iterators.*;
import static org.infai.amor.backend.util.EcoreModelHelper.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.*;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.EStructuralFeature.Setting;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil.ExternalCrossReferencer;
import org.eclipse.emf.ecore.util.EcoreUtil.ProxyCrossReferencer;
import org.infai.amor.backend.resources.AmorResourceSetImpl;
import org.infai.amor.backend.util.EcoreModelHelper;
import org.infai.amor.test.ModelUtil;
import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

/**
 * @author sdienst
 *
 */
public class ProxyTests {

    private void debugPrint(final Map<EObject, Collection<Setting>> map) {
        for (final EObject eo : map.keySet()) {
            String k = eo.toString();
            if (eo instanceof ENamedElement) {
                k = ((ENamedElement) eo).getName();
            }
            System.out.println(k + " -> " + Iterables.transform(map.get(eo), new Function<Setting, Object>() {
                @Override
                public Object apply(final Setting from) {
                    if (from.getEObject() instanceof ENamedElement) {
                        return ((ENamedElement) from.getEObject()).getName();
                    } else {
                        return from.getEObject().toString();
                    }
                }
            }));
        }
    }
    @Test
    public void shouldFindExternalReferences() throws Exception {
        // given
        final EObject model = ModelUtil.readInputModel("testmodels/bflow/oepc.ecore");
        // when
        final Map<EObject, Collection<Setting>> map = ExternalCrossReferencer.find(model);
        // debugPrint(map);

        // then
        // there are references to 16 external elements,14 of the bflow.ecore meta model, 1 to ecore EString and one to the
        // opec-package factory
        assertEquals(16, map.size());
        assertEquals(1, size(filter(map.keySet().iterator(), EFactory.class)));
        assertEquals(1, size(filter(map.keySet().iterator(), isEString())));
    }
    @Test
    public void shouldFindProxiedElements() throws Exception {
        // given
        final EObject model = ModelUtil.readInputModel("testmodels/bflow/oepc.ecore");
        final URI resourceUri = model.eResource().getURI();

        final Iterator<EObject> proxiedEobjects = EcoreModelHelper.getObjectsWithExternalReferences(model, resourceUri);
        // then there should be 34 eobjects with references to at least one other ecore file
        final int size = size(proxiedEobjects);
        assertEquals(34, size);
    }

    @Test
    // @Ignore(value = "GMF is pretty f***ed up...")
    public void shouldFindProxiedElementsInM1() throws Exception {
        final ResourceSet rs = new AmorResourceSetImpl();

        final EObject notationPackage = ModelUtil.readInputModel("testmodels/bflow/notation_1.02.ecore", rs);
        final EObject oepcPackage = ModelUtil.readInputModel("testmodels/bflow/oepc.ecore", rs);

        final List<EObject> oepclist = ModelUtil.readInputModels("testmodels/bflow/GewAnm.oepc", rs);
        // when
        final List<String> xmls = ModelUtil.storeViaXml(oepclist.toArray(new EObject[oepclist.size()]));
        // remove gmf package
        // rs = new AmorResourceSetImpl();
        // final EObject notationPackage = ModelUtil.readInputModel("testmodels/bflow/notation_1.02.ecore", rs);
        ModelUtil.readInputModel("testmodels/bflow/oepc.ecore", rs);

        for (final String uri : xmls) {
            ModelUtil.readInputModels(uri, rs);
        }

    }

    @Test
    public void shouldFindProxies() throws Exception {
        // given
        final EObject model = ModelUtil.readInputModel("testmodels/bflow/oepc.ecore");
        // when
        final Map<EObject, Collection<Setting>> map = ProxyCrossReferencer.find(model);
        // then there should be 21 proxies
        assertEquals(21, map.size());
    }

    @Test
    public void shouldFindReferencedProxyResources() throws Exception {
        // given
        final EObject model = ModelUtil.readInputModel("testmodels/bflow/oepc.ecore");
        final URI resourceUri = model.eResource().getURI();

        // when
        final Iterator<EObject> proxiedEobjects = getObjectsWithExternalReferences(model, resourceUri);
        // find all uris!=oepc that each object references to
        final Iterator<Set<URI>> extUriSets = transform(proxiedEobjects, getReferencesToExternalModels(resourceUri));

        final Set<URI> uniqueUris = flatten(extUriSets);

        assertEquals(1, uniqueUris.size());
        assertTrue(any(uniqueUris, uriEndsWith("testmodels/bflow/bflow.ecore")));
    }

    @Test
    public void shouldFindReferencedProxyResourcesForJavaXMI() throws Exception {
        // given
        final ResourceSetImpl rs = new ResourceSetImpl();
        ModelUtil.readInputModels("testmodels/02/primitive_types.ecore", rs);
        ModelUtil.readInputModels("testmodels/02/java.ecore", rs);
        final EObject model = ModelUtil.readInputModel("testmodels/02/Hello.java.xmi", rs);
        final URI resourceUri = model.eResource().getURI();

        // when
        final Iterator<EObject> proxiedEobjects = getObjectsWithExternalReferences(model, resourceUri);
        // find all uris!=oepc that each object references to
        final Iterator<Set<URI>> extUriSets = transform(proxiedEobjects, getReferencesToExternalModels(resourceUri));

        final Set<URI> uniqueRelativeUris = Sets.newHashSet();
        addAll(uniqueRelativeUris, transform(flatten(extUriSets).iterator(), makeUriRelativeTo(resourceUri)));

        // then
        assertEquals(3, uniqueRelativeUris.size());
        assertTrue(any(uniqueRelativeUris, uriEndsWith("java/lang/System.class.xmi")));
        assertTrue(any(uniqueRelativeUris, uriEndsWith("java/io/PrintStream.class.xmi")));
        assertTrue(any(uniqueRelativeUris, uriEndsWith("java/lang/String.class.xmi")));
    }

    @Test
    public void shouldStoreLinkedModels() throws Exception {
        // given
        final EObject javaMM = ModelUtil.readInputModel("testmodels/02/java.ecore");
        final EObject ptMM = ModelUtil.readInputModel("testmodels/02/primitive_types.ecore");

        // when

        // then
    }
}
