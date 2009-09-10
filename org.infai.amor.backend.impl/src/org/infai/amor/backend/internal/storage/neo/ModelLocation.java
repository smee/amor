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

import org.eclipse.emf.common.util.URI;
import org.infai.amor.backend.internal.NeoProvider;
import org.infai.amor.backend.internal.impl.NeoObject;
import org.neo4j.api.core.Direction;
import org.neo4j.api.core.DynamicRelationshipType;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;

/**
 * @author sdienst
 * 
 */
public class ModelLocation extends NeoObject {

    public static final String RELATIVE_PATH = "relativePath";
    private static final String EXTERNAL_URI = "externalUri";

    /**
     * Default constructor for new model location.
     * 
     * @param contentNode
     * 
     * @param node
     */
    public ModelLocation(final NeoProvider np, final Node contentNode, final String relativePath, final URI externalUri) {
        super(np);

        getNode().createRelationshipTo(contentNode, DynamicRelationshipType.withName(Constants.MODEL_HEAD));
        getNode().setProperty(RELATIVE_PATH, relativePath);
        getNode().setProperty(EXTERNAL_URI, externalUri.toString());
    }

    /**
     * Restore a previously stored model location
     * 
     * @param neoProvider
     * 
     * @param node
     */
    public ModelLocation(final Node contentNode) {
        super(contentNode);

    }

    /**
     * @return
     */
    public URI getExternalUri() {
        return URI.createURI((String) getNode().getProperty(EXTERNAL_URI));
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

    /**
     * What is the relative path component of this model?
     * 
     * @return
     */
    public String getRelativePath() {
        return (String) getNode().getProperty(RELATIVE_PATH);
    }
}
