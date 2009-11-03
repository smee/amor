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

import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.ecore.EObject;

/**
 * @author sdienst
 * 
 */
public interface Model {

    List<EObject> getContent();

    IPath getPersistencePath();

}
