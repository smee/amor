/*******************************************************************************
 * Copyright (c) 2010 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.neoblobstorage.test;

import org.infai.amor.backend.test.AbstractStorageIntegrationTest;
import org.infai.amor.backend.neo.NeoProvider;
import org.infai.amor.backend.neostorage.NeoBlobStorageFactory;
import org.infai.amor.backend.storage.StorageFactory;

/**
 * @author sdienst
 *
 */
public class NeoBlobStorageTest extends AbstractStorageIntegrationTest {

    /**
     * @param m
     */
    public NeoBlobStorageTest(final String... m) {
        super(m);
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.neoblobstorage.test.AbstractStorageIntegrationTest#getStorageFactory(org.infai.amor.backend.internal.NeoProvider)
     */
    @Override
    protected StorageFactory getStorageFactory(final NeoProvider np) {
        return new NeoBlobStorageFactory(np);
    }

}
