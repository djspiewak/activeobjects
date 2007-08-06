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

import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import net.java.ao.DatabaseFunction;
import net.java.ao.DatabaseProvider;
import net.java.ao.schema.ddl.DDLField;
import net.java.ao.schema.ddl.DDLTable;

/**
 * @author Daniel Spiewak
 */
public class OracleDatabaseProvider extends DatabaseProvider {

	public OracleDatabaseProvider(String uri, String username, String password) {
		super(uri, username, password);
	}

	@Override
	public Class<? extends Driver> getDriverClass() throws ClassNotFoundException {
		return (Class<? extends Driver>) Class.forName("oracle.jdbc.OracleDriver");
	}
	
	@Override
	public ResultSet getTables(Connection conn) throws SQLException {
		System.err.println("WARNING: Due to a bug in Oracle's JDBC Driver,");
		System.err.println("WARNING: table retrieval will fail with an error");
		System.err.println("WARNING: in native code.");
		
		return super.getTables(conn);
	}
	
	@Override
	protected String convertTypeToString(int type) {
		switch (type) {
			case Types.BIGINT:
				return "NUMBER";
				
			case Types.BOOLEAN:
				return "NUMBER";
			
			case Types.INTEGER:
				return "NUMBER";

			case Types.NUMERIC:
				return "NUMBER";
				
			case Types.DECIMAL:
				return "NUMBER";
				
			case Types.SMALLINT:
				return "NUMBER";
				
			case Types.FLOAT:
				return "NUMBER";
				
			case Types.DOUBLE:
				return "NUMBER";
				
			case Types.REAL:
				return "NUMBER";
		}
		
		return super.convertTypeToString(type);
	}
	
	@Override
	protected String renderAutoIncrement() {
		return "";
	}
	
	@Override
	protected String renderOnUpdate(DDLField field) {
		return "";
	}
	
	@Override
	protected String renderFunction(DatabaseFunction func) {
		switch (func) {
			case CURRENT_TIMESTAMP:
				return "SYSDATE";
			
			case CURRENT_DATE:
				return "SYSDATE";
		}
		
		return super.renderFunction(func);
	}

	@Override
	protected String getDateFormat() {
		return "dd-MMM-yy hh:mm:ss.SSS a";
	}
	
	@Override
	protected String renderTriggerForField(DDLTable table, DDLField field) {
		if (field.getOnUpdate() != null) {
			StringBuilder back = new StringBuilder();
			String value = renderValue(field.getOnUpdate());
			
			back.append("CREATE TRIGGER ").append(table.getName()).append('_').append(field.getName()).append("_onupdate\n");
			back.append("BEFORE UPDATE\n").append("    ON ").append(table.getName()).append("\n    FOR EACH ROW\n");
			back.append("BEGIN\n");
			back.append("    :new.").append(field.getName()).append(" := ").append(value).append(";\nEND");
			
			return back.toString();
		}
		
		return super.renderTriggerForField(table, field);
	}
}