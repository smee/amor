/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.internal.storage.neo;

import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.infai.amor.backend.Revision;
import org.infai.amor.backend.Revision.ChangeType;
import org.infai.amor.backend.internal.ModelLocation;
import org.infai.amor.backend.internal.NeoProvider;
import org.infai.amor.backend.internal.impl.NeoObject;
import org.neo4j.api.core.Direction;
import org.neo4j.api.core.DynamicRelationshipType;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

/**
 * @author sdienst
 * 
 */
public class NeoModelLocation extends NeoObject implements ModelLocation {


    private static final String CUSTOMPROPERTIES = "customProperties";
    private static final String CHANGETYPE = "changetype";

    /**
     * @param np
     * @param contentNode
     * @param loc
     */
    public NeoModelLocation(final NeoProvider np, final Node contentNode,final ModelLocation loc){
        this(np, contentNode, loc.getRelativePath(), loc.getExternalUri(), loc.getChangeType());
        storeCustomProperties(loc.getCustomProperties());
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
        super(np);

        getNode().createRelationshipTo(contentNode, DynamicRelationshipType.withName(Constants.MODEL_HEAD));
        set(RELATIVE_PATH, relativePath);
        if (externalUri != null) {
            set(EXTERNAL_URI, externalUri.toString());
        }
        set(CHANGETYPE, changeType.name());
    }

    /**
     * Restore a previously stored model location
     * 
     * @param neoProvider
     * 
     * @param node
     */
    public NeoModelLocation(final Node contentNode) {
        super(contentNode);

    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.internal.ModelLocation#getChangeType()
     */
    @Override
    public ChangeType getChangeType() {
        return Revision.ChangeType.valueOf((String) getNode().getProperty(CHANGETYPE));
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.internal.ModelLocation#getCustomProperties()
     */
    @Override
    public Map<String, Object> getCustomProperties() {
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
            return URI.createURI((String) getNode().getProperty(EXTERNAL_URI));
        } else {
            return null;
            // TODO nodes fuer modellocations und Changetypespezifische Knoten ueberpruefen, ist inkonsistent!
        }
    }

    /**
     * @return
     */
    public Node getModelHead() {
        final Node modelHeadNode = getNode().getSingleRelationship(DynamicRelationshipType.withName(Constants.MODEL_HEAD), Direction.OUTGOING).getEndNode();
        final Relationship isInstanceModelRel = modelHeadNode.getSingleRelationship(EcoreRelationshipType.CONTAINS, Direction.INCOMING);
        if (isInstanceModelRel != null) {
            return isInstanceModelRel.getOtherNode(modelHeadNode);
        } else {
            return modelHeadNode;
        }
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.internal.storage.neo.ModelLocation#getRelativePath()
     */
    public String getRelativePath() {
        return (String) getNode().getProperty(RELATIVE_PATH);
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
