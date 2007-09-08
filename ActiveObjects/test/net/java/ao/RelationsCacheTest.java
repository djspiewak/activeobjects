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
	public void testOneToManyDestinationCreation() throws SQLException {
		Person person = manager.get(Person.class, personID);
		person.getPens();
		
		Pen pen = manager.create(Pen.class);
		
		SQLLogMonitor.getInstance().markWatchSQL();
		person.getPens();
		assertTrue(SQLLogMonitor.getInstance().isExecutedSQL());
		
		manager.delete(pen);
	}
	
	@Test
	public void testOneToManyDestinationDeletion() throws SQLException {
		Pen pen = manager.create(Pen.class);
		Person person = manager.get(Person.class, personID);
		person.getPens();

		manager.delete(pen);
		
		SQLLogMonitor.getInstance().markWatchSQL();
		person.getPens();
		assertTrue(SQLLogMonitor.getInstance().isExecutedSQL());
	}

	@Test
	public void testOneToManyFieldModification() throws SQLException {
		Person person = manager.get(Person.class, personID);
		Pen pen = person.getPens()[0];
		
		pen.setDeleted(true);
		pen.save();
		
		SQLLogMonitor.getInstance().markWatchSQL();
		Pen pen2 = person.getPens()[0];
		assertTrue(SQLLogMonitor.getInstance().isExecutedSQL());
		
		pen2.setPerson(null);
		pen2.save();
		
		SQLLogMonitor.getInstance().markWatchSQL();
		person.getPens();
		assertTrue(SQLLogMonitor.getInstance().isExecutedSQL());
		
		pen2.setPerson(person);
		pen2.save();
		
		pen.setDeleted(false);
		pen.save();
	}
	
	@Test
	public void testManyToManyIntermediateCreation() throws SQLException {
		Person person = manager.get(Person.class, personID);
		person.getPersonLegalDefences();
		
		PersonSuit suit = manager.create(PersonSuit.class);
		
		SQLLogMonitor.getInstance().markWatchSQL();
		person.getPersonLegalDefences();
		assertTrue(SQLLogMonitor.getInstance().isExecutedSQL());
		
		manager.delete(suit);
	}
	
	@Test
	public void testManyToManyIntermediateDeletion() throws SQLException {
		PersonSuit suit = manager.create(PersonSuit.class);
		Person person = manager.get(Person.class, personID);
		person.getPersonLegalDefences();
		
		manager.delete(suit);
		
		SQLLogMonitor.getInstance().markWatchSQL();
		person.getPersonLegalDefences();
		assertTrue(SQLLogMonitor.getInstance().isExecutedSQL());
	}
	
	@Test
	public void testManyToManyFieldModification() throws SQLException {
		Person person = manager.get(Person.class, personID);
		PersonLegalDefence defence = person.getPersonLegalDefences()[0];
		PersonSuit suit = manager.get(PersonSuit.class, suitIDs[0]);
		
		suit.setDeleted(true);
		suit.save();
		
		SQLLogMonitor.getInstance().markWatchSQL();
		person.getPersonLegalDefences();
		assertTrue(SQLLogMonitor.getInstance().isExecutedSQL());
		
		suit.setDeleted(false);
		suit.save();
		person.getPersonLegalDefences();
		
		suit.setPerson(null);
		suit.save();
		
		SQLLogMonitor.getInstance().markWatchSQL();
		person.getPersonLegalDefences();
		assertTrue(SQLLogMonitor.getInstance().isExecutedSQL());

		suit.setPerson(person);
		suit.save();
		person.getPersonLegalDefences();
		
		suit.setPersonLegalDefence(null);
		suit.save();
		
		SQLLogMonitor.getInstance().markWatchSQL();
		person.getPersonLegalDefences();
		assertTrue(SQLLogMonitor.getInstance().isExecutedSQL());
		
		suit.setPersonLegalDefence(defence);
		suit.save();
	}
}
