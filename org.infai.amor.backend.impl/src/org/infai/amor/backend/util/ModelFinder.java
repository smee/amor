/*******************************************************************************
 * Copyright (c) 2010 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.util;

import org.infai.amor.backend.*;

/**
 * @author sdienst
 *
 */
public class ModelFinder {
    /**
     * Critera for finding a modellocation.
     * 
     * @author sdienst
     * 
     */
    public static interface ModelMatcher {
        boolean matches(ModelLocation loc);
    }

    /**
     * Search all revisions upto startRev for a {@link ModelLocation} that matches. If the most recent matching model got deleted,
     * returns null.
     * 
     * @param startRev
     * @param mm
     * @return
     */
    public static ModelLocation findActiveModel(final Revision startRev, final ModelMatcher mm) {
        Revision rev = startRev;
        while (rev != null) {
            for (final ModelLocation loc : rev.getModelReferences(ChangeType.ADDED, ChangeType.CHANGED, ChangeType.DELETED)) {
                if (mm.matches(loc)) {
                    if(loc.getChangeType()==ChangeType.DELETED) {
                        return null;
                    } else {
                        return loc;
                    }
                }
            }
            rev = rev.getPreviousRevision();
        }
        return null;
    }

    /**
     * @param startRev
     * @param mm
     * @return
     */
    public static ModelLocation findModel(final Revision startRev, final ModelMatcher mm) {
        Revision rev = startRev;
        while (rev != null) {
            for (final ModelLocation loc : rev.getModelReferences(ChangeType.ADDED, ChangeType.CHANGED, ChangeType.DELETED)) {
                if (mm.matches(loc)) {
                    return loc;
                }
            }
            rev = rev.getPreviousRevision();
        }
        return null;
    }

}
