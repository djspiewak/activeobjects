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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.java.ao.types.ClassType;
import net.java.ao.types.TypeManager;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.store.FSDirectory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.schema.Company;
import test.schema.Person;

/**
 * @author Daniel Spiewak
 */
public class SearchTest {
	private static final File TEST_INDEX = new File("test_index");
	
	protected static SearchableEntityManager manager;
	
	protected static int personID;
	protected static long companyID;
	
	protected static int[] penIDs;
	protected static int[] defenceIDs;
	protected static int[] suitIDs;
	
	protected static long[] coolCompanyIDs;

	@BeforeClass
	public static void setUp() throws IOException, SQLException {
		TypeManager.getInstance().addType(new ClassType());

		SearchableEntityManager.asynchronous = false;
		manager = new SearchableEntityManager("jdbc:hsqldb:mem:test_database", "sa", "", 
				FSDirectory.getDirectory(TEST_INDEX));
//		manager = new SearchableEntityManager("jdbc:derby:test_database;create=true", "sa", "jeffbridges", 
//				FSDirectory.getDirectory(TEST_INDEX));
//		manager = new SearchableEntityManager("jdbc:oracle:thin:@192.168.101.17:1521:xe", "activeobjects", "password", 
//				FSDirectory.getDirectory(TEST_INDEX));

		DataStruct data = TestUtilities.setUpEntityManager(manager);

		personID = data.personID;
		companyID = data.companyID;
		penIDs = data.penIDs;
		defenceIDs = data.defenceIDs;
		suitIDs = data.suitIDs;
		coolCompanyIDs = data.coolCompanyIDs;

		Map<String, String[]> people = new LinkedHashMap<String, String[]>() {
			{
				put("Daniel", new String[] {"Spiewak", "http://www.codecommit.com"});
				put("Christopher", new String[] {"Spiewak", "http://www.weirdthings.com"});

				put("Jack", new String[] {"O'Neil", "http://www.gateworld.net"});
				put("Katheryn", new String[] {"Janeway", "http://www.startrek.com"});

				put("Martin", new String[] {"Smith", "http://www.delirious.com"});
				put("Steve", new String[] {"Smith", "http://www.panthers.com"});
				put("Craig", new String[] {"Antler", "http://www.rockyandbullwinkle.com"});
				put("Greg", new String[] {"Smith", "http://www.imagination.com"});
			}
		};
		String[] companies = {"Miller", "Chrysler", "Apple", "GM", "HP", "Dell", "Wal-Mart", 
				"Ma Bell", "Sell, Sell, Sell", "Viacom"};

		for (String firstName : people.keySet()) {
			Person person = manager.create(Person.class, new DBParam("url", people.get(firstName)[1]));
			person.setFirstName(firstName);
			person.setLastName(people.get(firstName)[0]);
			person.save();
		}

		for (String name : companies) {
			Company company = manager.create(Company.class);
			company.setName(name);
			company.save();
		}
	}
	
	@Test
	public void testSearchable() {
		List<String> fields = Common.getSearchableFields(manager, Company.class);
		
		assertEquals(1, fields.size());
		assertEquals("name", fields.get(0));
		
		List<String> possibilities = Arrays.asList(new String[] {"firstName", "lastName"});
		fields = Common.getSearchableFields(manager, Person.class);
		
		assertEquals(possibilities.size(), fields.size());

		Collections.sort(fields);
		for (int i = 0; i < possibilities.size(); i++) {
			assertEquals(possibilities.get(i), fields.get(i));
		}
	}
	
