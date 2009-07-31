/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.impl;

import org.infai.amor.backend.internal.impl.NeoBranchFactoryImplTest;
import org.infai.amor.backend.internal.impl.NeoTransactionTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * @author sdienst
 * 
 */
@RunWith(Suite.class)
@SuiteClasses( { RepositoryTests.class, NeoBranchFactoryImplTest.class, NeoTransactionTest.class })
public class AllTests {

}
