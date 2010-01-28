/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.util;

import static com.google.common.base.Predicates.*;
import static com.google.common.collect.Iterables.any;
import static com.google.common.collect.Iterators.*;

import java.util.*;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.EStructuralFeature.Setting;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

/**
 * Collection of {@link Predicate}s, {@link Function}s and misc. functions for querying emf models.
 * 
 * @author sdienst
 * 
 */
public class EcoreModelHelper {
    private static String ecoreUri = URI.createURI(EcorePackage.eNS_URI).toString();

    /**
     * Return all externally referenced models, ignoring Ecore itself. Deresolves agains <code>resourceUri</code>.
     * 
     * @param model
     * @param resourceUri
     * @return
     */
    public static Set<URI> findReferencedModels(final EObject model, final URI resourceUri) {
        return findReferencedModels(model, resourceUri, resourceUri);
    }

    /**
     * Return all externally referenced models, ignoring Ecore itself.
     * 
     * @param model
     * @param resourceUri
     *            absolute uri locating the resource of model
     * @param baseUri
     *            absolute uri that all referenced uri should be deresolved agains
     * @return Set of relative paths that address the dependencies of model
     */
    public static Set<URI> findReferencedModels(final EObject model, final URI resourceUri, final URI baseUri) {
        final Map<EObject, Collection<Setting>> map = EcoreUtil.ProxyCrossReferencer.find(model);
        // final Iterator<EObject> proxiedEobjects = getObjectsWithExternalReferences(model, resourceUri);
        // // find all uris!=oepc that each object references to
        // final Iterator<Set<URI>> extUriSets = transform(proxiedEobjects, getReferencesToExternalModels(resourceUri));
        //
        final Set<URI> uniqueRelativeUris = Sets.newHashSet();
        // addAll(uniqueRelativeUris, transform(flatten(extUriSets).iterator(), makeUriRelativeTo(baseUri)));
        //
        addAll(uniqueRelativeUris,
                filter(transform(transform(map.keySet().iterator(),
                        getProxyUri()),
                        makeUriRelativeTo(baseUri)),
                        and(not(startsWith(ecoreUri)), not(isFragmentOnly()))));
        return uniqueRelativeUris;
    }

    public static <T> Set<T> flatten(final Iterator<Set<T>> nestedSet) {
        final Set<T> result = Sets.newHashSet();
        while (nestedSet.hasNext()) {
            result.addAll(nestedSet.next());
        }
        return result;
    }

    /**
     * Find all eobjects which have at least one reference to another resource.
     * 
     * @param model
     * @param resourceUri
     * @return
     */
    public static Iterator<EObject> getObjectsWithExternalReferences(final EObject model, final URI resourceUri) {
        final Predicate<EObject> proxiedEobjectPredicate = new Predicate<EObject>() {
            @Override
            public boolean apply(final EObject eo) {
                return any(eo.eCrossReferences(), not(sameResource(resourceUri)));
            }
        };

        // when
        final Iterator<EObject> proxiedEobjects = filter(model.eAllContents(), proxiedEobjectPredicate);
        return proxiedEobjects;
    }

    /**
     * @return
     */
    private static Function<EObject, URI> getProxyUri() {
        return new Function<EObject, URI>() {
            @Override
            public URI apply(final EObject eo) {
                return ((InternalEObject)eo).eProxyURI();
            }
        };
    }

    public static Function<? super EObject, ? extends Set<URI>> getReferencesToExternalModels(final URI resourceUri) {
        return new Function<EObject, Set<URI>>() {
            @Override
            public Set<URI> apply(final EObject eo) {
                final HashSet<URI> set = Sets.newHashSet();
                final Iterator<URI> uris = transform(eo.eCrossReferences().iterator(), getResourceUri());
                addAll(set, filter(uris, not(or(startsWith(ecoreUri), equalTo(resourceUri)))));
                return set;
            }
        };
    }

    public static Function<EObject, URI> getResourceUri() {
        return new Function<EObject, URI>(){
            @Override
            public URI apply(final EObject from) {
                final Resource res = from.eResource();
                if (res != null) {
                    return res.getURI();
                } else {
                    return null;
                }
            }};
    }

    public static Predicate<EObject> isEString() {
        return new Predicate<EObject>() {

            @Override
            public boolean apply(final EObject eo) {
                return eo instanceof EDataType && ((EDataType)eo).getName().equals("EString");
            }
        };
    }

    /**
     * @return
     */
    private static Predicate<URI> isFragmentOnly() {
        return new Predicate<URI>() {
            @Override
            public boolean apply(final URI input) {
                return input.hasFragment() && input.hasEmptyPath();
            }
        };
    }

    public static Function<URI, URI> makeUriRelativeTo(final URI baseUri) {
        return new Function<URI, URI>() {
            @Override
            public URI apply(final URI absUri) {
                assert !absUri.isRelative();
                return absUri.deresolve(baseUri);
            }

        };
    }

    public static Predicate<EObject> sameResource(final URI uri) {
        return new Predicate<EObject>(){
            @Override
            public boolean apply(final EObject input) {
                final Resource resource = input.eResource();
                return resource != null && resource.getURI().equals(uri);
            }
        };
    }

    /**
     * @param ecoreUri2
     * @return
     */
    private static Predicate<URI> startsWith(final String uri) {
        return new Predicate<URI>() {
            @Override
            public boolean apply(final URI input) {
                return input.toString().startsWith(uri);
            }
        };
    }
    public static Predicate<URI> uriEndsWith(final String string) {
        return new Predicate<URI>() {
            @Override
            public boolean apply(final URI uri) {
                return uri.toString().endsWith(string);
            }
        };
    }

}
