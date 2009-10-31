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

import org.infai.amor.backend.Branch;
import org.infai.amor.backend.internal.AbstractStorageFactory;
import org.infai.amor.backend.internal.NeoProvider;
import org.infai.amor.backend.internal.impl.NeoBranch;
import org.infai.amor.backend.storage.Storage;

/**
 * @author sdienst
 * 
 */
public class NeoBlobStorageFactory extends AbstractStorageFactory {
    private final NeoProvider neoprovider;

    /**
     * @param np
     */
    public NeoBlobStorageFactory(final NeoProvider np) {
        super();
        this.neoprovider = np;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.impl.AbstractStorageFactory#createNewStorage(java.lang.String)
     */
    @Override
    protected Storage createNewStorage(final Branch branch) {
        return new NeoBlobStorage(neoprovider, (NeoBranch) branch);
    }

}
