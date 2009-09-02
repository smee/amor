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

/**
 * @author sdienst
 * 
 */
public interface CommitTransaction {
    /**
     * @return
     */
    Branch getBranch();

    /**
     * @return
     */
    String getCommitMessage();

    /**
     * @return
     */
    long getRevisionId();

    /**
     * @return
     */
    String getUser();

    /**
     * @param message
     */
    void setCommitMessage(String message);

    /**
     * @param username
     */
    void setUser(String username);
}
