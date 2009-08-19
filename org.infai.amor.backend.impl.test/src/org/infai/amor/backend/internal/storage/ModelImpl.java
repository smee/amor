/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.internal.storage;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.ecore.EObject;
import org.infai.amor.backend.Model;

/**
 * @author sdienst
 * 
 */
public class ModelImpl implements Model {

    private final EObject model;
    private final IPath path;

    public ModelImpl(final EObject model, final IPath path) {
        this.model = model;
        this.path = path;
    }

    public ModelImpl(final EObject model, final String path) {
        this.model = model;
        this.path = new Path(path);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Model#getContent()
     */
    @Override
    public EObject getContent() {
        return model;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.Model#getPersistencePath()
     */
    @Override
    public IPath getPersistencePath() {
        return path;
    }

}
