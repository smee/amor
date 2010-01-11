/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.backend.filestorage.test;

import static org.infai.amor.test.ModelUtil.readInputModel;
import static org.junit.Assert.*;

import java.io.IOException;

import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.infai.amor.backend.*;
import org.infai.amor.backend.Revision.ChangeType;
import org.infai.amor.backend.internal.ModelImpl;
import org.infai.amor.backend.responses.CommitSuccessResponse;
import org.infai.amor.test.ModelUtil;
import org.junit.Test;

import com.google.common.collect.Iterables;

/**
 * @author sdienst
 * 
 */
public class IntegrationTests extends AbstractIntegrationTest {
    @Test
    public void shouldBeAbleToCreateBranch() throws Exception {
        // given
        final String branchName = "branch" + Math.random();
        // when
        final Branch branch = repository.createBranch(null, branchName);
        // then
        assertNotNull(branch);
        assertEquals(branchName, branch.getName());

        // tear down
        // commitTransaction.getNeoTransaction().failure();
        // commitTransaction.getNeoTransaction().finish();
    }

    @Test(expected = IOException.class)
    public void shouldNotLoadAfterRollback() throws Exception {
        // given
        final EObject input = readInputModel("testmodels/base.ecore");

        final Branch branch = repository.createBranch(null, "trunk");
        final CommitTransaction ct = repository.startCommitTransaction(branch);
        ct.setCommitMessage("test");
        ct.setUser("mustermann");
        // when
        // model checked in
        final Response checkin = repository.checkin(new ModelImpl(input, "testmodels/base.ecore"), ct);
        // but rolled back
        repository.rollbackTransaction(ct);
        // then
        repository.checkout(checkin.getURI());
    }

    @Test
    public void shouldSaveAndLoadModel() throws Exception {
        // given
        final EObject input = readInputModel("testmodels/base.ecore");

        final Branch branch = repository.createBranch(null, "trunk");
        final CommitTransaction ct = repository.startCommitTransaction(branch);
        ct.setCommitMessage("test");
        ct.setUser("mustermann");
        // when
        // model checked in successfully
        final Response checkin = repository.checkin(new ModelImpl(input, "testmodels/base.ecore"), ct);
        final CommitSuccessResponse commitResponse = (CommitSuccessResponse) repository.commitTransaction(ct);
        /*
         * // then the revision should know about this model final Revision revision =
         * repository.getRevision(commitResponse.getURI()); assertEquals(1, revision.getModelReferences().size());
         */
        // and reloaded from repository successfully
        final Model output = repository.checkout(checkin.getURI());
        // then
        ModelUtil.assertModelEqual(input, output.getContent().get(0));
    }

    @Test
    public void shouldShowRepositoryContentsPerRevision() throws Exception {
        // given
        final EObject input = readInputModel("testmodels/base.ecore");

        final Branch branch = repository.createBranch(null, "trunk");
        // when
        // start new checkin transaction
        final CommitTransaction ct = repository.startCommitTransaction(branch);
        ct.setCommitMessage("test");
        ct.setUser("mustermann");
        // add a model
        repository.checkin(new ModelImpl(input, "testmodels/base.ecore"), ct);
        final CommitSuccessResponse commitResponse = (CommitSuccessResponse) repository.commitTransaction(ct);
        // then the revision should know about this model
        final Revision revision = repository.getRevision(commitResponse.getURI());
        assertEquals(1, revision.getModelReferences(ChangeType.ADDED).size());
        // also the current repository contents should not be empty
        final Iterable<URI> intermediaryContents = repository.getActiveContents(commitResponse.getURI());
        assertEquals(1, Iterables.size(intermediaryContents));
        // now delete the model
        final CommitTransaction ct2 = repository.startCommitTransaction(branch);
        ct2.setCommitMessage("delete model");
        ct2.setUser("mustermann");
        repository.deleteModel(new Path("testmodels/base.ecore"), ct2);
        final CommitSuccessResponse commitResponse2 = (CommitSuccessResponse) repository.commitTransaction(ct2);

        final Iterable<URI> activeContents = repository.getActiveContents(commitResponse2.getURI());
        // no models should be visible at revision 3
        assertTrue(Iterables.isEmpty(activeContents));

    }
}
