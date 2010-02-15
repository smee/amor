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

import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.infai.amor.backend.ModelLocation;
import org.infai.amor.backend.Revision;
import org.infai.amor.backend.Revision.ChangeType;
import org.infai.amor.backend.internal.NeoProvider;
import org.neo4j.api.core.*;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

/**
 * @author sdienst
 * 
 */
public class NeoModelLocation extends NeoObject implements ModelLocation {


    private static final String CUSTOMPROPERTIES = "customProperties";
    private static final String CHANGETYPE = "changetype";
    private static final String MODEL_HEAD = "modelHead";

    /**
     * Restore a previously stored model location
     * 
     * @param neoProvider
     * 
     * @param node
     */
    public NeoModelLocation(final NeoProvider np,final Node contentNode) {
        super(np, contentNode);
    }

    /**
     * Wrap another {@link ModelLocation} implementation.
     * 
     * @param np
     * @param contentNode
     * @param loc
     */
    public NeoModelLocation(final NeoProvider np, final Node contentNode,final ModelLocation loc){
        this(np, contentNode, loc.getRelativePath(), loc.getExternalUri(), loc.getChangeType());
        storeCustomProperties(loc.getMetaData());
    }

    /**
     * Default constructor for new model location.
     * 
     * @param contentNode
     * @param changeType
     * 
     * @param node
     */
    public NeoModelLocation(final NeoProvider np, final Node contentNode, final String relativePath, final URI externalUri, final ChangeType changeType) {
        this(np, contentNode);

        setRelativePath(relativePath);
        if (externalUri != null) {
            setExternalUri(externalUri);
        }
        setChangetype(changeType);
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.internal.ModelLocation#getChangeType()
     */
    @Override
    public ChangeType getChangeType() {
        return Revision.ChangeType.valueOf((String) get(CHANGETYPE));
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.internal.ModelLocation#getCustomProperties()
     */
    @Override
    public Map<String, Object> getMetaData() {
        final Node propertiesNode = getNode().getSingleRelationship(DynamicRelationshipType.withName(CUSTOMPROPERTIES), Direction.OUTGOING).getEndNode();

        final Builder<String, Object> mb = new ImmutableMap.Builder<String, Object>();
        for (final String key : propertiesNode.getPropertyKeys()) {
            mb.put(key, propertiesNode.getProperty(key));
        }
        return mb.build();
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.internal.storage.neo.ModelLocation#getExternalUri()
     */
    public URI getExternalUri() {
        if (getNode().hasProperty(EXTERNAL_URI)) {
            return URI.createURI((String) get(EXTERNAL_URI));
        } else {
            return null;
            // TODO nodes fuer modellocations und Changetypespezifische Knoten ueberpruefen, ist inkonsistent!
        }
    }

    /**
     * @return
     */
    public Node getModelHead() {
        return getNode();
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.internal.storage.neo.ModelLocation#getRelativePath()
     */
    public String getRelativePath() {
        return (String) get(RELATIVE_PATH);
    }

    /**
     * @param added
     */
    public void setChangetype(final ChangeType changeType) {
        set(CHANGETYPE, changeType.name());
    }

    /**
     * 
     */
    public void setExternalUri(final URI uri){
        set(EXTERNAL_URI, uri.toString());
    }

    /**
     * @param relPath
     */
    public void setRelativePath(final String relPath){
        set(RELATIVE_PATH,relPath);
    }

    /**
     * @param customProperties
     */
    private void storeCustomProperties(final Map<String, Object> customProperties) {
        final Node propertiesNode = createNode();
        getNode().createRelationshipTo(propertiesNode, DynamicRelationshipType.withName(CUSTOMPROPERTIES));
        for (final String key : customProperties.keySet()) {
            propertiesNode.setProperty(key, customProperties.get(key));
        }

    }
}
