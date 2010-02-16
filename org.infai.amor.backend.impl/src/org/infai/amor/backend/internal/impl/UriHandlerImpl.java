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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.xml.type.internal.DataValue.URI.MalformedURIException;
import org.infai.amor.backend.Branch;
import org.infai.amor.backend.CommitTransaction;
import org.infai.amor.backend.internal.UriHandler;

import com.google.common.base.Preconditions;

/**
 * URI format: amor://host/repositoryname/branchname/revisionnumber/directories.../modelname
 * 
 * @author sdienst
 * 
 */
public class UriHandlerImpl implements UriHandler {

    private final String hostName;
    private final String repoName;

    /**
     * @param hostname
     * @param reponame
     */
    public UriHandlerImpl(final String hostname, final String reponame) {
        if (hostname == null) {
            throw new IllegalArgumentException("Hostname must not be null!");
        }
        if (reponame == null) {
            throw new IllegalArgumentException("Name of the repository must not be null!");
        }
        this.hostName = hostname;
        this.repoName = reponame;
    }

    /**
     * @param uri
     * @param numSegments
     * @param msg
     * @throws MalformedURIException
     */
    private void assertValidUri(final URI uri, final int numSegments, final String msg) throws MalformedURIException {
        if (uri.segmentCount() < numSegments) {
            throw new MalformedURIException(msg + ": " + uri.toString());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.UriHandler#createModelUri(org.infai.amor.backend.CommitTransaction, java.lang.String)
     */
    @Override
    public URI createModelUri(final CommitTransaction tr, final IPath modelPath) {
        final URI uri = createUriFor(tr);

        if (modelPath != null && !modelPath.isAbsolute()) {
            return uri.appendSegments(modelPath.segments());
        } else {
            throw new IllegalArgumentException("The given path must be relative for storing a model, was absolute: " + modelPath);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.UriHandler#createUriFor(org.infai.amor.backend.Branch)
     */
    @Override
    public URI createUriFor(final Branch branch) {
        return URI.createHierarchicalURI("amor", hostName, null, new String[] { repoName, branch.getName() }, null, null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.UriHandler#createUriFor(org.infai.amor.backend.Branch, long)
     */
    @Override
    public URI createUriFor(final Branch branch, final long revisionId) {
        return URI.createHierarchicalURI("amor", hostName, null, new String[] { repoName, branch.getName(), Long.toString(revisionId) }, null, null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.UriHandler#createUriFor(org.infai.amor.backend.Branch, long,
     * org.eclipse.core.runtime.IPath)
     */
    @Override
    public URI createUriFor(final Branch branch, final long revisionId, final IPath path) {
        final List<String> uriSegments = new ArrayList<String>();
        uriSegments.add(repoName);
        uriSegments.add(branch.getName());
        uriSegments.add(Long.toString(revisionId));
        for (final String segment : path.segments()) {
            uriSegments.add(segment);
        }

        return URI.createHierarchicalURI("amor", hostName, null, uriSegments.toArray(new String[uriSegments.size()]), null, null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.UriHandler#createUriFor(org.infai.amor.backend.CommitTransaction)
     */
    @Override
    public URI createUriFor(final CommitTransaction tr) {
        return createUriFor(tr.getBranch(), tr.getRevision().getRevisionId());
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.internal.UriHandler#createUriFor(java.lang.String)
     */
    @Override
    public URI createUriFor(final String branchName) {
        return URI.createHierarchicalURI("amor", hostName, null, new String[] { repoName, branchName }, null, null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.UriHandler#extractBranchName(org.eclipse.emf.common.util.URI)
     */
    @Override
    public String extractBranchName(final URI uri) throws MalformedURIException {
        assertValidUri(uri, 2, "This uri doesn't contain a branch name");

        final String name = uri.segment(1);
        if (StringUtils.isEmpty(name)) {
            throw new MalformedURIException("This uri doesn't contain a branch name: " + uri);
        } else {
            return name;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.UriHandler#extractModelPathFrom(org.eclipse.emf.common.util.URI)
     */
    @Override
    public IPath extractModelPathFrom(final URI uri) throws MalformedURIException {
        assertValidUri(uri, 4, "This uri doesn't contain any model paths or name");

        final String[] modelPath = new String[uri.segmentCount() - 3];
        // copy all segments after the revision number
        System.arraycopy(uri.segments(), 3, modelPath, 0, uri.segmentCount() - 3);
        return new Path(StringUtils.join(modelPath, '/'));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.UriHandler#extractRevision(org.eclipse.emf.common.util.URI)
     */
    @Override
    public long extractRevision(final URI uri) throws MalformedURIException {
        assertValidUri(uri, 3, "This uri doesn't contain a revision number");

        return Long.parseLong(uri.segment(2));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.UriHandler#isPrefix(org.eclipse.emf.common.util.URI, org.eclipse.emf.common.util.URI)
     */
    public boolean isPrefix(final URI prefixUri, final URI fullUri) {
        return fullUri.toString().startsWith(prefixUri.toString());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.UriHandler#isPrefixIgnoringRevision(org.eclipse.emf.common.util.URI,
     * org.eclipse.emf.common.util.URI)
     */
    public boolean isPrefixIgnoringRevision(final URI prefixUri, final URI fullUri) {
        Preconditions.checkState(prefixUri.segmentCount() >= 3, "No revision id in uri '%s'", prefixUri);
        Preconditions.checkState(fullUri.segmentCount() >= 3, "No revision id in uri '%s'", fullUri);

        for (int i = 0; i < prefixUri.segmentCount(); i++) {
            if (i == 2) {
                continue;
            }
            if (!prefixUri.segment(i).equals(fullUri.segment(i))) {
                return false;
            }
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.UriHandler#trimToNextSegment(org.eclipse.emf.common.util.URI,
     * org.eclipse.emf.common.util.URI)
     */
    @Override
    public URI trimToNextSegment(final URI prefix, final URI fullUri) {
        Preconditions.checkState(prefix.segmentCount() < fullUri.segmentCount(), "'%s' can't be a prefix of '%s' at all!", prefix, fullUri);
        Preconditions.checkState(fullUri.toString().startsWith(prefix.toString()), "'%s' can't be a prefix of '%s' at all!", prefix, fullUri);

        if (!prefix.hasTrailingPathSeparator()) {
            return prefix.appendSegment(fullUri.segment(prefix.segmentCount()));
        } else {
            return URI.createURI(prefix.toString() + fullUri.segment(prefix.segmentCount() - 1));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.UriHandler#trimToNextSegmentKeepingRevision(org.eclipse.emf.common.util.URI,
     * org.eclipse.emf.common.util.URI)
     */
    @Override
    public URI trimToNextSegmentKeepingRevision(final int segmentIdx, final URI fullUri) {
        Preconditions.checkState(segmentIdx >= 3, "We need at least 3 uri segments to have a revision number!");

        final String[] segments = new String[segmentIdx];
        System.arraycopy(fullUri.segments(), 0, segments, 0, segmentIdx);
        return URI.createHierarchicalURI(fullUri.scheme(), fullUri.authority(), fullUri.device(), segments, fullUri.query(), fullUri.fragment());

    }
}
