/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.impl;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.any;
import static com.google.common.collect.Iterators.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.*;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.EStructuralFeature.Setting;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil.ExternalCrossReferencer;
import org.eclipse.emf.ecore.util.EcoreUtil.ProxyCrossReferencer;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.gmf.runtime.notation.NotationPackage;
import org.infai.amor.test.ModelUtil;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

/**
 * @author sdienst
 *
 */
public class ProxyTests {
    public final static class GetResourceUri implements Function<EObject, URI> {
        public final static GetResourceUri INSTANCE = new GetResourceUri();
        @Override
        public URI apply(final EObject from) {
            final Resource res = from.eResource();
            if (res != null) {
                return res.getURI();
            } else {
                return null;
            }
        }
    }

    private final class SameResource implements Predicate<EObject> {
        /**
         * 
         */
        private final URI mainUri;

        /**
         * @param mainUri
         */
        private SameResource(final URI mainUri) {
            this.mainUri = mainUri;
        }

        @Override
        public boolean apply(final EObject input) {
            final Resource resource = input.eResource();
            return resource != null && resource.getURI().equals(mainUri);
        }
    }


    /**
     * @return
     */
    private static Predicate<EObject> isEString() {
        return new Predicate<EObject>() {

            @Override
            public boolean apply(final EObject eo) {
                return eo instanceof EDataType && ((EDataType)eo).getName().equals("EString");
            }
        };
    }

    /**
     * @param map
     */
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
    /**
     * @param extUriSets
     * @return
     */
    private <T> Set<T> flatten(final Iterator<Set<T>> nestedSet) {
        final Set<T> result = Sets.newHashSet();
        while(nestedSet.hasNext()) {
            result.addAll(nestedSet.next());
        }
        return result;
    }

    /**
     * @param model
     * @param resourceUri
     * @return
     */
    private Iterator<EObject> getObjectsWithExternalReferences(final EObject model, final URI resourceUri) {
        final Predicate<EObject> proxiedEobjectPredicate = new Predicate<EObject>() {
            @Override
            public boolean apply(final EObject eo) {
                return any(eo.eCrossReferences(), not(new SameResource(resourceUri)));
            }
        };

        // when
        final Iterator<EObject> proxiedEobjects = filter(model.eAllContents(), proxiedEobjectPredicate);
        return proxiedEobjects;
    }

    @Test
    public void shouldFindExternalReferences() throws Exception {
        // given
        final EObject model = ModelUtil.readInputModel("testmodels/bflow/oepc.ecore");
        // when
        final Map<EObject, Collection<Setting>> map = ExternalCrossReferencer.find(model);
        debugPrint(map);
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

        final Iterator<EObject> proxiedEobjects = getObjectsWithExternalReferences(model, resourceUri);
        // then there should be 34 eobjects with references to at least one other ecore file
        final int size = size(proxiedEobjects);
        assertEquals(34, size);
    }
    @Test
    @Ignore
    public void shouldFindProxiedElementsInM1() throws Exception {
        // given
        // an available gmf notation package
        NotationPackage.eINSTANCE.eClass();
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("oepc", new XMIResourceFactoryImpl());

        ResourceSetImpl rs = new ResourceSetImpl();
        // final EObject notationPackage = ModelUtil.readInputModel("testmodels/bflow/notation_1.02.ecore", rs);
        final EObject oepcPackage = ModelUtil.readInputModel("testmodels/bflow/oepc.ecore", rs);
        final List<EObject> oepclist = ModelUtil.readInputModels("testmodels/bflow/GewAnm.oepc", rs);

        // change renamed features, stupid gmf!
        // TODO introduce configurable element name changes in the backend
        /*
         * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=159226
         * children is persistedChildren edges is persistedEdges
         * relativeBendpoints do not exist at all in notation.ecore.... (NotationPackage uses custom serialization code for
         * bendpoints, not generated...)
         */
        for (final EObject root : oepclist) {
            for (final Iterator<EObject> it = root.eAllContents(); it.hasNext();) {
                final EObject eo = it.next();
                if (eo.getClass().getName().contains("RelativeBend")) {
                    System.out.println(eo);
                }
                /*
                 * if (eo.eClass().getEPackage().getNsURI().startsWith("http://www.eclipse.org/gmf/runtime/1.0") &&
                 * eo.eClass().getName().equals("Node")) { for (final EReference ref : eo.eClass().getEAllReferences()) { if
                 * (ref.getName().equals("children")) { ref.setName("persistedChildren"); } else if
                 * (ref.getName().equals("edges")) { ref.setName("persistedEdges"); } } }
                 */            }
        }
        // when
        final List<String> xmls = ModelUtil.storeViaXml(oepclist.get(0));
        // remove gmf package
        rs = new ResourceSetImpl();
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
        final Iterator<Set<URI>> extUriSets = transform(proxiedEobjects, new Function<EObject, Set<URI>>() {
            @Override
            public Set<URI> apply(final EObject eo) {
                final HashSet<URI> set = Sets.newHashSet();
                final Iterator<URI> uris = transform(eo.eCrossReferences().iterator(), GetResourceUri.INSTANCE);
                addAll(set, filter(uris, not(equalTo(resourceUri))));
                return set;
            }

        });

        final Set<URI> uniqueUris = flatten(extUriSets);

        assertEquals(2, uniqueUris.size());
        assertTrue(uniqueUris.contains(URI.createURI("http://www.eclipse.org/emf/2002/Ecore")));
        // TODO do not use absolute path
        assertTrue(uniqueUris.contains(URI.createURI("file:/D:/Projekte/Amor/amor_ws/org.infai.backend.tests/bin/testmodels/bflow/bflow.ecore")));
    }
}
