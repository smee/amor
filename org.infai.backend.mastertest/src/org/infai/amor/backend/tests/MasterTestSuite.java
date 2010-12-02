/*******************************************************************************
 * Copyright (c) 2010 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * @author sdienst Run with the following parameters: -ea -Xmx1000M -Djava.util.logging.config.file=logging.properties
 */
@RunWith(Suite.class)
@SuiteClasses( {
    org.infai.amor.backend.neoblobstorage.test.AllTests.class,
    org.infai.amor.backend.test.AllTests.class,
    org.infai.amor.backend.filestorage.test.AllTests.class,
})
public class MasterTestSuite {

}