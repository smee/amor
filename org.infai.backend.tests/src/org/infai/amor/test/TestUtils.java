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

import java.util.*;

import org.eclipse.emf.common.util.URI;
import org.infai.amor.backend.*;
import org.jmock.Expectations;
import org.jmock.Mockery;

/**
 * @author sdienst
 *
 */
public class TestUtils {

    /**
     * @param uri
     * @param modelPath
     * @param ct
     * @return
     */
    public static ModelLocation createLocation(final URI uri, final String modelPath, final ChangeType ct) {
        return new ModelLocation() {

            @Override
            public ChangeType getChangeType() {
                return ct;
            }

            @Override
            public URI getExternalUri() {
                return uri;
            }

            @Override
            public Map<String, Object> getMetaData() {
                return Collections.EMPTY_MAP;
            }

            @Override
            public Collection<String> getNamespaceUris() {
                return Collections.EMPTY_LIST;
            }

            @Override
            public String getRelativePath() {
                return modelPath;
            }

            @Override
            public boolean isMetaModel() {
                return false;
            }
        };
    }

    /**
     * @param branchname
     * @param revisionId
     * @return
     */
    public static InternalRevision createRevision(final long revisionId) {
        return new InternalRevision() {
            Collection<ModelLocation> mlocs = new ArrayList<ModelLocation>();

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
                for (final ModelLocation mloc : mlocs) {
                    if (mloc.getRelativePath().equals(modelPath)) {
                        return mloc;
                    }
                }
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
                return revisionId;
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
                mlocs.add(loc);
            }

        };
    }

    /**
     * @param branchname
     * @param revisionId
     * @return
     */
    public static CommitTransaction createTransaction(final String branchname, final long revisionId) {
        final Mockery context = new Mockery();
        final Branch branch = context.mock(Branch.class, "" + Math.random());
        final InternalRevision revision = createRevision(revisionId);
        final InternalCommitTransaction ct = context.mock(InternalCommitTransaction.class);
        context.checking(new Expectations() {
            {
                allowing(branch).getName();
                will(returnValue(branchname));
                allowing(ct).getRevision();
                will(returnValue(revision));
                allowing(ct).getBranch();
                will(returnValue(branch));
            }
        });
        return ct;
    }
}
