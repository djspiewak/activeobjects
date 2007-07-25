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
import java.sql.SQLException;
import java.sql.Types;

import net.java.ao.DBParam;
import net.java.ao.DatabaseFunction;
import net.java.ao.DatabaseProvider;
import net.java.ao.Query;
import net.java.ao.schema.PluggableNameConverter;
import net.java.ao.schema.ddl.DDLField;
import net.java.ao.schema.ddl.DDLTable;

/**
 * @author Daniel Spiewak
 */
public class SQLServerDatabaseProvider extends DatabaseProvider {

	public SQLServerDatabaseProvider(String uri, String username, String password) {
		super(uri, username, password);
	}

	@Override
	public Class<? extends Driver> getDriverClass() throws ClassNotFoundException {
		return (Class<? extends Driver>) Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
	}
	
	@Override
	protected String renderQuerySelect(Query query, PluggableNameConverter converter, boolean count) {
		StringBuilder sql = new StringBuilder();
		String tableName = query.getTable();
		
		if (tableName == null) {
			tableName = converter.getName(query.getTableType());
		}
		
		switch (query.getType()) {
			case SELECT:
				sql.append("SELECT ");
				
				if (query.isDistinct()) {
					sql.append("DISTINCT ");
				}
				
				int limit = query.getLimit();
				if (limit >= 0) {
					sql.append("TOP ").append(limit).append(' ');
				}
				
				if (count) {
					sql.append("COUNT(*)");
				} else {
					StringBuilder fields = new StringBuilder();
					for (String field : query.getFields()) {
						fields.append(field).append(',');
					}
					if (query.getFields().length > 0) {
						fields.setLength(fields.length() - 1);
					}
					
					sql.append(fields);
				}
				sql.append(" FROM ");
				
				sql.append(tableName);
			break;
		}
		
		return sql.toString();
	}
	
	@Override
	protected String renderQueryLimit(Query query) {
		return "";
	}

	@Override
	protected String renderAutoIncrement() {
		return "IDENTITY(1,1)";
	}
	
	@Override
	protected String renderOnUpdate(DDLField field) {
		return "";
	}

	@Override
	protected String convertTypeToString(int type) {
		switch (type) {
			case Types.BOOLEAN:
				return "INTEGER";
			
			case Types.TIMESTAMP:
				return "DATETIME";
				
			case Types.DATE:
				return "SMALLDATETIME";
				
			case Types.CLOB:
				return "NTEXT";
		}
		
		return super.convertTypeToString(type);
	}
	
	@Override
	protected String renderFunction(DatabaseFunction func) {
		switch (func) {
			case CURRENT_DATE:
				return "GetDate()";
				
			case CURRENT_TIMESTAMP:
				return "GetDate()";
		}
		
		return super.renderFunction(func);
	}
	
	@Override
	protected String renderTriggerForField(DDLTable table, DDLField field) {
		Object onUpdate = field.getOnUpdate();
		if (onUpdate != null) {
			StringBuilder back = new StringBuilder();
			back.append("CREATE TRIGGER ").append(table.getName()).append('_').append(field.getName()).append("_onupdate\n");
			back.append("ON ").append(table.getName()).append("\n");
			back.append("FOR UPDATE\nAS\n");
			back.append("    UPDATE ").append(table.getName()).append(" SET ").append(field.getName());
			back.append(" = ").append(renderValue(onUpdate)).append(" WHERE id = (SELECT id FROM inserted)");
			
			return back.toString();
		}
		
		return super.renderTriggerForField(table, field);
	}
	
	@Override
	public synchronized int insertReturningKeys(Connection conn, String table, DBParam... params) throws SQLException {
		boolean identityInsert = false;
		StringBuilder sql = new StringBuilder();
		
		for (DBParam param : params) {
			if (param.getField().trim().equalsIgnoreCase("id")) {
				identityInsert = true;
				sql.append("SET IDENTITY_INSERT ").append(table).append(" ON\n");
				break;
			}
		}
		
		sql.append("INSERT INTO ").append(table);
		
		if (params.length > 0) {
			sql.append(" (");
			for (DBParam param : params) {
				sql.append(param.getField());
				sql.append(',');
			}
			sql.setLength(sql.length() - 1);
			
			sql.append(") VALUES (");
			
			for (@SuppressWarnings("unused") DBParam param : params) {
				sql.append("?,");
			}
			sql.setLength(sql.length() - 1);
			
			sql.append(")");
		} else {
			sql.append(" DEFAULT VALUES");
		}
		
		if (identityInsert) {
			sql.append("\nSET IDENTITY_INSERT ").append(table).append(" OFF");
		}
		
		int back = executeInsertReturningKeys(conn, sql.toString(), params);
		
		return back;
	}
}
