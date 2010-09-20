/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.neostorage;

import org.neo4j.graphdb.RelationshipType;

/**
 * Specifies the types of Ecore specific relationships in neo4j nodespace.
 */
public enum EcoreRelationshipType implements RelationshipType {

    /** From subref node for all models to a single model. */
    RESOURCE,

    /** Association from meta element to it's instance. */
    INSTANCE,

    /** Association from container to it's containment. */
    CONTAINS,

    /** Supertype. */
    SUPER,

    /** Exception. */
    EXCEPTION,

    /** Type. */
    TYPE,

    /** EKey. */
    E_KEY,

    /** Opposite of <code>EReference</code>. */
    OPPOSITE,

    /** Default value. */
    DEFAULT_VALUE,

    /** Relationship from metamodel to a model. */
    INSTANCE_MODEL,

    /** Relationship from a typed element to its generic type. */
    GENERIC_TYPE,

    /** Relationship from a generic type to its type argument. */
    GENERIC_TYPE_ARGUMENT,

    /** Relationship from a reference point to its reference end in an m1 model. */
    REFERENCES,

    /**
     * Relationship from a reference point to its containment reference end in an m1 model.
     */
    REFERENCES_AS_CONTAINMENT,

    /** Relationship from a model to another model it depends on. */
    DEPENDS,

    /**
     * References from neomodellocation to its contents
     */
    MODEL_CONTENT,
    /**
     * Flag node that exists only if the ecore M3 model is stored.
     */
    ECORE_PACKAGE_STORED
}
