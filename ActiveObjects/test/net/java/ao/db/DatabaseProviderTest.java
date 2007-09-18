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

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

import org.junit.Test;

import test.schema.Company;

/**
 * @author Daniel Spiewak
 */
public class DatabaseProviderTest {

	@Test
	public void testRenderAction() throws IOException {
		// create table
		DDLAction action = createAction();
		
		DatabaseProvider provider = new EmbeddedDerbyDatabaseProvider("", "", "");
		assertEquals(readString("derby-create-table.sql"), provider.renderAction(action)[0]);
		
		provider = new HSQLDatabaseProvider("", "", "");
		assertEquals(readString("hsqldb-create-table.sql"), provider.renderAction(action)[0]);
		
		provider = new JTDSSQLServerDatabaseProvider("", "", "");
		assertEquals(readString("sqlserver-create-table.sql"), provider.renderAction(action)[0]);
		
		provider = new MySQLDatabaseProvider("", "", "");
		assertEquals(readString("mysql-create-table.sql"), provider.renderAction(action)[0]);
		
		provider = new OracleDatabaseProvider("", "", "");
		assertEquals(readString("oracle-create-table.sql"), provider.renderAction(action)[0]);
		
		provider = new PostgreSQLDatabaseProvider("", "", "");
		assertEquals(readString("postgres-create-table.sql"), provider.renderAction(action)[0]);action = createAction();
		
		// create index
		action = createIndexAction();
		
		provider = new EmbeddedDerbyDatabaseProvider("", "", "");
		assertEquals(readString("derby-create-index.sql"), provider.renderAction(action)[0]);
		
		provider = new HSQLDatabaseProvider("", "", "");
		assertEquals(readString("hsqldb-create-index.sql"), provider.renderAction(action)[0]);
		
		provider = new JTDSSQLServerDatabaseProvider("", "", "");
		assertEquals(readString("sqlserver-create-index.sql"), provider.renderAction(action)[0]);
		
		provider = new MySQLDatabaseProvider("", "", "");
		assertEquals(readString("mysql-create-index.sql"), provider.renderAction(action)[0]);
		
		provider = new OracleDatabaseProvider("", "", "");
		assertEquals(readString("oracle-create-index.sql"), provider.renderAction(action)[0]);
		
		provider = new PostgreSQLDatabaseProvider("", "", "");
		assertEquals(readString("postgres-create-index.sql"), provider.renderAction(action)[0]);
	}
	
	private DDLAction createAction() {
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
	
	private DDLAction createIndexAction() {
		DDLAction back = new DDLAction(DDLActionType.CREATE_INDEX);
		
		DDLIndex index = new DDLIndex();
		index.setField("companyID");
		index.setTable("person");
		back.setIndex(index);
		
		return back;
	}
	
	private String readString(String resource) throws IOException {
		StringBuilder back = new StringBuilder();
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/net/java/ao/db/" + resource)));
		String cur = null;
		while ((cur = reader.readLine()) != null) {
			back.append(cur).append('\n');
		}
		reader.close();
		
		back.setLength(back.length() - 1);
		
		return back.toString();
	}
}
