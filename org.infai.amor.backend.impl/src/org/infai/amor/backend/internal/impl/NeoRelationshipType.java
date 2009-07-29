/*
 * NeoRelationshipType.java
 *
 * Copyright (c) 2007 Intershop Communications AG
 */
package org.infai.amor.backend.internal.impl;

import org.neo4j.api.core.RelationshipType;

/**
 * This class is a relationship type that allows to create dynamic instances
 * at runtime.
 * 
 * @author  Peter H&auml;nsgen
 */
public class NeoRelationshipType implements RelationshipType
{
    /**
     * The name of the relationship type.
     */
    private String name;

    /**
     * The constructor.
     */
    public NeoRelationshipType(String name)
    {
        this.name = name;
    }
    
    /**
     * Returns the name of the relationship type.
     */
    public String name()
    {
        return name;
    }

    /**
     * Looks up the relationship type for the give name.
     */
    public static RelationshipType getRelationshipType(String name)
    {
        return new NeoRelationshipType(name);
    }
}
