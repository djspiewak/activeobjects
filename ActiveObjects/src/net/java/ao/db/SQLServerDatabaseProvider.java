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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.ao.DBParam;
import net.java.ao.DatabaseFunction;
import net.java.ao.DatabaseProvider;
import net.java.ao.Query;
import net.java.ao.schema.TableNameConverter;
import net.java.ao.schema.ddl.DDLField;
import net.java.ao.schema.ddl.DDLForeignKey;
import net.java.ao.schema.ddl.DDLTable;
import net.java.ao.types.DatabaseType;

/**
 * @author Daniel Spiewak
 */
public class SQLServerDatabaseProvider extends DatabaseProvider {
	private static final Pattern VALUE_PATTERN = Pattern.compile("^\\((.*?)\\)$");

	public SQLServerDatabaseProvider(String uri, String username, String password) {
		super(uri, username, password);
	}

	@Override
	public Class<? extends Driver> getDriverClass() throws ClassNotFoundException {
		return (Class<? extends Driver>) Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
	}
	
	@Override
	public void setQueryResultSetProperties(ResultSet res, Query query) throws SQLException {
		if (query.getOffset() >= 0) {
			res.absolute(query.getOffset());
		}
	}
	
	@Override
	public ResultSet getTables(Connection conn) throws SQLException {
		return conn.getMetaData().getTables(null, "dbo", null, new String[] {"TABLE"});
	}
	
	@Override
	public Object parseValue(int type, String value) {
		if (value == null || value.equals("") || value.equals("NULL")) {
			return null;
		}
		
		Matcher valueMatcher = VALUE_PATTERN.matcher(value);
		while (valueMatcher.matches()) {
			value = valueMatcher.group(1);
			valueMatcher = VALUE_PATTERN.matcher(value);
		}
		
		switch (type) {
			case Types.TIMESTAMP:
				Matcher matcher = Pattern.compile("'(.+)'.*").matcher(value);
				if (matcher.find()) {
					value = matcher.group(1);
				}
			break;

			case Types.DATE:
				matcher = Pattern.compile("'(.+)'.*").matcher(value);
				if (matcher.find()) {
					value = matcher.group(1);
				}
			break;
			
			case Types.TIME:
				matcher = Pattern.compile("'(.+)'.*").matcher(value);
				if (matcher.find()) {
					value = matcher.group(1);
				}
			break;
			
			case Types.BIT:
				try {
					return Byte.parseByte(value);
				} catch (Throwable t) {
					try {
						return Boolean.parseBoolean(value);
					} catch (Throwable t1) {
						return null;
					}
				}
		}
		
		return super.parseValue(type, value);
	}
	
	@Override
	protected String renderQuerySelect(Query query, TableNameConverter converter, boolean count) {
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
					if (query.getOffset() > 0) {
						limit += query.getOffset();
					}
					
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
	protected String convertTypeToString(DatabaseType<?> type) {
		switch (type.getType()) {
			case Types.BOOLEAN:
				return "INTEGER";
			
			case Types.DOUBLE:
				return "DECIMAL";
				
			case Types.TIMESTAMP:
				return "DATETIME";
				
			case Types.DATE:
				return "SMALLDATETIME";
				
			case Types.CLOB:
				return "NTEXT";
				
			case Types.BLOB:
				return "IMAGE";
		}
		
		return super.convertTypeToString(type);
	}
	
	@Override
	protected boolean considerPrecision(DDLField field) {
		switch (field.getType().getType()) {
			case Types.INTEGER:
				return false;
		}
		
		return super.considerPrecision(field);
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
			
			DDLField pkField = null;
			for (DDLField f : table.getFields()) {
				if (f.isPrimaryKey()) {
					pkField = f;
					break;
				}
			}
			
			if (pkField == null) {
				throw new IllegalArgumentException("No primary key field found in table '" + table.getName() + '\'');
			}
			
			back.append("CREATE TRIGGER ").append(table.getName()).append('_').append(field.getName()).append("_onupdate\n");
			back.append("ON ").append(table.getName()).append("\n");
			back.append("FOR UPDATE\nAS\n");
			back.append("    UPDATE ").append(table.getName()).append(" SET ").append(field.getName());
			back.append(" = ").append(renderValue(onUpdate));
			back.append(" WHERE " + pkField.getName() + " = (SELECT " + pkField.getName() + " FROM inserted)");
			
			return back.toString();
		}
		
		return super.renderTriggerForField(table, field);
	}
	
	@Override
	protected String renderAlterTableChangeColumnStatement(DDLTable table, DDLField oldField, DDLField field) {
		StringBuilder current = new StringBuilder();
		
		current.append("ALTER TABLE ").append(table.getName()).append(" ALTER COLUMN ");
		current.append(renderField(field));
		
		return current.toString();
	}
	
	@Override
	protected String[] renderAlterTableAddColumn(DDLTable table, DDLField field) {
		List<String> back = new ArrayList<String>();
		
		back.add("ALTER TABLE " + table.getName() + " ADD " + renderField(field));
		
		String function = renderFunctionForField(table, field);
		if (function != null) {
			back.add(function);
		}
		
		String trigger = renderTriggerForField(table, field);
		if (trigger != null) {
			back.add(trigger);
		}
		
		return back.toArray(new String[back.size()]);
	}
	
	@Override
	protected String renderAlterTableDropKey(DDLForeignKey key) {
		StringBuilder back = new StringBuilder("ALTER TABLE ");
		
		back.append(key.getDomesticTable()).append(" DROP CONSTRAINT ").append(key.getFKName());
		
		return back.toString();
	}
	
	@Override
	@SuppressWarnings("unused")
	public synchronized <T> T insertReturningKey(Connection conn, Class<T> pkType, String pkField, boolean pkIdentity, 
			String table, DBParam... params) throws SQLException {
		boolean identityInsert = false;
		StringBuilder sql = new StringBuilder();
		
		if (pkIdentity) {
			for (DBParam param : params) {
				if (param.getField().trim().equalsIgnoreCase(pkField)) {
					identityInsert = true;
					sql.append("SET IDENTITY_INSERT ").append(table).append(" ON\n");
					break;
				}
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
			
			for (DBParam param : params) {
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
		
		T back = executeInsertReturningKey(conn, pkType, pkField, sql.toString(), params);
		
		return back;
	}
}
