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

import java.util.Collections;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.infai.amor.backend.Branch;
import org.infai.amor.backend.CommitTransaction;
import org.infai.amor.backend.Revision;
import org.infai.amor.backend.Revision.ChangeType;
import org.infai.amor.backend.internal.ModelLocation;
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
    public static ModelLocation createLocation(final URI uri, final String modelPath, final Revision.ChangeType ct) {
        return new ModelLocation() {

            @Override
            public ChangeType getChangeType() {
                return ct;
            }

            @Override
            public Map<String, Object> getCustomProperties() {
                return Collections.EMPTY_MAP;
            }

            @Override
            public URI getExternalUri() {
                return uri;
            }

            @Override
            public String getRelativePath() {
                return modelPath;
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
        final CommitTransaction ct = context.mock(CommitTransaction.class);
        context.checking(new Expectations() {
            {
                allowing(branch).getName();
                will(returnValue(branchname));
                allowing(ct).getRevisionId();
                will(returnValue(revisionId));
                allowing(ct).getBranch();
                will(returnValue(branch));
            }
        });
        return ct;
    }
}
