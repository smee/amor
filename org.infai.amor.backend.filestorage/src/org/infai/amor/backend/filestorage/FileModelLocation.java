/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.filestorage;

import java.util.*;

import org.eclipse.emf.common.util.URI;
import org.infai.amor.backend.ModelLocation;
import org.infai.amor.backend.Revision;
import org.infai.amor.backend.Revision.ChangeType;

import com.google.common.collect.ImmutableMap;

/**
 * @author sdienst
 *
 */
public class FileModelLocation implements ModelLocation {

    private final String rp;
    private final URI exUri;
    private final Map<String, Object> props;
    private final Revision.ChangeType ct;

    private FileModelLocation(final URI externalUri, final String relativePath, final ChangeType ct, final Map<String, Object> props) {
        this.rp = relativePath;
        this.exUri = externalUri;
        this.ct = ct;
        this.props = props;
    }

    public FileModelLocation(final URI externalUri, final String relativePath, final Revision.ChangeType ct) {
        this(externalUri, relativePath, ct, new HashMap<String, Object>());
    }
    public FileModelLocation(final URI externalUri, final String relativePath, final Revision.ChangeType ct, final Collection<String> namespaceUris) {
        this(externalUri, relativePath, ct, ImmutableMap.of(NAMESPACE_URIS, (Object) namespaceUris.toArray(new String[namespaceUris.size()])));
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.internal.ModelLocation#getChangeType()
     */
    @Override
    public ChangeType getChangeType() {
        return ct;
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.internal.ModelLocation#getExternalUri()
     */
    @Override
    public URI getExternalUri() {
        return exUri;
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.internal.ModelLocation#getCustomProperties()
     */
    @Override
    public Map<String, Object> getMetaData() {
        return props;
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.ModelLocation#getNamespaceUris()
     */
    @Override
    public Collection<String> getNamespaceUris() {
        return (Collection<String>) props.get(NAMESPACE_URIS);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.ModelLocation#getRelativePath()
     */
    @Override
    public String getRelativePath() {
        return rp;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.ModelLocation#isMetaModel()
     */
    @Override
    public boolean isMetaModel() {
        return this.props.containsKey(NAMESPACE_URIS);
    }

}
