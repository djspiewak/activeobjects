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
package net.java.ao.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import net.java.ao.DataTest;

import org.junit.Test;

/**
 * @author Daniel Spiewak
 */
public class DatabaseTypeTest extends DataTest {

	@Test
	public void testIsHandlerForInt() {
		assertTrue(new IntegerType().isHandlerFor(Types.INTEGER));
		assertTrue(new DoubleType().isHandlerFor(Types.DOUBLE));
		assertTrue(new URLType().isHandlerFor(Types.VARCHAR));
		assertTrue(new VarcharType().isHandlerFor(Types.VARCHAR));
		assertTrue(new TimestampType().isHandlerFor(Types.TIMESTAMP));
		assertTrue(new GenericType(Types.ARRAY).isHandlerFor(Types.ARRAY));
	}

	@Test
	public void testIsHandlerForClass() {
		assertTrue(new IntegerType().isHandlerFor(int.class));
		assertTrue(new IntegerType().isHandlerFor(Integer.class));
		
		assertTrue(new DoubleType().isHandlerFor(double.class));
		assertTrue(new DoubleType().isHandlerFor(Double.class));
		
		assertTrue(new URLType().isHandlerFor(URL.class));
		assertTrue(new VarcharType().isHandlerFor(String.class));
		assertTrue(new TimestampType().isHandlerFor(Calendar.class));
		
		assertFalse(new GenericType(Types.ARRAY).isHandlerFor(Object.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testPutToDatabase() throws SQLException, MalformedURLException {
		Connection conn = manager.getProvider().getConnection();
		try {
			PreparedStatement stmt = conn.prepareStatement(
					"UPDATE person SET firstName = ?, age = ?, url = ?, favoriteClass = ? WHERE id = ?");
			
			int index = 1;
			
			new VarcharType().putToDatabase(index++, stmt, "JoeJoe");
			new IntegerType().putToDatabase(index++, stmt, 123);
			new URLType().putToDatabase(index++, stmt, new URL("http://www.google.com"));
			new ClassType().putToDatabase(index++, stmt, getClass());
			
			stmt.setInt(index++, personID);
			
			stmt.executeUpdate();
			stmt.close();
			
			stmt = conn.prepareStatement("SELECT firstName,age,url,favoriteClass FROM person WHERE id = ?");
			stmt.setInt(1, personID);
			
			ResultSet res = stmt.executeQuery();
			if (res.next()) {
				assertEquals("JoeJoe", res.getString("firstName"));
				assertEquals(123, res.getInt("age"));
				assertEquals("http://www.google.com", res.getString("url"));
				assertEquals("net.java.ao.types.DatabaseTypeTest", res.getString("favoriteClass"));
			}
			res.close();
			stmt.close();
		} finally {
			conn.close();
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testConvert() throws SQLException, MalformedURLException {
		Connection conn = manager.getProvider().getConnection();
		try {
			PreparedStatement stmt = conn.prepareStatement(
					"UPDATE person SET firstName = ?, age = ?, url = ?, favoriteClass = ? WHERE id = ?");
	
			int index = 1;
			
			stmt.setString(index++, "JoeJoe");
			stmt.setInt(index++, 123);
			stmt.setString(index++, "http://www.google.com");
			stmt.setString(index++, "net.java.ao.types.DatabaseTypeTest");
			
			stmt.setInt(index++, personID);
			
			stmt.executeUpdate();
			stmt.close();
			
			stmt = conn.prepareStatement("SELECT firstName,age,url,favoriteClass FROM person WHERE id = ?");
			stmt.setInt(1, personID);
			
			ResultSet res = stmt.executeQuery();
			if (res.next()) {
				assertEquals("JoeJoe", new VarcharType().convert(manager, res, String.class, "firstName"));
				assertEquals(123, new IntegerType().convert(manager, res, int.class, "age"));
				assertEquals(new URL("http://www.google.com"), new URLType().convert(manager, res, URL.class, "url"));
				assertEquals(getClass(), new ClassType().convert(manager, res, (Class) Class.class, "favoriteClass"));
			}
			res.close();
			stmt.close();
		} finally {
			conn.close();
		}
	}

	@Test
	public void testDefaultParseValue() throws MalformedURLException {
		assertEquals(123, new IntegerType().defaultParseValue("123"));
		assertEquals(123.456, new DoubleType().defaultParseValue("123.456"));
		assertEquals(123.456, new FloatType().defaultParseValue("123.456"));
		assertEquals("My test value", new VarcharType().defaultParseValue("My test value"));
		assertEquals(new URL("http://www.google.com"), new URLType().defaultParseValue("http://www.google.com"));
		assertEquals(false, new BooleanType().defaultParseValue("false"));
		assertEquals(String.class, new ClassType().defaultParseValue("java.lang.String"));
		assertEquals(123, new TinyIntType().defaultParseValue("123"));
		assertEquals('c', new CharType().defaultParseValue("c"));
		

		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MILLISECOND, 0);
		
		assertEquals(cal, new TimestampType().defaultParseValue(dateFormatter.format(cal.getTime())));
		assertEquals(cal.getTime(), new TimestampDateType().defaultParseValue(dateFormatter.format(cal.getTime())));
	}
	
	@Test
	public void testValueToString() throws MalformedURLException {
		assertEquals("123", new IntegerType().valueToString(123));
		assertEquals("123.456", new DoubleType().valueToString(123.456));
		assertEquals("123.456", new FloatType().valueToString(123.456));
		assertEquals("My test value", new VarcharType().valueToString("My test value"));
		assertEquals("http://www.google.com", new URLType().valueToString(new URL("http://www.google.com")));
		assertEquals("false", new BooleanType().valueToString(false));
		assertEquals("java.lang.String", new ClassType().valueToString(String.class));
		assertEquals("123", new TinyIntType().valueToString(123));
		assertEquals("c", new CharType().valueToString('c'));
		

		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MILLISECOND, 0);
		
		assertEquals(dateFormatter.format(cal.getTime()), new TimestampType().valueToString(cal));
		assertEquals(dateFormatter.format(cal.getTime()), new TimestampDateType().valueToString(cal.getTime()));
	}
}
