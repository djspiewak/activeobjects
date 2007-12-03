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
import test.schema.Authorship;
import test.schema.Book;
import test.schema.Comment;
import test.schema.Distribution;
import test.schema.Magazine;
import test.schema.OnlineDistribution;
import test.schema.Pen;
import test.schema.PersonSuit;
import test.schema.Photo;
import test.schema.Post;
import test.schema.PrintDistribution;
import test.schema.PublicationToDistribution;

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
				Photo.class, Post.class, Book.class, Magazine.class, PrintDistribution.class, OnlineDistribution.class));
		
		try {
			manager.migrate(PersonSuit.class, Pen.class, Comment.class, Photo.class, Post.class, 
					Authorship.class, Book.class, Magazine.class, 
					PublicationToDistribution.class, PrintDistribution.class, OnlineDistribution.class);
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
			
			back.bookIDs = new int[2];
			index = 0;
			
			stmt = conn.prepareStatement("INSERT INTO book (title,hardcover) VALUES (?,?)", 
					PreparedStatement.RETURN_GENERATED_KEYS);
			
			stmt.setString(1, "Test Book 1");
			stmt.setInt(2, 1);
			stmt.executeUpdate();
			
			res = stmt.getGeneratedKeys();
			if (res.next()) {
				back.bookIDs[index++] = res.getInt(1);
			}
			res.close();
			
			stmt.setString(1, "Test Book 2");
			stmt.setInt(2, 1);
			stmt.executeUpdate();
			
			res = stmt.getGeneratedKeys();
			if (res.next()) {
				back.bookIDs[index++] = res.getInt(1);
			}
			res.close();
			
			stmt.close();
			
			back.magazineIDs = new int[2];
			index = 0;
			
			stmt = conn.prepareStatement("INSERT INTO magazine (title) VALUES (?)", 
					PreparedStatement.RETURN_GENERATED_KEYS);
			
			stmt.setString(1, "Test Magazine 1");
			stmt.executeUpdate();
			
			res = stmt.getGeneratedKeys();
			if (res.next()) {
				back.magazineIDs[index++] = res.getInt(1);
			}
			res.close();
			
			stmt.setString(1, "Test Magazine 2");
			stmt.executeUpdate();
			
			res = stmt.getGeneratedKeys();
			if (res.next()) {
				back.magazineIDs[index++] = res.getInt(1);
			}
			res.close();
			
			stmt.close();
			
			back.bookAuthorIDs = new int[2][3];
			index = 0;
			
			for (int i = 0; i < back.bookIDs.length; i++) {
				for (int subIndex = 0; subIndex < back.bookAuthorIDs[0].length; subIndex++) {
					stmt = conn.prepareStatement("INSERT INTO author (name) VALUES (?)", 
							PreparedStatement.RETURN_GENERATED_KEYS);
					
					stmt.setString(1, "Test Book Author " + (subIndex + 1));
					stmt.executeUpdate();
					
					res = stmt.getGeneratedKeys();
					if (res.next()) {
						back.bookAuthorIDs[i][subIndex] = res.getInt(1);
					}
					res.close();
					
					stmt.close();
					
					stmt = conn.prepareStatement("INSERT INTO authorship (publicationID,publicationType,authorID) VALUES (?,?,?)");
					
					stmt.setInt(1, back.bookIDs[i]);
					stmt.setString(2, "book");
					stmt.setInt(3, back.bookAuthorIDs[i][subIndex]);
					stmt.executeUpdate();
					
					stmt.close();
				}
			}
			
			back.magazineAuthorIDs = new int[2][3];
			
			for (int i = 0; i < back.magazineIDs.length; i++) {
				for (int subIndex = 0; subIndex < back.magazineAuthorIDs[0].length; subIndex++) {
					stmt = conn.prepareStatement("INSERT INTO author (name) VALUES (?)", 
							PreparedStatement.RETURN_GENERATED_KEYS);
					
					stmt.setString(1, "Test Magazine Author " + (subIndex + 1));
					stmt.executeUpdate();
					
					res = stmt.getGeneratedKeys();
					if (res.next()) {
						back.magazineAuthorIDs[i][subIndex] = res.getInt(1);
					}
					res.close();
					
					stmt.close();
					
					stmt = conn.prepareStatement("INSERT INTO authorship (publicationID,publicationType,authorID) VALUES (?,?,?)");
					
					stmt.setInt(1, back.magazineIDs[i]);
					stmt.setString(2, "magazine");
					stmt.setInt(3, back.magazineAuthorIDs[i][subIndex]);
					stmt.executeUpdate();
					
					stmt.close();
				}
			}
			
			back.bookDistributionIDs = new int[2][5];
			back.bookDistributionTypes = new Class[2][5];
			
			for (int i = 0; i < back.bookIDs.length; i++) {
				for (int subIndex = 0; subIndex < back.bookDistributionIDs[0].length; subIndex++) {
					Class<? extends Distribution> distType = (subIndex % 2 == 0 ? PrintDistribution.class 
							: OnlineDistribution.class);
					String distTableName = manager.getTableNameConverter().getName(distType);
					String params = null;
					
					if (distType == PrintDistribution.class) {
						params = " (copies) VALUES (?)";
					} else if (distType == OnlineDistribution.class) {
						params = " (url) VALUES (?)";
					}
					
					back.bookDistributionTypes[i][subIndex] = distType;
					
					stmt = conn.prepareStatement("INSERT INTO " + distTableName + ' ' + params, 
							PreparedStatement.RETURN_GENERATED_KEYS);
					
					if (distType == PrintDistribution.class) {
						stmt.setInt(1, 20);
					} else if (distType == OnlineDistribution.class) {
						stmt.setString(1, "http://www.google.com");
					}
					stmt.executeUpdate();
					
					res = stmt.getGeneratedKeys();
					if (res.next()) {
						back.bookDistributionIDs[i][subIndex] = res.getInt(1);
					}
					res.close();
					
					stmt.close();
					
					stmt = conn.prepareStatement("INSERT INTO publicationToDistribution " +
							"(publicationID,publicationType,distributionID,distributionType) VALUES (?,?,?,?)");
					
					stmt.setInt(1, back.bookIDs[i]);
					stmt.setString(2, "book");
					stmt.setInt(3, back.bookDistributionIDs[i][subIndex]);
					stmt.setString(4, manager.getPolymorphicTypeMapper().convert(distType));
					stmt.executeUpdate();
					
					stmt.close();
				}
			}
			
			back.magazineDistributionIDs = new int[2][12];
			back.magazineDistributionTypes = new Class[2][12];
			
			for (int i = 0; i < back.magazineIDs.length; i++) {
				for (int subIndex = 0; subIndex < back.magazineDistributionIDs[0].length; subIndex++) {
					Class<? extends Distribution> distType = (subIndex % 2 == 0 ? PrintDistribution.class 
							: OnlineDistribution.class);
					String distTableName = manager.getTableNameConverter().getName(distType);
					String params = null;
					
					if (distType == PrintDistribution.class) {
						params = " (copies) VALUES (?)";
					} else if (distType == OnlineDistribution.class) {
						params = " (url) VALUES (?)";
					}
					
					back.magazineDistributionTypes[i][subIndex] = distType;
					
					stmt = conn.prepareStatement("INSERT INTO " + distTableName + ' ' + params, 
							PreparedStatement.RETURN_GENERATED_KEYS);
					
					if (distType == PrintDistribution.class) {
						stmt.setInt(1, 20);
					} else if (distType == OnlineDistribution.class) {
						stmt.setString(1, "http://www.google.com");
					}
					stmt.executeUpdate();
					
					res = stmt.getGeneratedKeys();
					if (res.next()) {
						back.magazineDistributionIDs[i][subIndex] = res.getInt(1);
					}
					res.close();
					
					stmt.close();
					
					stmt = conn.prepareStatement("INSERT INTO publicationToDistribution " +
							"(publicationID,publicationType,distributionID,distributionType) VALUES (?,?,?,?)");
					
					stmt.setInt(1, back.magazineIDs[i]);
					stmt.setString(2, "magazine");
					stmt.setInt(3, back.magazineDistributionIDs[i][subIndex]);
					stmt.setString(4, manager.getPolymorphicTypeMapper().convert(distType));
					stmt.executeUpdate();
					
					stmt.close();
				}
			}
			
			for (int i = 0; i < 3; i++) {	// add some extra, unrelated distributions
				Class<? extends Distribution> distType = (i % 2 == 0 ? PrintDistribution.class 
						: OnlineDistribution.class);
				String distTableName = manager.getTableNameConverter().getName(distType);
				String params = null;
				
				if (distType == PrintDistribution.class) {
					params = " (copies) VALUES (?)";
				} else if (distType == OnlineDistribution.class) {
					params = " (url) VALUES (?)";
				}
				
				stmt = conn.prepareStatement("INSERT INTO " + distTableName + ' ' + params);
				
				if (distType == PrintDistribution.class) {
					stmt.setInt(1, 20);
				} else if (distType == OnlineDistribution.class) {
					stmt.setString(1, "http://www.dzone.com");
				}
				stmt.executeUpdate();
				
				stmt.close();
			}
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
			stmt.executeUpdate("DELETE FROM authorship");
			stmt.executeUpdate("DELETE FROM author");
			stmt.executeUpdate("DELETE FROM book");
			stmt.executeUpdate("DELETE FROM magazine");
			stmt.executeUpdate("DELETE FROM publicationToDistribution");
			stmt.executeUpdate("DELETE FROM printDistribution");
			stmt.executeUpdate("DELETE FROM onlineDistribution");
			
			stmt.close();
		} finally {
			conn.close();
		}
	}
}
