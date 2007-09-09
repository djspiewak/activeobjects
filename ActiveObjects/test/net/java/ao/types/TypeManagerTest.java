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

import java.net.URL;
import java.sql.Types;
import java.util.Calendar;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import test.schema.Person;

/**
 * @author Daniel Spiewak
 */
public class TypeManagerTest {
	
	@Before
	public void preTest() {
		TypeManager.getInstance().addType(new ClassType());
	}

	@Test
	public void testGetTypeClass() {
		TypeManager manager = TypeManager.getInstance();

		assertEquals(new VarcharType(), manager.getType(String.class));
		assertEquals(new IntegerType(), manager.getType(int.class));
		assertEquals(new IntegerType(), manager.getType(Integer.class));
		assertEquals(new DoubleType(), manager.getType(double.class));
		assertEquals(new TimestampType(), manager.getType(Calendar.class));
		assertEquals(new TimestampDateType(), manager.getType(Date.class));
		assertEquals(new EntityType(), manager.getType(Person.class));
		assertEquals(new URLType(), manager.getType(URL.class));
		
		assertEquals(new ClassType(), manager.getType(Class.class));
	}

	@Test
	public void testGetTypeInt() {
		TypeManager manager = TypeManager.getInstance();

		assertEquals(new VarcharType(), manager.getType(Types.VARCHAR));
		assertEquals(new IntegerType(), manager.getType(Types.INTEGER));
		assertEquals(new TimestampType(), manager.getType(Types.TIMESTAMP));
		assertEquals(new CharType(), manager.getType(Types.CHAR));
		assertEquals(new GenericType(Types.SQLXML), manager.getType(Types.SQLXML));
		assertEquals(new DateType(), manager.getType(Types.DATE));
		assertEquals(new RealType(), manager.getType(Types.REAL));
	}
}
