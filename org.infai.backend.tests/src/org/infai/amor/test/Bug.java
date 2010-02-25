/*******************************************************************************
 * Copyright (c) 2010 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.*;
import org.neo4j.kernel.EmbeddedGraphDatabase;

/**
 * see bug report at https://trac.neo4j.org/ticket/222
 * 
 * @author sdienst
 * 
 */
public class Bug {
    private EmbeddedGraphDatabase neo;

    @Before
    public void createNeo() throws IOException {
        final File tempFile = File.createTempFile("unit", "test");
        tempFile.delete();
        tempFile.mkdirs();

        neo = new EmbeddedGraphDatabase(tempFile.getAbsolutePath());
        neo.beginTx();
    }

    @Test
    public void shouldDeleteAllNodes() throws Exception {
        assertEquals(1, size(neo.getAllNodes()));
        neo.createNode().delete();
        assertEquals(1, size(neo.getAllNodes()));
    }

    @After
    public void shutDownNeo() {
        neo.shutdown();
    }
    private int size(final Iterable<?> iterable) {
        int count = 0;
        for(final Object o:iterable) {
            count++;
        }
        return count;
    }
}
