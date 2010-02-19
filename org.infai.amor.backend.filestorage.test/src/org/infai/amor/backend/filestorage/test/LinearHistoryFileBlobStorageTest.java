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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;

import org.eclipse.core.runtime.Path;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.infai.amor.backend.*;
import org.infai.amor.backend.filestorage.FileBlobStorage;
import org.infai.amor.backend.internal.InternalRevision;
import org.infai.amor.backend.internal.ModelImpl;
import org.infai.amor.backend.internal.impl.ChangedModelImpl;
import org.infai.amor.test.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Test, if we can correctly store and retrieve a changed model instance in one branch.
 * 
 * @author sdienst
 * 
 */
public class LinearHistoryFileBlobStorageTest extends AbstractNeo4JPerformanceTest {
    private static final String BRANCHNAME = "testBranch";
    private FileBlobStorage storage;
    private File tempDir;
    private InternalRevision rev;

    /**
     * The models to store. First the meta model, then the revisions of the instance model.
     */
    private final String[] models = {
        "testmodels/filesystem.ecore", "testmodels/fs/simplefilesystem_v1.filesystem", "testmodels/fs/simplefilesystem_v2.filesystem", "testmodels/fs/simplefilesystem_v3.filesystem", "testmodels/fs/simplefilesystem_v4.filesystem" };
    /**
     * Common name of the instancemodel
     */
    private final String modelName = "testmodels/simplefilesystem.filesystem";



    @Before
    public void setUp() throws IOException {
        tempDir = File.createTempFile("storage", "temp");
        tempDir.delete();
        tempDir.mkdirs();

        storage = new FileBlobStorage(tempDir, BRANCHNAME);
        rev = new InternalRevision() {

            @Override
            public String getCommitMessage() {
                return null;
            }

            @Override
            public Date getCommitTimestamp() {
                return null;
            }

            @Override
            public ModelLocation getModelLocation(final String modelPath) {
                return null;
            }

            @Override
            public Collection<ModelLocation> getModelReferences(final ChangeType... ct) {
                return null;
            }

            @Override
            public Revision getPreviousRevision() {
                return null;
            }

            @Override
            public long getRevisionId() {
                return 0;
            }

            @Override
            public String getUser() {
                return null;
            }

            @Override
            public void setCommitMessage(final String message) {
            }

            @Override
            public void setTimestamp(final long currentTimeMillis) {
            }

            @Override
            public void setUser(final String username) {
            }

            @Override
            public void touchedModel(final ModelLocation loc) {
            }
        };
    }

    @Test
    public void shouldPersistLinearHistoryOfAModel() throws Exception {
        // given
        final ResourceSet rs = new ResourceSetImpl();
        // checked in metamodel
        final Model mm = new ModelImpl(ModelUtil.readInputModel(models[0], rs), models[0]);
        CommitTransaction tr = TestUtils.createTransaction(BRANCHNAME, 0);
        storage.startTransaction(tr);
        storage.checkin(mm, null, tr.getRevision());
        storage.commit(tr);
        split("Committed M2");

        // checked in first model revision
        final Model m = new ModelImpl(ModelUtil.readInputModel(models[1], rs), modelName);
        tr = TestUtils.createTransaction(BRANCHNAME, 1);
        storage.startTransaction(tr);
        storage.checkin(m, null, tr.getRevision());
        storage.commit(tr);
        split("Committed initial M1");

        EObject lastModel = m.getContent().get(0);
        // commit all changes to the model in order
        for (int i = 2; i < models.length; i++) {
            tr = TestUtils.createTransaction(BRANCHNAME, i);
            storage.startTransaction(tr);
            final EObject changedModel = ModelUtil.readInputModel(models[i], rs);
            final ChangedModel cm = new ChangedModelImpl(ModelUtil.createEpatch(lastModel, changedModel), modelName);
            storage.checkin(cm, null, tr.getRevision());
            storage.commit(tr);

            final Model checkedoutmodel = storage.checkout(new Path(modelName), TestUtils.createRevision(i));
            ModelUtil.assertModelEqual(changedModel, checkedoutmodel.getContent().get(0));
            lastModel = changedModel;
            split("Roundtrip completed for M1 revision " + i);
        }

    }
}
