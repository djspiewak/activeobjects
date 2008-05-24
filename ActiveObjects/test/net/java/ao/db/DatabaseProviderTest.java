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
package net.java.ao.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.sql.Types;
import java.util.Calendar;

import net.java.ao.DatabaseFunction;
import net.java.ao.DatabaseProvider;
import net.java.ao.schema.ddl.DDLAction;
import net.java.ao.schema.ddl.DDLActionType;
import net.java.ao.schema.ddl.DDLField;
import net.java.ao.schema.ddl.DDLForeignKey;
import net.java.ao.schema.ddl.DDLIndex;
import net.java.ao.schema.ddl.DDLTable;
import net.java.ao.types.ClassType;
import net.java.ao.types.TypeManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import test.schema.Company;

/**
 * @author Daniel Spiewak
 */
@SuppressWarnings("deprecation")
public class DatabaseProviderTest {
	private static final PrintStream STDERR = System.err;
	
	@Before
	public void setUp() {
		System.setErr(new PrintStream(new OutputStream() {
			@Override
			public void write(int arg0) throws IOException {
			}
		}));
	}

	@Test
	public void testRenderActionCreateTable() throws IOException {
		DDLAction action = createActionCreateTable();
		
		DatabaseProvider provider = new EmbeddedDerbyDatabaseProvider("", "", "");
		assertEquals(readStatements("derby-create-table.sql"), provider.renderAction(action));
		
		provider = new HSQLDatabaseProvider("", "", "");
		assertEquals(readStatements("hsqldb-create-table.sql"), provider.renderAction(action));
		
		provider = new JTDSSQLServerDatabaseProvider("", "", "");
		assertEquals(readStatements("sqlserver-create-table.sql"), provider.renderAction(action));
		
		provider = new MySQLDatabaseProvider("", "", "");
		assertEquals(readStatements("mysql-create-table.sql"), provider.renderAction(action));
		
		provider = new OracleDatabaseProvider("", "", "");
		assertEquals(readStatements("oracle-create-table.sql"), provider.renderAction(action));
		
		provider = new PostgreSQLDatabaseProvider("", "", "");
		assertEquals(readStatements("postgres-create-table.sql"), provider.renderAction(action));action = createActionCreateTable();
	}

	@Test
	public void testRenderActionDropTable() throws IOException {
		DDLAction action = createActionDropTable();
		String[] ddl = {"DROP TABLE person"};
		
		DatabaseProvider provider = new EmbeddedDerbyDatabaseProvider("", "", "");
		assertEquals(ddl, provider.renderAction(action));
		
		provider = new HSQLDatabaseProvider("", "", "");
		assertEquals(ddl, provider.renderAction(action));
		
		provider = new JTDSSQLServerDatabaseProvider("", "", "");
		assertEquals(ddl, provider.renderAction(action));
		
		provider = new MySQLDatabaseProvider("", "", "");
		assertEquals(ddl, provider.renderAction(action));
		
		provider = new OracleDatabaseProvider("", "", "");
		assertEquals(readStatements("oracle-drop-table.sql"), provider.renderAction(action));
		
		provider = new PostgreSQLDatabaseProvider("", "", "");
		assertEquals(ddl, provider.renderAction(action));action = createActionCreateTable();
	}
	
	@Test
	public void testRenderActionAddColumn() throws IOException {
		DDLAction action = createActionAddColumn();
		
		DatabaseProvider provider = new EmbeddedDerbyDatabaseProvider("", "", "");
		assertEquals(readStatements("derby-add-column.sql"), provider.renderAction(action));
		
		provider = new HSQLDatabaseProvider("", "", "");
		assertEquals(readStatements("hsqldb-add-column.sql"), provider.renderAction(action));
		
		provider = new JTDSSQLServerDatabaseProvider("", "", "");
		assertEquals(readStatements("sqlserver-add-column.sql"), provider.renderAction(action));
		
		provider = new MySQLDatabaseProvider("", "", "");
		assertEquals(readStatements("mysql-add-column.sql"), provider.renderAction(action));
		
		provider = new OracleDatabaseProvider("", "", "");
		assertEquals(readStatements("oracle-add-column.sql"), provider.renderAction(action));
		
		provider = new PostgreSQLDatabaseProvider("", "", "");
		assertEquals(readStatements("postgres-add-column.sql"), provider.renderAction(action));
	}
	
