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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
        uh = new UriHandlerImpl("localhost", "repo");
        context = new Mockery();
    }

    @Test(expected = MalformedURIException.class)
    public void shouldFindBranchnameOnShortUris() throws MalformedURIException {
        // at least hostname,reponame,branch, modelname,revision
        uh.extractBranchName(URI.createURI("amor://localhost/repo1"));
    }

    @Test
    public void shouldRecognizeNextSegment1() throws Exception {
        // given
        final URI query = URI.createURI("amor://localhost/r/");
        final URI fullUri = URI.createURI("amor://localhost/r/branch/23/foo/bar");
        // when
        final URI next = uh.trimToNextSegment(query, fullUri);
        // then
        assertEquals("amor://localhost/r/branch", next.toString());
    }

    @Test
    public void shouldRecognizeNextSegment2() throws Exception {
        // given
        final URI query = URI.createURI("amor://localhost/r/branch");
        final URI fullUri = URI.createURI("amor://localhost/r/branch/23/foo/bar");
        // when
        final URI next = uh.trimToNextSegment(query, fullUri);
        // then
        assertEquals("amor://localhost/r/branch/23", next.toString());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRecognizeNextSegment3() throws Exception {
        // given
        final URI query = URI.createURI("amor://localhost/r/branch/foo/bar");
        final URI fullUri = URI.createURI("amor://localhost/r/branch/23/foo/bar");
        // when
        final URI next = uh.trimToNextSegment(query, fullUri);
        // then
    }

    @Test
    public void shouldRecognizePrefixes() throws Exception {
        // given
        final URI model = URI.createURI("amor://host/repo/branch/14/dir1/file1");
        final URI pref1 = URI.createURI("amor://host/repo/branch/14/dir1/");
        final URI pref2 = URI.createURI("amor://host/repo/branch/14/dir1");
        final URI notPref1 = URI.createURI("amor://host/repo/branch/14/dir55");
        final URI notPref2 = URI.createURI("amor://host/repo2/branch/14/");
        // when
        final boolean isPrefix1 = uh.isPrefix(pref1, model);
        final boolean isPrefix2 = uh.isPrefix(pref2, model);
        final boolean isPrefix3 = uh.isPrefix(notPref1, model);
        final boolean isPrefix4 = uh.isPrefix(notPref2, model);
        // then
        assertTrue(isPrefix1);
        assertTrue(isPrefix2);
        assertFalse(isPrefix3);
        assertFalse(isPrefix4);
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
    public void testExpectsAtLeastThreeSegments2() throws MalformedURIException {
        uh.extractRevision(URI.createURI("amor://localhost/repo1/branch1"));
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
