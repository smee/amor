/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.internal.impl;

import static org.junit.Assert.assertEquals;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.xml.type.internal.DataValue.URI.MalformedURIException;
import org.infai.amor.backend.Branch;
import org.infai.amor.backend.impl.CommitTransactionImpl;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;

/**
 * @author sdienst
 * 
 */
public class UriTest {

    private UriHandlerImpl uh;
    private Mockery context;

    @Before
    public void setup() {
        uh = new UriHandlerImpl();
        context = new Mockery();
    }

    @Test
    public void testCreateUriOnCommit() {
        final Branch branch = context.mock(Branch.class);
        context.checking(new Expectations() {
            {
                allowing(branch).getName();
                will(returnValue("branch"));
            }
        });
        final CommitTransactionImpl tr = new CommitTransactionImpl(branch, 55, null);

        final URI uri = uh.createUriFor(tr);
        assertEquals("amor://localhost/repo/branch/55", uri.toString());
    }

    @Test(expected = MalformedURIException.class)
    public void testExpectsAtLeastFiveSegments() throws MalformedURIException {
        // at least hostname,reponame,branch, modelname,revision
        uh.extractBranchName(URI.createURI("amor:/localhost/repo1"));
    }

    @Test(expected = MalformedURIException.class)
    public void testExpectsAtLeastThreeSegments2() throws MalformedURIException {
        uh.extractRevision(URI.createURI("amor://localhost/repo1/branch1/model"));
    }

    @Test
    public void testRecognizesBranchname() throws MalformedURIException {
        final String branchName = uh.extractBranchName(URI.createURI("amor://localhost/repo1/branch1/123/model"));
        assertEquals(branchName, "branch1");
    }

    @Test
    public void testRecognizesRevision() throws MalformedURIException {
        final long revision = uh.extractRevision(URI.createURI("amor://localhost/repo1/branch1/123/model"));
        assertEquals(123, revision);
    }

    @Test
    public void testUriSegments() {
        final URI uri = URI.createURI("amor://localhost/repo1/main/123/dir1/dir2/filename");
        assertEquals(6, uri.segmentCount());
    }
}
