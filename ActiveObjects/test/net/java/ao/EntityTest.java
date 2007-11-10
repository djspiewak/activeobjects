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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.Test;

import test.schema.Company;
import test.schema.Pen;
import test.schema.Person;
import test.schema.PersonImpl;
import test.schema.PersonLegalDefence;

/**
 * @author Daniel Spiewak
 */
public class EntityTest extends DataTest {
	
	@Test
	public void testDatabaseAccessor() {
		Person person = manager.get(Person.class, personID);
		
		assertEquals("Daniel", person.getFirstName());
		
		assertEquals(companyID, person.getCompany().getCompanyID());
		assertEquals("Company Name", person.getCompany().getName());
		assertEquals(false, person.getCompany().isCool());
	}
	
	@Test
	public void testCacheAccessor() {
		Person person = manager.get(Person.class, personID);
		
		person.getFirstName();
		Company c = person.getCompany();
		c.getName();
		c.isCool();
		
		SQLLogMonitor.getInstance().markWatchSQL();
		
		assertEquals("Daniel", person.getFirstName());
		
		assertEquals(companyID, person.getCompany().getCompanyID());
		assertEquals("Company Name", person.getCompany().getName());
		assertEquals(false, person.getCompany().isCool());
		
		assertFalse(SQLLogMonitor.getInstance().isExecutedSQL());
	}
	
	@Test
	public void testCacheMutator() throws SQLException {
		Company company = manager.create(Company.class);
		
		SQLLogMonitor.getInstance().markWatchSQL();
		
		company.setName("Another company name");
		company.setCool(true);
		
		assertFalse(SQLLogMonitor.getInstance().isExecutedSQL());
		assertEquals("Another company name", company.getName());
		assertEquals(true, company.isCool());
		
		company.setName(null);
		assertNull(company.getName());
		
		manager.delete(company);
	}
	
	@Test
	public void testSave() throws SQLException {
		Company company = manager.create(Company.class);
		
		company.setName("Another company name");
		company.setCool(true);
		
		SQLLogMonitor.getInstance().markWatchSQL();
		company.save();
		assertTrue(SQLLogMonitor.getInstance().isExecutedSQL());
		
		String name = null;
		boolean cool = false;
		Connection conn = manager.getProvider().getConnection();
		try {
			PreparedStatement stmt = conn.prepareStatement("SELECT name,cool FROM company WHERE companyID = ?");
			stmt.setLong(1, company.getCompanyID());
			
			ResultSet res = stmt.executeQuery();
			if (res.next()) {
				name = res.getString("name");
				cool = res.getBoolean("cool");
			}
			res.close();
			stmt.close();
		} finally {
			conn.close();
		}
		
		assertEquals("Another company name", name);
		assertEquals(true, cool);
		
		company.setName(null);
		
		SQLLogMonitor.getInstance().markWatchSQL();
		company.save();
		assertTrue(SQLLogMonitor.getInstance().isExecutedSQL());
		
		conn = manager.getProvider().getConnection();
		try {
			PreparedStatement stmt = conn.prepareStatement("SELECT name,cool FROM company WHERE companyID = ?");
			stmt.setLong(1, company.getCompanyID());
			
			ResultSet res = stmt.executeQuery();
			if (res.next()) {
				name = res.getString("name");
				cool = res.getBoolean("cool");
			}
			res.close();
			stmt.close();
		} finally {
			conn.close();
		}
		
		assertNull(name);
		assertEquals(true, cool);
		
		manager.delete(company);
	}
	
	@Test
	public void testCreate() throws SQLException {
		SQLLogMonitor.getInstance().markWatchSQL();
		Company company = manager.create(Company.class);
		assertTrue(SQLLogMonitor.getInstance().isExecutedSQL());
		
		Connection conn = manager.getProvider().getConnection();
		try {
			PreparedStatement stmt = conn.prepareStatement("SELECT companyID FROM company WHERE companyID = ?");
			stmt.setLong(1, company.getCompanyID());
			
			ResultSet res = stmt.executeQuery();
			if (!res.next()) {
				fail("Unable to find INSERTed company row");
			}
			res.close();
			stmt.close();
		} finally {
			conn.close();
		}
		
		manager.delete(company);
		
		SQLLogMonitor.getInstance().markWatchSQL();
		Person person = manager.create(Person.class, new DBParam("url", "http://www.codecommit.com"));
		assertTrue(SQLLogMonitor.getInstance().isExecutedSQL());
		
		conn = manager.getProvider().getConnection();
		try {
			PreparedStatement stmt = conn.prepareStatement("SELECT id FROM person WHERE id = ?");
			stmt.setInt(1, person.getID());
			
			ResultSet res = stmt.executeQuery();
			if (!res.next()) {
				fail("Unable to find INSERTed person row");
			}
			res.close();
			stmt.close();
		} finally {
			conn.close();
		}
		
		manager.delete(person);
	}
	
