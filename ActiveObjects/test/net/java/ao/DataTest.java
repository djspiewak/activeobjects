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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.java.ao.types.ClassType;
import net.java.ao.types.TypeManager;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import test.schema.Company;
import test.schema.Pen;
import test.schema.Person;
import test.schema.PersonLegalDefence;
import test.schema.PersonSuit;

/**
 * @author Daniel Spiewak
 */
public abstract class DataTest {
	protected static EntityManager manager;
	
	protected static int personID;
	protected static int companyID;
	
	protected static int[] penIDs;
	protected static int[] defenceIDs;
	protected static int[] suitIDs;
	
	protected static int[] coolCompanyIDs;

	@BeforeClass
	public static void setUp() throws SQLException {
		TypeManager.getInstance().addType(new ClassType());
		
		manager = new EntityManager("jdbc:derby:test_database;create=true", "sa", "jeffbridges");
		manager.migrate(PersonSuit.class, Pen.class);
		
		Logger logger = Logger.getLogger("net.java.ao");
		Logger l = logger;
		
		while ((l = l.getParent()) != null) {
			for (Handler h : l.getHandlers()) {
				l.removeHandler(h);
			}
		}
		
		logger.setLevel(Level.FINE);
		logger.addHandler(SQLLogMonitor.getInstance());
		
		Connection conn = manager.getProvider().getConnection();
		try {
			PreparedStatement stmt = conn.prepareStatement("INSERT INTO company (name, cool) VALUES (?,?)", 
					PreparedStatement.RETURN_GENERATED_KEYS);
			
			stmt.setString(1, "Company Name");
			stmt.setBoolean(2, false);
			
			stmt.executeUpdate();
			
			ResultSet res = stmt.getGeneratedKeys();
			if (res.next()) {
				companyID = res.getInt(1);
			}
			res.close();
			
			int index = 0;
			coolCompanyIDs = new int[3];

			stmt.setString(1, "Cool Company");
			stmt.setBoolean(2, true);
			
			stmt.executeUpdate();
			
			res = stmt.getGeneratedKeys();
			if (res.next()) {
				coolCompanyIDs[index++] = res.getInt(1);
			}
			res.close();

			stmt.setString(1, "Cool Company");
			stmt.setBoolean(2, true);
			
			stmt.executeUpdate();
			
			res = stmt.getGeneratedKeys();
			if (res.next()) {
				coolCompanyIDs[index++] = res.getInt(1);
			}
			res.close();

			stmt.setString(1, "Cool Company");
			stmt.setBoolean(2, true);
			
			stmt.executeUpdate();
			
			res = stmt.getGeneratedKeys();
			if (res.next()) {
				coolCompanyIDs[index++] = res.getInt(1);
			}
			res.close();
			
			stmt.close();
			
			stmt = conn.prepareStatement("INSERT INTO person (firstName, companyID) VALUES (?, ?)", 
					PreparedStatement.RETURN_GENERATED_KEYS);
			
			stmt.setString(1, "Daniel");
			stmt.setInt(2, companyID);
			
			stmt.executeUpdate();
			
			res = stmt.getGeneratedKeys();
			if (res.next()) {
				personID = res.getInt(1);
			}
			res.close();
			stmt.close();
			
			penIDs = new int[3];
			stmt = conn.prepareStatement("INSERT INTO pen (width,personID) VALUES (?,?)", 
					PreparedStatement.RETURN_GENERATED_KEYS);
	
			index = 0;
			
			stmt.setDouble(1, 0.5);
			stmt.setInt(2, personID);
			stmt.executeUpdate();
			
			res = stmt.getGeneratedKeys();
			if (res.next()) {
				penIDs[index++] = res.getInt(1);
			}
			res.close();
			
			stmt.setDouble(1, 0.7);
			stmt.setInt(2, personID);
			stmt.executeUpdate();
			
			res = stmt.getGeneratedKeys();
			if (res.next()) {
				penIDs[index++] = res.getInt(1);
			}
			res.close();
			
			stmt.setDouble(1, 1);
			stmt.setInt(2, personID);
			stmt.executeUpdate();
			
			res = stmt.getGeneratedKeys();
			if (res.next()) {
				penIDs[index++] = res.getInt(1);
			}
			res.close();
			
			stmt.close();
			
			defenceIDs = new int[3];
			stmt = conn.prepareStatement("INSERT INTO personDefence (severity) VALUES (?)", 
					PreparedStatement.RETURN_GENERATED_KEYS);
			
			index = 0;
	
			stmt.setInt(1, 5);
			stmt.executeUpdate();
	
			res = stmt.getGeneratedKeys();
			if (res.next()) {
				defenceIDs[index++] = res.getInt(1);
			}
			res.close();
			
			stmt.setInt(1, 7);
			stmt.executeUpdate();
	
			res = stmt.getGeneratedKeys();
			if (res.next()) {
				defenceIDs[index++] = res.getInt(1);
			}
			res.close();
			
			stmt.setInt(1, 1);
			stmt.executeUpdate();
	
			res = stmt.getGeneratedKeys();
			if (res.next()) {
				defenceIDs[index++] = res.getInt(1);
			}
			res.close();
			
			stmt.close();
			
			suitIDs = new int[3];
			stmt = conn.prepareStatement("INSERT INTO personSuit (personID, personLegalDefenceID) VALUES (?,?)",
					PreparedStatement.RETURN_GENERATED_KEYS);
	
			index = 0;
			
			stmt.setInt(1, personID);
			stmt.setInt(2, defenceIDs[0]);
			stmt.executeUpdate();
			
			res = stmt.getGeneratedKeys();
			if (res.next()) {
				suitIDs[index++] = res.getInt(1);
			}
			res.close();
			
			stmt.setInt(1, personID);
			stmt.setInt(2, defenceIDs[1]);
			stmt.executeUpdate();
			
			res = stmt.getGeneratedKeys();
			if (res.next()) {
				suitIDs[index++] = res.getInt(1);
			}
			res.close();
			
			stmt.setInt(1, personID);
			stmt.setInt(2, defenceIDs[2]);
			stmt.executeUpdate();
			
			res = stmt.getGeneratedKeys();
			if (res.next()) {
				suitIDs[index++] = res.getInt(1);
			}
			res.close();
			
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			conn.close();
		}
	}

	@AfterClass
	public static void tearDown() throws SQLException {
		for (int id : penIDs) {
			manager.delete(manager.get(Pen.class, id));
		}
		for (int id : suitIDs) {
			manager.delete(manager.get(PersonSuit.class, id));
		}
		for (int id : defenceIDs) {
			manager.delete(manager.get(PersonLegalDefence.class, id));
		}
		
		for (int id : coolCompanyIDs) {
			manager.delete(manager.get(Company.class, id));
		}
		
		manager.delete(manager.get(Person.class, personID));
		manager.delete(manager.get(Company.class, companyID));
		
		manager.getProvider().dispose();
	}

}
