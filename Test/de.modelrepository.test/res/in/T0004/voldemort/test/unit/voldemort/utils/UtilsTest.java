/*
 * Copyright 2008-2009 LinkedIn, Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package voldemort.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import junit.framework.TestCase;

public class UtilsTest extends TestCase {

    public void testSorted() {
        assertEquals(Collections.EMPTY_LIST, Utils.sorted(new ArrayList<Integer>()));
        assertEquals(Arrays.asList(1), Utils.sorted(Arrays.asList(1)));
        assertEquals(Arrays.asList(1, 2, 3, 4), Utils.sorted(Arrays.asList(4, 3, 2, 1)));
    }

    public void testReversed() {
        assertEquals(Collections.EMPTY_LIST, Utils.sorted(new ArrayList<Integer>()));
        assertEquals(Arrays.asList(1, 2, 3, 4), Utils.sorted(Arrays.asList(4, 3, 2, 1)));
    }

}
