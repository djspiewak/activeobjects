package net.java.ao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;
import test.schema.Comment;
import test.schema.Pen;
import test.schema.PersonSuit;
import test.schema.Photo;
import test.schema.Post;

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
public class TestUtilities {
	public static final Test asTest(Class<?> clazz) {
		return new JUnit4TestAdapter(clazz);
	}
	
	public static final DataStruct setUpEntityManager(EntityManager manager) throws SQLException {
		DataStruct back = new DataStruct();
		
		Logger logger = Logger.getLogger("net.java.ao");
		Logger l = logger;	
		
		while ((l = l.getParent()) != null) {
			for (Handler h : l.getHandlers()) {
				l.removeHandler(h);
			}
		}
		
		logger.setLevel(Level.FINE);
		logger.addHandler(SQLLogMonitor.getInstance());
		
		manager.setPolymorphicTypeMapper(new DefaultPolymorphicTypeMapper(manager.getTableNameConverter(), 
				Photo.class, Post.class));
		
		try {
			manager.migrate(PersonSuit.class, Pen.class, Comment.class, Photo.class, Post.class);
		} catch (Throwable t) {
			t.printStackTrace();
		}
		
		Connection conn = manager.getProvider().getConnection();
		try {
			PreparedStatement stmt = conn.prepareStatement("INSERT INTO company (companyID, name, cool) VALUES (?,?,?)");
			
			stmt.setLong(1, back.companyID = System.currentTimeMillis());
			stmt.setString(2, "Company Name");
			stmt.setBoolean(3, false);
			
			stmt.executeUpdate();
			
			Thread.sleep(10);
			
			int index = 0;
			back.coolCompanyIDs = new long[3];

			stmt.setLong(1, back.coolCompanyIDs[index++] = System.currentTimeMillis());
			stmt.setString(2, "Cool Company");
			stmt.setBoolean(3, true);
			
			stmt.executeUpdate();
			
			Thread.sleep(10);

			stmt.setLong(1, back.coolCompanyIDs[index++] = System.currentTimeMillis());
			stmt.setString(2, "Cool Company");
			stmt.setBoolean(3, true);
			
			stmt.executeUpdate();
			
			Thread.sleep(10);

			stmt.setLong(1, back.coolCompanyIDs[index++] = System.currentTimeMillis());
			stmt.setString(2, "Cool Company");
			stmt.setBoolean(3, true);
			
			stmt.executeUpdate();
			
			stmt.close();
			
			stmt = conn.prepareStatement("INSERT INTO person (firstName, companyID) VALUES (?, ?)", 
					PreparedStatement.RETURN_GENERATED_KEYS);
			
			stmt.setString(1, "Daniel");
			stmt.setLong(2, back.companyID);
			
			stmt.executeUpdate();
			
			ResultSet res = stmt.getGeneratedKeys();
			if (res.next()) {
				back.personID = res.getInt(1);
			}
			res.close();
			stmt.close();
			
			back.penIDs = new int[3];
			stmt = conn.prepareStatement("INSERT INTO pen (width,personID) VALUES (?,?)", 
					PreparedStatement.RETURN_GENERATED_KEYS);
	
			index = 0;
			
			stmt.setDouble(1, 0.5);
			stmt.setInt(2, back.personID);
			stmt.executeUpdate();
			
			res = stmt.getGeneratedKeys();
			if (res.next()) {
				back.penIDs[index++] = res.getInt(1);
			}
			res.close();
			
			stmt.setDouble(1, 0.7);
			stmt.setInt(2, back.personID);
			stmt.executeUpdate();
			
			res = stmt.getGeneratedKeys();
			if (res.next()) {
				back.penIDs[index++] = res.getInt(1);
			}
			res.close();
			
			stmt.setDouble(1, 1);
			stmt.setInt(2, back.personID);
			stmt.executeUpdate();
			
			res = stmt.getGeneratedKeys();
			if (res.next()) {
				back.penIDs[index++] = res.getInt(1);
			}
			res.close();
			
			stmt.close();
			
			back.defenceIDs = new int[3];
			stmt = conn.prepareStatement("INSERT INTO personDefence (severity) VALUES (?)", 
					PreparedStatement.RETURN_GENERATED_KEYS);
			
			index = 0;
	
			stmt.setInt(1, 5);
			stmt.executeUpdate();
	
			res = stmt.getGeneratedKeys();
			if (res.next()) {
				back.defenceIDs[index++] = res.getInt(1);
			}
			res.close();
			
			stmt.setInt(1, 7);
			stmt.executeUpdate();
	
			res = stmt.getGeneratedKeys();
			if (res.next()) {
				back.defenceIDs[index++] = res.getInt(1);
			}
			res.close();
			
			stmt.setInt(1, 1);
			stmt.executeUpdate();
	
			res = stmt.getGeneratedKeys();
			if (res.next()) {
				back.defenceIDs[index++] = res.getInt(1);
			}
			res.close();
			
			stmt.close();
			
			back.suitIDs = new int[3];
			stmt = conn.prepareStatement("INSERT INTO personSuit (personID, personLegalDefenceID) VALUES (?,?)",
					PreparedStatement.RETURN_GENERATED_KEYS);
	
			index = 0;
			
			stmt.setInt(1, back.personID);
			stmt.setInt(2, back.defenceIDs[0]);
			stmt.executeUpdate();
			
			res = stmt.getGeneratedKeys();
			if (res.next()) {
				back.suitIDs[index++] = res.getInt(1);
			}
			res.close();
			
			stmt.setInt(1, back.personID);
			stmt.setInt(2, back.defenceIDs[1]);
			stmt.executeUpdate();
			
			res = stmt.getGeneratedKeys();
			if (res.next()) {
				back.suitIDs[index++] = res.getInt(1);
			}
			res.close();
			
			stmt.setInt(1, back.personID);
			stmt.setInt(2, back.defenceIDs[2]);
			stmt.executeUpdate();
			
			res = stmt.getGeneratedKeys();
			if (res.next()) {
				back.suitIDs[index++] = res.getInt(1);
			}
			res.close();
			
			stmt.close();
			
			stmt = conn.prepareStatement("INSERT INTO post (title) VALUES (?)", PreparedStatement.RETURN_GENERATED_KEYS);
			
			stmt.setString(1, "Test Post");
			stmt.executeUpdate();
			
			res = stmt.getGeneratedKeys();
			if (res.next()) {
				back.postID = res.getInt(1);
			}
			res.close();
			
			stmt.close();
			
			stmt = conn.prepareStatement("INSERT INTO photo (depth) VALUES (?)", PreparedStatement.RETURN_GENERATED_KEYS);
			
			stmt.setInt(1, 256);
			
			stmt.executeUpdate();
			
			res = stmt.getGeneratedKeys();
			if (res.next()) {
				back.photoID = res.getInt(1);
			}
			res.close();
			
			stmt.close();
			
			stmt = conn.prepareStatement("INSERT INTO comment (title,text,commentableID,commentableType) VALUES (?,?,?,?)",
					PreparedStatement.RETURN_GENERATED_KEYS);
			
			back.postCommentIDs = new int[3];
			back.photoCommentIDs = new int[2];
			
			int postCommentIndex = 0;
			int photoCommentIndex = 0;
			
			index = 1;
			stmt.setString(index++, "Test Post Comment 1");
			stmt.setString(index++, "Here's some test text");
			stmt.setInt(index++, back.postID);
			stmt.setString(index++, "post");
			stmt.executeUpdate();
			
			res = stmt.getGeneratedKeys();
			if (res.next()) {
				back.postCommentIDs[postCommentIndex++] = res.getInt(1);
			}
			res.close();
			
			index = 1;
			stmt.setString(index++, "Test Post Comment 2");
			stmt.setString(index++, "Here's some test text");
			stmt.setInt(index++, back.postID);
			stmt.setString(index++, "post");
			stmt.executeUpdate();
			
			res = stmt.getGeneratedKeys();
			if (res.next()) {
				back.postCommentIDs[postCommentIndex++] = res.getInt(1);
			}
			res.close();
			
			index = 1;
			stmt.setString(index++, "Test Photo Comment 1");
			stmt.setString(index++, "Here's some test text");
			stmt.setInt(index++, back.photoID);
			stmt.setString(index++, "photo");
			stmt.executeUpdate();
			
			res = stmt.getGeneratedKeys();
			if (res.next()) {
				back.photoCommentIDs[photoCommentIndex++] = res.getInt(1);
			}
			res.close();
			
			index = 1;
			stmt.setString(index++, "Test Post Comment 3");
			stmt.setString(index++, "Here's some test text");
			stmt.setInt(index++, back.postID);
			stmt.setString(index++, "post");
			stmt.executeUpdate();
			
			res = stmt.getGeneratedKeys();
			if (res.next()) {
				back.postCommentIDs[postCommentIndex++] = res.getInt(1);
			}
			res.close();
			
			index = 1;
			stmt.setString(index++, "Test Photo Comment 2");
			stmt.setString(index++, "Here's some test text");
			stmt.setInt(index++, back.photoID);
			stmt.setString(index++, "photo");
			stmt.executeUpdate();
			
			res = stmt.getGeneratedKeys();
			if (res.next()) {
				back.photoCommentIDs[photoCommentIndex++] = res.getInt(1);
			}
			res.close();
			
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
		} finally {
			conn.close();
		}
		
		return back;
	}
	
	public static final void tearDownEntityManager(EntityManager manager) throws SQLException {
		Connection conn = manager.getProvider().getConnection();
		try {
			Statement stmt = conn.createStatement();
			
			stmt.executeUpdate("DELETE FROM pen");
			stmt.executeUpdate("DELETE FROM personSuit");
			stmt.executeUpdate("DELETE FROM personDefence");
			stmt.executeUpdate("DELETE FROM person");
			stmt.executeUpdate("DELETE FROM company");
			stmt.executeUpdate("DELETE FROM comment");
			stmt.executeUpdate("DELETE FROM post");
			stmt.executeUpdate("DELETE FROM photo");
			
			stmt.close();
		} finally {
			conn.close();
		}
	}
}
