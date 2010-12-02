/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.filestorage.test;

import static org.infai.amor.test.ModelUtil.readInputModel;
import static org.infai.amor.test.ModelUtil.readInputModels;
import static org.junit.Assert.assertEquals;

import java.util.*;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.infai.amor.backend.*;
import org.infai.amor.backend.resources.AmorResourceSetImpl;
import org.infai.amor.backend.responses.CommitSuccessResponse;
import org.infai.amor.test.ModelUtil;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author sdienst
 *
 */
public class StoreLinkedModelsTest extends AbstractIntegrationTest {

    @Test
    public void shouldBeAbleToLoadAllJavaEMFPackages() throws Exception {
        // given
        final List<EObject> model = readInputModels("testmodels/02/java.ecore");
        // when
        final int numberOfPackages = model.size();
        // then
        assertEquals(18, numberOfPackages);
    }

    @Ignore(value = "no real test yet")
    @Test
    public void shouldInformAboutLinkedModels() throws Exception {
        // given
        final ResourceSet rs = new AmorResourceSetImpl();
        final Collection<EObject> javaMetamodel = readInputModels("testmodels/02/java.ecore", rs);
        final EObject javaModelInstance = readInputModel("testmodels/02/Hello.java.xmi", rs);
        // when
        for (final Iterator<EObject> it = javaModelInstance.eAllContents(); it.hasNext();) {
            final EObject eo = it.next();
            if (eo.eIsProxy()) {
                final URI proxyURI = ((InternalEObject) eo).eProxyURI();
                if (proxyURI != null) {
                    System.out.println(proxyURI);
                }
            }
        }
        // then
    }

    @Ignore(value = "classcastexception on comparing the checked out model to the original...")
    @Test
    public void shouldSaveAndLoadModel() throws Exception {
        // given
        final ResourceSet rs = new AmorResourceSetImpl();

        final Branch branch = repository.createBranch(null, "trunk");
        final CommitTransaction ct = repository.startCommitTransaction(branch);
        ct.setCommitMessage("test");
        ct.setUser("mustermann");
        // when
        // model checked in successfully
        final Response checkin = repository.checkin(new ModelImpl(readInputModels("testmodels/02/java.ecore", rs), "testmodels/02/java.ecore"), ct);
        final EObject javaModel = readInputModel("testmodels/02/Hello.java.xmi", rs);
        final Response checkin2 = repository.checkin(new ModelImpl(javaModel, "testmodels/02/Hello.java.xmi"), ct);
        final CommitSuccessResponse commitResponse = (CommitSuccessResponse) repository.commitTransaction(ct);
        /*
         * // then the revision should know about this model final Revision revision =
         * repository.getRevision(commitResponse.getURI()); assertEquals(1, revision.getModelReferences().size());
         */
        // and reloaded from repository successfully
        final Model output = repository.checkout(checkin2.getURI());
        // then
        // ModelUtil.storeViaXml(output.getContent().toArray(new EObject[] {}));
        ModelUtil.assertModelEqual(javaModel, output.getContent().get(0));
    }
}