	@Test
	public void testRenderActionAlterColumn() throws IOException {
		DDLAction action = createActionAlterColumn();
		
		DatabaseProvider provider  = new EmbeddedDerbyDatabaseProvider("", "", "");
		assertEquals(new String[0], provider.renderAction(action));
		
		provider = new HSQLDatabaseProvider("", "", "");
		assertEquals(readStatements("hsqldb-alter-column.sql"), provider.renderAction(action));
		
		provider = new JTDSSQLServerDatabaseProvider("", "", "");
		assertEquals(readStatements("sqlserver-alter-column.sql"), provider.renderAction(action));
		
		provider = new MySQLDatabaseProvider("", "", "");
		assertEquals(readStatements("mysql-alter-column.sql"), provider.renderAction(action));
		
		provider = new OracleDatabaseProvider("", "", "");
		assertEquals(readStatements("oracle-alter-column.sql"), provider.renderAction(action));
		
		provider = new PostgreSQLDatabaseProvider("", "", "");
		assertEquals(readStatements("postgres-alter-column.sql"), provider.renderAction(action));
	}
	
	@Test
	public void testRenderActionDropColumn() throws IOException {
		DDLAction action = createActionDropColumn();
		
		DatabaseProvider provider = new EmbeddedDerbyDatabaseProvider("", "", "");
		assertEquals(new String[0], provider.renderAction(action));
		
		provider = new HSQLDatabaseProvider("", "", "");
		assertEquals(readStatements("hsqldb-drop-column.sql"), provider.renderAction(action));
		
		provider = new JTDSSQLServerDatabaseProvider("", "", "");
		assertEquals(readStatements("sqlserver-drop-column.sql"), provider.renderAction(action));
		
		provider = new MySQLDatabaseProvider("", "", "");
		assertEquals(readStatements("mysql-drop-column.sql"), provider.renderAction(action));
		
		provider = new OracleDatabaseProvider("", "", "");
		assertEquals(readStatements("oracle-drop-column.sql"), provider.renderAction(action));
		
		provider = new PostgreSQLDatabaseProvider("", "", "");
		assertEquals(readStatements("postgres-drop-column.sql"), provider.renderAction(action));
	}
	
	@Test
	public void testRenderActionCreateIndex() throws IOException {
		DDLAction action = createActionCreateIndex();
		
		DatabaseProvider provider = new EmbeddedDerbyDatabaseProvider("", "", "");
		assertEquals(readStatements("derby-create-index.sql"), provider.renderAction(action));
		
		provider = new HSQLDatabaseProvider("", "", "");
		assertEquals(readStatements("hsqldb-create-index.sql"), provider.renderAction(action));
		
		provider = new JTDSSQLServerDatabaseProvider("", "", "");
		assertEquals(readStatements("sqlserver-create-index.sql"), provider.renderAction(action));
		
		provider = new MySQLDatabaseProvider("", "", "");
		assertEquals(readStatements("mysql-create-index.sql"), provider.renderAction(action));
		
		provider = new OracleDatabaseProvider("", "", "");
		assertEquals(readStatements("oracle-create-index.sql"), provider.renderAction(action));
		
		provider = new PostgreSQLDatabaseProvider("", "", "");
		assertEquals(readStatements("postgres-create-index.sql"), provider.renderAction(action));
	}
	
	@Test
	public void testRenderActionDropIndex() throws IOException {
		DDLAction action = createActionDropIndex();
		
		DatabaseProvider provider = new EmbeddedDerbyDatabaseProvider("", "", "");
		assertEquals(readStatements("derby-drop-index.sql"), provider.renderAction(action));
		
		provider = new HSQLDatabaseProvider("", "", "");
		assertEquals(readStatements("hsqldb-drop-index.sql"), provider.renderAction(action));
		
		provider = new JTDSSQLServerDatabaseProvider("", "", "");
		assertEquals(readStatements("sqlserver-drop-index.sql"), provider.renderAction(action));
		
		provider = new MySQLDatabaseProvider("", "", "");
		assertEquals(readStatements("mysql-drop-index.sql"), provider.renderAction(action));
		
		provider = new OracleDatabaseProvider("", "", "");
		assertEquals(readStatements("oracle-drop-index.sql"), provider.renderAction(action));
		
		provider = new PostgreSQLDatabaseProvider("", "", "");
		assertEquals(readStatements("postgres-drop-index.sql"), provider.renderAction(action));
	}
	
