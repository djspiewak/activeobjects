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

import static org.junit.Assert.assertTrue;

import java.sql.SQLException;

import org.junit.Test;

import test.schema.Pen;
import test.schema.Person;
import test.schema.PersonLegalDefence;
import test.schema.PersonSuit;

/**
 * @author Daniel Spiewak
 */
public class RelationsCacheTest extends DataTest {
	
	@Test
	public void testOneToManyExpiryDestinationCreation() throws SQLException {
		Person person = manager.get(Person.class, personID);
		person.getPens();
		
		Pen pen = manager.create(Pen.class);
		
		SQLLogMonitor.getInstance().markWatchSQL();
		person.getPens();
		assertTrue(SQLLogMonitor.getInstance().isExecutedSQL());
		
		manager.delete(pen);
	}
	
	@Test
	public void testOneToManyExpiryDestinationDeletion() throws SQLException {
		Pen pen = manager.create(Pen.class);
		Person person = manager.get(Person.class, personID);
		person.getPens();

		manager.delete(pen);
		
		SQLLogMonitor.getInstance().markWatchSQL();
		person.getPens();
		assertTrue(SQLLogMonitor.getInstance().isExecutedSQL());
	}

	@Test
	public void testOneToManyExpiryFieldModification() throws SQLException {
		Pen pen = manager.create(Pen.class);
		Person person = manager.get(Person.class, personID);
		person.getPens();
		
		pen.setDeleted(true);
		pen.save();
		
		SQLLogMonitor.getInstance().markWatchSQL();
		person.getPens();
		assertTrue(SQLLogMonitor.getInstance().isExecutedSQL());
		
		manager.delete(pen);
	}
	
	@Test
	public void testManyToManyExpiryIntermediateCreation() throws SQLException {
		Person person = manager.get(Person.class, personID);
		person.getPersonLegalDefences();
		
		PersonSuit suit = manager.create(PersonSuit.class);
		
		SQLLogMonitor.getInstance().markWatchSQL();
		person.getPersonLegalDefences();
		assertTrue(SQLLogMonitor.getInstance().isExecutedSQL());
		
		manager.delete(suit);
	}
	
	@Test
	public void testManyToManyExpiryIntermediateDeletion() throws SQLException {
		PersonSuit suit = manager.create(PersonSuit.class);
		Person person = manager.get(Person.class, personID);
		person.getPersonLegalDefences();
		
		manager.delete(suit);
		
		SQLLogMonitor.getInstance().markWatchSQL();
		person.getPersonLegalDefences();
		assertTrue(SQLLogMonitor.getInstance().isExecutedSQL());
	}
	
	@Test
	public void testManyToManyExpiryDestinationDeletion() throws SQLException {
		PersonLegalDefence defence = manager.create(PersonLegalDefence.class);
		Person person = manager.get(Person.class, personID);
		person.getPersonLegalDefences();
		
		manager.delete(defence);
		
		SQLLogMonitor.getInstance().markWatchSQL();
		person.getPersonLegalDefences();
		assertTrue(SQLLogMonitor.getInstance().isExecutedSQL());
	}
	
	@Test
	public void testManyToManyExpiryFieldModification() throws SQLException {
		PersonSuit suit = manager.create(PersonSuit.class);
		Person person = manager.get(Person.class, personID);
		person.getPersonLegalDefences();
		
		suit.setDeleted(true);
		suit.save();
		
		SQLLogMonitor.getInstance().markWatchSQL();
		person.getPersonLegalDefences();
		assertTrue(SQLLogMonitor.getInstance().isExecutedSQL());
		
		manager.delete(suit);
	}
}
