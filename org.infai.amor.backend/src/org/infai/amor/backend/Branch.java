/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend;

import java.util.Date;

/**
 * @author sdienst
 * 
 */
public interface Branch {
    /**
     * Get creation time of this branch.
     * 
     * @return
     */
    Date getCreationTime();

    /**
     * @return the name of this branch
     */
    String getName();
}
