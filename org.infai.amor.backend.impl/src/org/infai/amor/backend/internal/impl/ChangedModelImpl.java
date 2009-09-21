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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.compare.epatch.Epatch;
import org.infai.amor.backend.ChangedModel;

/**
 * @author sdienst
 * 
 */
public class ChangedModelImpl implements ChangedModel {

    private final Epatch epatch;
    private final IPath path;

    public ChangedModelImpl(final Epatch p, final IPath path) {
        this.epatch = p;
        this.path = path;
    }

    public ChangedModelImpl(final Epatch p, final String path) {
        this(p, new Path(path));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.ChangedModel#getDiffModel()
     */
    @Override
    public Epatch getDiffModel() {
        return epatch;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.ChangedModel#getPath()
     */
    @Override
    public IPath getPath() {
        return path;
    }

}