	@Test
	public void testRenderActionAddKey() throws IOException {
		DDLAction action = createActionAddKey();
		
		DatabaseProvider provider = new EmbeddedDerbyDatabaseProvider("", "", "");
		assertEquals(readStatements("derby-add-key.sql"), provider.renderAction(action));
		
		provider = new HSQLDatabaseProvider("", "", "");
		assertEquals(readStatements("hsqldb-add-key.sql"), provider.renderAction(action));
		
		provider = new JTDSSQLServerDatabaseProvider("", "", "");
		assertEquals(readStatements("sqlserver-add-key.sql"), provider.renderAction(action));
		
		provider = new MySQLDatabaseProvider("", "", "");
		assertEquals(readStatements("mysql-add-key.sql"), provider.renderAction(action));
		
		provider = new OracleDatabaseProvider("", "", "");
		assertEquals(readStatements("oracle-add-key.sql"), provider.renderAction(action));
		
		provider = new PostgreSQLDatabaseProvider("", "", "");
		assertEquals(readStatements("postgres-add-key.sql"), provider.renderAction(action));
	}
	
	@Test
	public void testRenderActionDropKey() throws IOException {
		DDLAction action = createActionDropKey();
		
		DatabaseProvider provider = new EmbeddedDerbyDatabaseProvider("", "", "");
		assertEquals(readStatements("derby-drop-key.sql"), provider.renderAction(action));
		
		provider = new HSQLDatabaseProvider("", "", "");
		assertEquals(readStatements("hsqldb-drop-key.sql"), provider.renderAction(action));
		
		provider = new JTDSSQLServerDatabaseProvider("", "", "");
		assertEquals(readStatements("sqlserver-drop-key.sql"), provider.renderAction(action));
		
		provider = new MySQLDatabaseProvider("", "", "");
		assertEquals(readStatements("mysql-drop-key.sql"), provider.renderAction(action));
		
		provider = new OracleDatabaseProvider("", "", "");
		assertEquals(readStatements("oracle-drop-key.sql"), provider.renderAction(action));
		
		provider = new PostgreSQLDatabaseProvider("", "", "");
		assertEquals(readStatements("postgres-drop-key.sql"), provider.renderAction(action));
	}
	
	@After
	public void tearDown() {
		System.setErr(STDERR);
	}
	
	private DDLAction createActionCreateTable() {
		TypeManager tm = TypeManager.getInstance();
		tm.addType(new ClassType());
		
		DDLTable table = new DDLTable();
		table.setName("person");
		
		DDLField[] fields = new DDLField[10];
		table.setFields(fields);
		
		fields[0] = new DDLField();
		fields[0].setName("id");
		fields[0].setType(tm.getType(int.class));
		fields[0].setAutoIncrement(true);
		fields[0].setPrimaryKey(true);
		fields[0].setNotNull(true);
		
		fields[1] = new DDLField();
		fields[1].setName("firstName");
		fields[1].setType(tm.getType(String.class));
		fields[1].setNotNull(true);
		
		fields[2] = new DDLField();
		fields[2].setName("lastName");
		fields[2].setType(tm.getType(Types.CLOB));
		
		fields[3] = new DDLField();
		fields[3].setName("age");
		fields[3].setType(tm.getType(int.class));
		fields[3].setPrecision(12);
		
		fields[4] = new DDLField();
		fields[4].setName("url");
		fields[4].setType(tm.getType(URL.class));
		fields[4].setUnique(true);
		
		fields[5] = new DDLField();
		fields[5].setName("favoriteClass");
		fields[5].setType(tm.getType(Class.class));
		
		fields[6] = new DDLField();
		fields[6].setName("height");
		fields[6].setType(tm.getType(double.class));
		fields[6].setPrecision(32);
		fields[6].setScale(6);
		fields[6].setDefaultValue(62.3);
		
		fields[7] = new DDLField();
		fields[7].setName("companyID");
		fields[7].setType(tm.getType(Company.class));
		
		fields[8] = new DDLField();
		fields[8].setName("cool");
		fields[8].setType(tm.getType(boolean.class));
		fields[8].setDefaultValue(true);
		
		fields[9] = new DDLField();
		fields[9].setName("created");
		fields[9].setType(tm.getType(Calendar.class));
		fields[9].setDefaultValue(DatabaseFunction.CURRENT_TIMESTAMP);
		
		DDLForeignKey[] keys = new DDLForeignKey[1];
		table.setForeignKeys(keys);
		
		keys[0] = new DDLForeignKey();
		keys[0].setDomesticTable("person");
		keys[0].setField("companyID");
		keys[0].setForeignField("id");
		keys[0].setTable("company");
		
		DDLAction back = new DDLAction(DDLActionType.CREATE);
		back.setTable(table);
		
		return back;
	}
	
