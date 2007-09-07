import junit.framework.Test;
import junit.framework.TestSuite;
import net.java.ao.BaseTests;
import net.java.ao.db.DBTests;
import net.java.ao.schema.SchemaTests;
import net.java.ao.types.TypeTests;

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

/**
 * @author Daniel Spiewak
 */
public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("All tests for ActiveObjects ORM");
		//$JUnit-BEGIN$
		suite.addTest(BaseTests.suite());
		suite.addTest(DBTests.suite());
		suite.addTest(SchemaTests.suite());
		suite.addTest(TypeTests.suite());
		//$JUnit-END$
		return suite;
	}

}
