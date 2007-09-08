/*
 * Copyright 2007 Daniel Spiewak
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 *	    http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.ao;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author Daniel Spiewak
 */
public class BaseTests extends TestUtilities {
	public static Test suite() {
		TestSuite suite = new TestSuite("Tests for net.java.ao");
		//$JUnit-BEGIN$
		suite.addTest(NameConverterTests.suite());
		
		suite.addTest(asTest(EntityTest.class));
		suite.addTest(asTest(RelationsCacheTest.class));
		//$JUnit-END$
		return suite;
	}
}