	@Test
	public void testStringGenerate() throws SQLException {
		Company company = manager.create(Company.class);
		
		Connection conn = manager.getProvider().getConnection();
		try {
			PreparedStatement stmt = conn.prepareStatement("SELECT motivation FROM company WHERE companyID = ?");
			stmt.setLong(1, company.getCompanyID());
			
			ResultSet res = stmt.executeQuery();
			if (res.next()) {
				assertEquals("Work smarter, not harder", res.getString("motivation"));
			} else {
				fail("Unable to find INSERTed company row");
			}
			res.close();
			stmt.close();
		} finally {
			conn.close();
		}
		
		manager.delete(company);
	}
	
	@Test
	public void testDelete() throws SQLException {
		Company company = manager.create(Company.class);
		
		SQLLogMonitor.getInstance().markWatchSQL();
		manager.delete(company);
		assertTrue(SQLLogMonitor.getInstance().isExecutedSQL());
		
		Connection conn = manager.getProvider().getConnection();
		try {
			PreparedStatement stmt = conn.prepareStatement("SELECT companyID FROM company WHERE companyID = ?");
			stmt.setLong(1, company.getCompanyID());
			
			ResultSet res = stmt.executeQuery();
			if (res.next()) {
				fail("Row was not deleted");
			}
			res.close();
			stmt.close();
		} finally {
			conn.close();
		}
	}
	
	@Test
	public void testDefinedImplementation() {
		Person person = manager.get(Person.class, personID);
		
		PersonImpl.enableOverride = true;
		
		SQLLogMonitor.getInstance().markWatchSQL();
		assertEquals("Smith", person.getLastName());
		assertFalse(SQLLogMonitor.getInstance().isExecutedSQL());
		
		PersonImpl.enableOverride = false;
	}
	
	// if this test doesn't stack overflow, we're good
	@Test
	public void testDefinedImplementationRecursion() {
		Person person = manager.get(Person.class, personID);
		person.setLastName("Jameson");
	}
	
	@Test
	public void testOneToManyRetrievalIDs() {
		Person person = manager.get(Person.class, personID);
		Pen[] pens = person.getPens();
		
		assertEquals(penIDs.length, pens.length);
		
		for (Pen pen : pens) {
			boolean found = false;
			for (int id : penIDs) {
				if (pen.getID() == id) {
					found = true;
					break;
				}
			}
			
			if (!found) {
				fail("Unable to find id=" + pen.getID());
			}
		}
	}
	
	@Test
	public void testOneToManyRetrievalPreload() {
		Person person = manager.get(Person.class, personID);
		
		for (Pen pen : person.getPens()) {
			SQLLogMonitor.getInstance().markWatchSQL();
			pen.getWidth();
			assertFalse(SQLLogMonitor.getInstance().isExecutedSQL());
		}
	}
	
	@Test
	public void testOneToManyRetrievalFromCache() {
		Person person = manager.get(Person.class, personID);
		person.getPens();
		
		SQLLogMonitor.getInstance().markWatchSQL();
		person.getPens();
		assertFalse(SQLLogMonitor.getInstance().isExecutedSQL());
	}
	
	@Test
	public void testManyToManyRetrievalIDs() {
		Person person = manager.get(Person.class, personID);
		PersonLegalDefence[] defences = person.getPersonLegalDefences();
		
		assertEquals(defenceIDs.length, defences.length);
		
		for (PersonLegalDefence defence : defences) {
			boolean found = false;
			for (int id : defenceIDs) {
				if (defence.getID() == id) {
					found = true;
					break;
				}
			}
			
			if (!found) {
				fail("Unable to find id=" + defence.getID());
			}
		}
	}
	
	@Test
	public void testManyToManyRetrievalPreload() {
		Person person = manager.get(Person.class, personID);
		
		for (PersonLegalDefence defence : person.getPersonLegalDefences()) {
			SQLLogMonitor.getInstance().markWatchSQL();
			defence.getSeverity();
			assertFalse(SQLLogMonitor.getInstance().isExecutedSQL());
		}
	}
	
	@Test
	public void testManyToManyRetrievalFromCache() {
		Person person = manager.get(Person.class, personID);
		person.getPersonLegalDefences();
		
		SQLLogMonitor.getInstance().markWatchSQL();
		person.getPersonLegalDefences();
		assertFalse(SQLLogMonitor.getInstance().isExecutedSQL());
	}
}
