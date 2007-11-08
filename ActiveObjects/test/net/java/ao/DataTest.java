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

import java.sql.SQLException;

import net.java.ao.types.ClassType;
import net.java.ao.types.TypeManager;

import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * @author Daniel Spiewak
 */
public abstract class DataTest {
	protected static EntityManager manager;
	
	protected static int personID;
	protected static long companyID;
	
	protected static int[] penIDs;
	protected static int[] defenceIDs;
	protected static int[] suitIDs;
	
	protected static long[] coolCompanyIDs;

	@BeforeClass
	public static void setUp() throws SQLException {
		TypeManager.getInstance().addType(new ClassType());
		
		manager = new EntityManager("jdbc:derby:test_database;create=true", "sa", "jeffbridges");
		
		DataStruct data = TestUtilities.setUpEntityManager(manager);
		
		personID = data.personID;
		companyID = data.companyID;
		penIDs = data.penIDs;
		defenceIDs = data.defenceIDs;
		suitIDs = data.suitIDs;
		coolCompanyIDs = data.coolCompanyIDs;
	}

	@AfterClass
	public static void tearDown() throws SQLException {
		TestUtilities.tearDownEntityManager(manager);
		
		manager.getProvider().dispose();
	}
}