	@Test
	public void testSearch() throws SQLException, IOException, ParseException {
		Person[] people = manager.search(Person.class, "Spiewak");
		Map<String, String> resultsMap = new HashMap<String, String>() {
			{
				put("Daniel", "Spiewak");
				put("Christopher", "Spiewak");
			}
		};
		
		assertEquals(resultsMap.size(), people.length);
		for (Person p : people) {
			assertNotNull(resultsMap.get(p.getFirstName()));
			assertEquals(resultsMap.get(p.getFirstName()), p.getLastName());
			
			resultsMap.remove(p.getFirstName());
		}
		assertEquals(0, resultsMap.size());
		
		people = manager.search(Person.class, "martin");
		resultsMap = new HashMap<String, String>() {
			{
				put("Martin", "Smith");
			}
		};
		
		assertEquals(resultsMap.size(), people.length);
		for (Person p : people) {
			assertNotNull(resultsMap.get(p.getFirstName()));
			assertEquals(resultsMap.get(p.getFirstName()), p.getLastName());
			
			resultsMap.remove(p.getFirstName());
		}
		assertEquals(0, resultsMap.size());
		
		people = manager.search(Person.class, "sMitH");
		resultsMap = new HashMap<String, String>() {
			{
				put("Martin", "Smith");
				put("Steve", "Smith");
				put("Greg", "Smith");
			}
		};
		
		assertEquals(resultsMap.size(), people.length);
		for (Person p : people) {
			assertNotNull(resultsMap.get(p.getFirstName()));
			assertEquals(resultsMap.get(p.getFirstName()), p.getLastName());
			
			resultsMap.remove(p.getFirstName());
		}
		assertEquals(0, resultsMap.size());
		
		Company[] companies = manager.search(Company.class, "miller");
		Set<String> resultSet = new HashSet<String>() {
			{
				add("Miller");
			}
		};
		
		assertEquals(resultSet.size(), companies.length);
		for (Company c : companies) {
			assertTrue(resultSet.contains(c.getName()));
			resultSet.remove(c.getName());
		}
		assertEquals(0, resultSet.size());
		
		companies = manager.search(Company.class, "deLL sell");
		resultSet = new HashSet<String>() {
			{
				add("Dell");
				add("Sell, Sell, Sell");
			}
		};
		
		assertEquals(resultSet.size(), companies.length);
		for (Company c : companies) {
			assertTrue(resultSet.contains(c.getName()));
			resultSet.remove(c.getName());
		}
		assertEquals(0, resultSet.size());
		
		companies = manager.search(Company.class, "vaguesearchofnothingatall");
		resultSet = new HashSet<String>();
		
		assertEquals(resultSet.size(), companies.length);
		for (Company c : companies) {
			assertTrue(resultSet.contains(c.getName()));
			resultSet.remove(c.getName());
		}
		assertEquals(0, resultSet.size());
	}
	
	@Test
	public void testDelete() throws SQLException, IOException, ParseException {
		assertEquals(0, manager.search(Person.class, "foreman").length);
		
		Person person = manager.create(Person.class, new DBParam("url", "http://en.wikipedia.org"));
		person.setFirstName("George");
		person.setLastName("Foreman");
		person.save();
		
		assertEquals(1, manager.search(Person.class, "foreman").length);
		
		manager.delete(person);
		
		assertEquals(0, manager.search(Person.class, "foreman").length);
	}
	
	@Test
	public void testAddToIndex() throws SQLException, IOException, ParseException {
		assertEquals(0, manager.search(Person.class, "foreman").length);
		
		Person person = manager.create(Person.class, new DBParam("url", "http://en.wikipedia.org"));
		person.setFirstName("George");
		person.setLastName("Foreman");
		person.save();
		
		assertEquals(1, manager.search(Person.class, "foreman").length);
		
		manager.removeFromIndex(person);
		assertEquals(0, manager.search(Person.class, "foreman").length);
		
		manager.addToIndex(person);
		assertEquals(1, manager.search(Person.class, "foreman").length);
		
		manager.delete(person);
		assertEquals(0, manager.search(Person.class, "foreman").length);
	}
	
	@Test
	public void testRemoveFromIndex() throws SQLException, IOException, ParseException {
		assertEquals(0, manager.search(Person.class, "foreman").length);
		
		Person person = manager.create(Person.class, new DBParam("url", "http://en.wikipedia.org"));
		person.setFirstName("George");
		person.setLastName("Foreman");
		person.save();
		
		assertEquals(1, manager.search(Person.class, "foreman").length);
		
		manager.removeFromIndex(person);
		assertEquals(0, manager.search(Person.class, "foreman").length);
		
		manager.delete(person);
		assertEquals(0, manager.search(Person.class, "foreman").length);
	}
	
	@Test
	public void testOptimize() throws CorruptIndexException, IOException {
		IndexReader reader = IndexReader.open(manager.getIndexDir());
		assertFalse(reader.isOptimized());
		reader.close();
		
		manager.optimize();

		reader = IndexReader.open(manager.getIndexDir());
		assertTrue(reader.isOptimized());
		reader.close();
	}

	@AfterClass
	public static void tearDown() throws SQLException {
		TestUtilities.tearDownEntityManager(manager);
		
		manager.getProvider().dispose();
		deleteDir(TEST_INDEX);

		SearchableEntityManager.asynchronous = true;
	}
	
	private static void deleteDir(File dir) {
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				deleteDir(file);
			} else {
				file.delete();
			}
		}
		dir.delete();
	}
}
