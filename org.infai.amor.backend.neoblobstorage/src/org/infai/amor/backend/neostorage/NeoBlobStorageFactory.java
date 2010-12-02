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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcorePackage;
import org.infai.amor.backend.Branch;
import org.infai.amor.backend.neo.NeoProvider;
import org.infai.amor.backend.storage.AbstractStorageFactory;
import org.infai.amor.backend.storage.Storage;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

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
        assertEcoreIdStored();
    }

    /**
     * 
     */
    private void assertEcoreIdStored() {
        Transaction tx = neoprovider.getNeo().beginTx();
        try {

            NeoMappingDispatcher disp1 = new NeoMappingDispatcher(neoprovider);
            if (!isM3StoredYet(disp1)) {
                // checkin ecore M3 model
                Map<EObject, Node> map = new HashMap();
                disp1.setRegistry(map);
                disp1.store(EcorePackage.eINSTANCE.eResource());
                NeoMetadataDispatcher disp2 = new NeoMetadataDispatcher(neoprovider);
                disp2.setRegistry(map);
                disp2.store(EcorePackage.eINSTANCE.eResource());
            }
            tx.success();
        } finally {
            tx.finish();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.internal.impl.AbstractStorageFactory#createNewStorage(java.lang.String)
     */
    @Override
    protected Storage createNewStorage(final Branch branch) {
        return new NeoBlobStorage(neoprovider);
    }

    /**
     * @param disp
     * @return
     */
    private boolean isM3StoredYet(NeoMappingDispatcher disp) {
        return disp.findEcoreNode() != null;
    }

}
