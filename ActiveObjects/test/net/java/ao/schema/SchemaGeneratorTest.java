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
package net.java.ao.schema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.sql.Types;

import net.java.ao.DataTest;
import net.java.ao.schema.ddl.DDLField;
import net.java.ao.schema.ddl.DDLForeignKey;
import net.java.ao.schema.ddl.DDLIndex;
import net.java.ao.schema.ddl.DDLTable;

import org.junit.Test;

import test.schema.Company;
import test.schema.Pen;
import test.schema.Person;
import test.schema.PersonSuit;

/**
 * @author Daniel Spiewak
 */
public class SchemaGeneratorTest extends DataTest {

	public SchemaGeneratorTest(int ordinal, TableNameConverter tableConverter, FieldNameConverter fieldConverter) throws SQLException {
		super(ordinal, tableConverter, fieldConverter);
	}

	@SuppressWarnings("null")
	@Test
	public void testParseDDL() {
		String[] expectedFields = {"id", "firstName", "lastName", "profession", "age", "url", "favoriteClass", "companyID", "image", "modified"};
		String[] expectedIndexes = {"age", "companyID"};
		
		TableNameConverter tableNameConverter = manager.getTableNameConverter();
		DDLTable[] parsedTables = SchemaGenerator.parseDDL(manager.getProvider(), tableNameConverter, 
				manager.getFieldNameConverter(), SchemaGeneratorTest.class.getClassLoader(), PersonSuit.class, Pen.class);
		
		assertEquals(6, parsedTables.length);
		
		DDLTable personDDL = null;
		for (DDLTable table : parsedTables) {
			if (table.getName().equals(tableNameConverter.getName(Person.class))) {
				personDDL = table;
				break;
			}
		}
		
		assertNotNull(personDDL);
		assertEquals(expectedFields.length, personDDL.getFields().length);
		assertEquals(1, personDDL.getForeignKeys().length);
		
		for (DDLField field : personDDL.getFields()) {
			boolean found = false;
			for (String expectedField : expectedFields) {
				if (expectedField.equals(field.getName())) {
					found = true;
					break;
				}
			}
			
			if (!found) {
				fail("Field " + field.getName() + " was unexpected");
			}
		}
		
		DDLField urlField = null;
		DDLField ageField = null;
		DDLField lastNameField = null;
		DDLField idField = null;
		DDLField cidField = null;
		
		for (DDLField field : personDDL.getFields()) {
			if (field.getName().equals("url")) {
				urlField = field;
			} else if (field.getName().equals("age")) {
				ageField = field;
			} else if (field.getName().equals("lastName")) {
				lastNameField = field;
			} else if (field.getName().equals("id")) {
				idField = field;
			} else if (field.getName().equals("companyID")) {
				cidField = field;
			}
		}
		
		assertEquals(Types.VARCHAR, urlField.getType().getType());
		
		assertFalse(urlField.isAutoIncrement());
		assertFalse(urlField.isNotNull());
		assertFalse(urlField.isPrimaryKey());
		assertTrue(urlField.isUnique());
		
		assertNull(urlField.getOnUpdate());
		assertNotNull(urlField.getDefaultValue());
		
		assertEquals(Types.INTEGER, ageField.getType().getType());
		assertEquals(20, ageField.getPrecision());
		
		assertFalse(ageField.isAutoIncrement());
		assertFalse(ageField.isNotNull());
		assertFalse(ageField.isPrimaryKey());
		assertFalse(ageField.isUnique());
		
		assertEquals(Types.VARCHAR, lastNameField.getType().getType());
		assertEquals(127, lastNameField.getPrecision());
		
		assertFalse(lastNameField.isAutoIncrement());
		assertFalse(lastNameField.isNotNull());
		assertFalse(lastNameField.isPrimaryKey());
		assertFalse(lastNameField.isUnique());
		
		assertEquals(Types.INTEGER, idField.getType().getType());
		
		assertTrue(idField.isAutoIncrement());
		assertTrue(idField.isNotNull());
		assertTrue(idField.isPrimaryKey());
		assertFalse(idField.isUnique());
		
		assertNull(idField.getOnUpdate());
		assertNull(idField.getDefaultValue());
		
		assertEquals(Types.BIGINT, cidField.getType().getType());
		
		assertFalse(cidField.isAutoIncrement());
		assertFalse(cidField.isNotNull());
		assertFalse(cidField.isPrimaryKey());
		assertFalse(cidField.isUnique());
		
		assertNull(cidField.getOnUpdate());
		assertNull(cidField.getDefaultValue());
		
		DDLForeignKey cidKey = null;
		for (DDLForeignKey key : personDDL.getForeignKeys()) {
			if (key.getField().equals("companyID")) {
				cidKey = key;
				break;
			}
		}
		
		assertNotNull(cidKey);
		
		assertEquals(manager.getProvider().processID(tableNameConverter.getName(Person.class)),
				cidKey.getDomesticTable());
		assertEquals("companyID", cidKey.getField());
		assertEquals("companyID", cidKey.getForeignField());
		assertEquals(tableNameConverter.getName(Company.class), cidKey.getTable());
		
		assertEquals(expectedIndexes.length, personDDL.getIndexes().length);
		for (DDLIndex index : personDDL.getIndexes()) {
			boolean found = false;
			for (String expectedIndex : expectedIndexes) {
				if (expectedIndex.equals(index.getField())) {
					assertEquals(tableNameConverter.getName(Person.class), index.getTable());
					
					found = true;
					break;
				}
			}
			
			if (!found) {
				fail("Index for field " + index.getField() + " was unexpected");
			}
		}
	}
}
