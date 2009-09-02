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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.api.core.Direction;
import org.neo4j.api.core.DynamicRelationshipType;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;

/**
 * @author sdienst
 * 
 */
public class NeoTransactionTest extends AbstractNeo4JTest {

    @Ignore(value = "leads to an error: Can't delete node with relationships.")
    @Test
    public void shouldDeleteDescendantsOnRemoveNode() throws Exception {
        // given
        final Node node1 = neoservice.createNode();
        final Node node2 = neoservice.createNode();
        node1.createRelationshipTo(node2, DynamicRelationshipType.withName("test"));
        // when
        node1.delete();
        tx.success();
        tx.finish();
        tx = neoservice.beginTx();
        // then
        // assertFalse(neoservice.getAllNodes().iterator().hasNext());
        for (final Node n : neoservice.getAllNodes()) {
            System.out.println(n);
        }
    }

    @Test
    public void testTransactionsAreIsolated() throws InterruptedException {
        final Node node = neoservice.createNode();
        neoservice.getReferenceNode().createRelationshipTo(node, DynamicRelationshipType.withName("test"));

        final Thread t = new Thread(new Runnable() {
            public void run() {
                neoservice.beginTx();
                // should not see this reference/node within another transaction
                final Relationship rel = neoservice.getReferenceNode().getSingleRelationship(DynamicRelationshipType.withName("test"), Direction.OUTGOING);
                assertNull(rel);
            }
        });
        t.start();
        t.join();
        // but this thread should still see it
        final Relationship rel = neoservice.getReferenceNode().getSingleRelationship(DynamicRelationshipType.withName("test"), Direction.OUTGOING);
        assertNotNull(rel);
    }
}