	private DDLAction createActionDropTable() {
		DDLAction back = new DDLAction(DDLActionType.DROP);
		
		DDLTable table = new DDLTable();
		table.setName("person");
		back.setTable(table);
		
		DDLField idField = new DDLField();
		idField.setName("id");
		idField.setType(TypeManager.getInstance().getType(int.class));
		idField.setAutoIncrement(true);
		idField.setNotNull(true);
		idField.setPrimaryKey(true);
		
		DDLField nameField = new DDLField();
		nameField.setName("name");
		nameField.setType(TypeManager.getInstance().getType(String.class));
		
		table.setFields(new DDLField[] {idField, nameField});
		
		return back;
	}
	
	private DDLAction createActionAddColumn() {
		DDLTable table = new DDLTable();
		table.setName("company");
		
		DDLField field = new DDLField();
		field.setName("name");
		field.setType(TypeManager.getInstance().getType(String.class));
		field.setNotNull(true);
		
		DDLAction back = new DDLAction(DDLActionType.ALTER_ADD_COLUMN);
		back.setField(field);
		back.setTable(table);
		
		return back;
	}
	
	private DDLAction createActionAlterColumn() {
		DDLTable table = new DDLTable();
		table.setName("company");
		
		DDLField oldField = new DDLField();
		oldField.setName("name");
		oldField.setType(TypeManager.getInstance().getType(int.class));
		oldField.setNotNull(false);
		table.setFields(new DDLField[] {oldField});
		
		DDLField field = new DDLField();
		field.setName("name");
		field.setType(TypeManager.getInstance().getType(String.class));
		field.setNotNull(true);
		
		DDLAction back = new DDLAction(DDLActionType.ALTER_CHANGE_COLUMN);
		back.setOldField(oldField);
		back.setField(field);
		back.setTable(table);
		
		return back;
	}
	
	private DDLAction createActionDropColumn() {
		DDLTable table = new DDLTable();
		table.setName("company");
		
		DDLField field = new DDLField();
		field.setName("name");
		field.setType(TypeManager.getInstance().getType(String.class));
		field.setNotNull(true);
		
		DDLAction back = new DDLAction(DDLActionType.ALTER_DROP_COLUMN);
		back.setField(field);
		back.setTable(table);
		
		return back;
	}
	
	private DDLAction createActionAddKey() {
		DDLAction back = new DDLAction(DDLActionType.ALTER_ADD_KEY);
		
		DDLForeignKey key = new DDLForeignKey();
		key.setDomesticTable("person");
		key.setField("companyID");
		key.setForeignField("id");
		key.setTable("company");
		back.setKey(key);
		
		return back;
	}
	
	private DDLAction createActionDropKey() {
		DDLAction back = new DDLAction(DDLActionType.ALTER_DROP_KEY);
		
		DDLForeignKey key = new DDLForeignKey();
		key.setDomesticTable("person");
		key.setField("companyID");
		key.setForeignField("id");
		key.setTable("company");
		back.setKey(key);
		
		return back;
	}
	
	private DDLAction createActionCreateIndex() {
		DDLAction back = new DDLAction(DDLActionType.CREATE_INDEX);
		
		DDLIndex index = new DDLIndex();
		index.setField("companyID");
		index.setTable("person");
		index.setType(TypeManager.getInstance().getType(Types.VARCHAR));
		back.setIndex(index);
		
		return back;
	}
	
	private DDLAction createActionDropIndex() {
		DDLAction back = new DDLAction(DDLActionType.DROP_INDEX);
		
		DDLIndex index = new DDLIndex();
		index.setField("companyID");
		index.setTable("person");
		index.setType(TypeManager.getInstance().getType(Types.VARCHAR));
		back.setIndex(index);
		
		return back;
	}
	
	private String[] readStatements(String resource) throws IOException {
		StringBuilder back = new StringBuilder();
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				getClass().getResourceAsStream("/net/java/ao/db/" + resource)));
		String cur = null;
		while ((cur = reader.readLine()) != null) {
			back.append(cur).append('\n');
		}
		reader.close();
		
		back.setLength(back.length() - 1);
		
		return back.toString().split("\n\n");
	}
	
	public static final void assertEquals(String[] array1, String[] array2) {
		if (array1.length != array2.length) {
			Assert.fail("Array lengths are not equal: expected " + array1.length + " got " + array2.length);
		}
		
		for (int i = 0; i < array1.length; i++) {
			Assert.assertEquals(array1[i].trim(), array2[i].trim());
		}
	}
}
