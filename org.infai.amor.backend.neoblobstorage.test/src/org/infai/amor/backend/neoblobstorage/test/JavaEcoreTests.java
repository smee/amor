/*******************************************************************************
 * Copyright (c) 2010 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.neoblobstorage.test;

import static org.infai.amor.test.ModelUtil.readInputModels;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.infai.amor.backend.Model;
import org.infai.amor.backend.Revision;
import org.infai.amor.backend.internal.ModelImpl;
import org.infai.amor.backend.neo.NeoProvider;
import org.infai.amor.backend.neostorage.NeoBlobStorage;
import org.infai.amor.backend.storage.Storage;
import org.infai.amor.test.*;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;

/**
 * Make sure we can restore a complex ecore with several packages and a multitute of references between them.
 * 
 * @author sdienst
 * 
 */
public class JavaEcoreTests extends AbstractNeo4JTest {
    Storage storage;
    private ResourceSet rs;
    private Revision revision;

    /**
     * @param string
     * @param i
     * @return
     */
    private List<EObject> checkin(final String location) {
        try {
            final List<EObject> contents = readInputModels(location, rs, true);

            final ModelImpl model = new ModelImpl(contents, location);
            final URI externalUri = URI.createURI(String.format("amor://localhost/trunk/%d/%s", revision.getRevisionId(), location));

            storage.checkin(model, externalUri, revision);

            return contents;
        } catch (final IOException e) {
            throw new AssertionError(e);
        }
    }

    @Before
    public void setup() throws Exception {
        final NeoProvider np = new NeoProvider() {

            @Override
            public GraphDatabaseService getNeo() {
                return neoservice;
            }
        };
        this.storage = new NeoBlobStorage(np);
        rs = new ResourceSetImpl();
        revision = TestUtils.createRevision(1);
        storage.startTransaction(TestUtils.createTransaction("trunk", revision.getRevisionId()));
    }

    @Test
    public void shouldRestoreJavaEcore() throws Exception {
        // given
        checkin("testmodels/Ecore.ecore");
        // when
        final List<EObject> orig = checkin("testmodels/02/java.ecore");
        final Model checkout = storage.checkout(new Path("testmodels/02/java.ecore"), revision);
        // then
        ModelUtil.assertModelEqual(orig.get(0).eResource(), checkout.getContent().get(0).eResource());

    }
}
