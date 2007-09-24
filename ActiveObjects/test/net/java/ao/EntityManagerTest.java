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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;

import org.junit.Test;

import test.schema.Company;
import test.schema.Pen;
import test.schema.Person;

/**
 * @author Daniel Spiewak
 */
public class EntityManagerTest extends DataTest {
	
	@Test
	public void testFindCheckIDs() throws SQLException {
		Company[] coolCompanies = manager.find(Company.class, "cool = ?", true);
		
		assertEquals(coolCompanyIDs.length, coolCompanies.length);
		
		for (Company c : coolCompanies) {
			boolean found = false;
			for (long id : coolCompanyIDs) {
				if (c.getCompanyID() == id) {
					found = true;
					break;
				}
			}
			
			if (!found) {
				fail("Unable to find key=" + c.getCompanyID());
			}
		}
		
		Company[] companies = manager.find(Company.class);
		
		assertEquals(coolCompanyIDs.length + 1, companies.length);
		
		for (Company c : companies) {
			boolean found = false;
			for (long id : coolCompanyIDs) {
				if (c.getCompanyID() == id) {
					found = true;
					break;
				}
			}
			
			if (c.getCompanyID() == companyID) {
				found = true;
			}
			
			if (!found) {
				fail("Unable to find key=" + c.getCompanyID());
			}
		}
	}
	
	@Test
	public void testFindCheckPreload() throws SQLException {
		Pen[] pens = manager.find(Pen.class);
		
		SQLLogMonitor.getInstance().markWatchSQL();
		for (Pen pen : pens) {
			pen.getWidth();
		}
		assertFalse(SQLLogMonitor.getInstance().isExecutedSQL());
		
		SQLLogMonitor.getInstance().markWatchSQL();
		for (Pen pen : pens) {
			pen.getPerson();
		}
		assertTrue(SQLLogMonitor.getInstance().isExecutedSQL());
	}
	
	@Test
	public void testFindCheckDefinedPrecache() throws SQLException {
		Person[] people = manager.find(Person.class, Query.select("id,firstName,lastName"));
		
		SQLLogMonitor.getInstance().markWatchSQL();
		for (Person person : people) {
			person.getFirstName();
			person.getLastName();
		}
		assertFalse(SQLLogMonitor.getInstance().isExecutedSQL());
		
		SQLLogMonitor.getInstance().markWatchSQL();
		for (Person person : people) {
			person.getURL();
			person.getCompany();
		}
		assertTrue(SQLLogMonitor.getInstance().isExecutedSQL());
	}
	
	@Test
	public void testFindWithSQL() throws SQLException {
		Company[] coolCompanies = manager.findWithSQL(Company.class, "companyID", "SELECT companyID FROM company WHERE cool = ?", true);
		
		assertEquals(coolCompanyIDs.length, coolCompanies.length);
		
		for (Company c : coolCompanies) {
			boolean found = false;
			for (long id : coolCompanyIDs) {
				if (c.getCompanyID() == id) {
					found = true;
					break;
				}
			}
			
			if (!found) {
				fail("Unable to find key=" + c.getCompanyID());
			}
		}
		
		Company[] companies = manager.findWithSQL(Company.class, "companyID", "SELECT companyID FROM company");
		
		assertEquals(coolCompanyIDs.length + 1, companies.length);
		
		for (Company c : companies) {
			boolean found = false;
			for (long id : coolCompanyIDs) {
				if (c.getCompanyID() == id) {
					found = true;
					break;
				}
			}
			
			if (c.getCompanyID() == companyID) {
				found = true;
			}
			
			if (!found) {
				fail("Unable to find key=" + c.getCompanyID());
			}
		}
	}
	
	@Test
	public void testCount() throws SQLException {
		assertEquals(coolCompanyIDs.length, manager.count(Company.class, "cool = ?", true));
		assertEquals(penIDs.length, manager.count(Pen.class));
		assertEquals(1, manager.count(Person.class));
	}
}
