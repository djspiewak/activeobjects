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
import java.sql.Statement;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.java.ao.types.ClassType;
import net.java.ao.types.TypeManager;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import test.schema.Pen;
import test.schema.PersonSuit;

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
		
		Logger logger = Logger.getLogger("net.java.ao");
		Logger l = logger;	
		
		while ((l = l.getParent()) != null) {
			for (Handler h : l.getHandlers()) {
				l.removeHandler(h);
			}
		}
		
		logger.setLevel(Level.FINE);
		logger.addHandler(SQLLogMonitor.getInstance());
		
		try {
			manager.migrate(PersonSuit.class, Pen.class);
		} catch (Throwable t) {
			t.printStackTrace();
		}
		
		Connection conn = manager.getProvider().getConnection();
		try {
			PreparedStatement stmt = conn.prepareStatement("INSERT INTO company (companyID, name, cool) VALUES (?,?,?)");
			
			stmt.setLong(1, companyID = System.currentTimeMillis());
			stmt.setString(2, "Company Name");
			stmt.setBoolean(3, false);
			
			stmt.executeUpdate();
			
			int index = 0;
			coolCompanyIDs = new long[3];

			stmt.setLong(1, coolCompanyIDs[index++] = System.currentTimeMillis());
			stmt.setString(2, "Cool Company");
			stmt.setBoolean(3, true);
			
			stmt.executeUpdate();

			stmt.setLong(1, coolCompanyIDs[index++] = System.currentTimeMillis());
			stmt.setString(2, "Cool Company");
			stmt.setBoolean(3, true);
			
			stmt.executeUpdate();

			stmt.setLong(1, coolCompanyIDs[index++] = System.currentTimeMillis());
			stmt.setString(2, "Cool Company");
			stmt.setBoolean(3, true);
			
			stmt.executeUpdate();
			
			stmt.close();
			
			stmt = conn.prepareStatement("INSERT INTO person (firstName, companyID) VALUES (?, ?)", 
					PreparedStatement.RETURN_GENERATED_KEYS);
			
			stmt.setString(1, "Daniel");
			stmt.setLong(2, companyID);
			
			stmt.executeUpdate();
			
			ResultSet res = stmt.getGeneratedKeys();
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
		Connection conn = manager.getProvider().getConnection();
		try {
			Statement stmt = conn.createStatement();
			
			stmt.executeUpdate("DELETE FROM pen");
			stmt.executeUpdate("DELETE FROM personSuit");
			stmt.executeUpdate("DELETE FROM personDefence");
			stmt.executeUpdate("DELETE FROM person");
			stmt.executeUpdate("DELETE FROM company");
			
			stmt.close();
		} finally {
			conn.close();
		}
		
		manager.getProvider().dispose();
	}

}
