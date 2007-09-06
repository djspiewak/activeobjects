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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.ao.DBParam;
import net.java.ao.DatabaseProvider;
import net.java.ao.Entity;
import net.java.ao.Query;
import net.java.ao.schema.PluggableTableNameConverter;
import net.java.ao.schema.ddl.DDLField;
import net.java.ao.types.DatabaseType;

/**
 * @author Daniel Spiewak
 */
public class HSQLDatabaseProvider extends DatabaseProvider {

	public HSQLDatabaseProvider(String uri, String username, String password) {
		super(uri, username, password);
	}

	@Override
	public Class<? extends Driver> getDriverClass() throws ClassNotFoundException {
		return (Class<? extends Driver>) Class.forName("org.hsqldb.jdbcDriver");
	}
	
	public int insertReturningKeys(Connection conn, String table, DBParam... params) throws SQLException {
		StringBuilder sql = new StringBuilder("INSERT INTO " + table + " (");
		
		for (DBParam param : params) {
			sql.append(param.getField());
			sql.append(',');
		}
		if (params.length > 0) {
			sql.setLength(sql.length() - 1);
		} else {
			sql.append("id");
		}
		
		sql.append(") VALUES (");
		
		for (@SuppressWarnings("unused") DBParam param : params) {
			sql.append("?,");
		}
		if (params.length > 0) {
			sql.setLength(sql.length() - 1);
		} else {
			sql.append("NULL");
		}
		
		sql.append(")");
		
		return executeInsertReturningKeys(conn, sql.toString(), params);
	}
	
	@Override
	protected synchronized int executeInsertReturningKeys(Connection conn, String sql, DBParam... params) throws SQLException {
		int back = -1;
		Logger.getLogger("net.java.ao").log(Level.INFO, sql);
		PreparedStatement stmt = conn.prepareStatement(sql);
		
		for (int i = 0; i < params.length; i++) {
			Object value = params[i].getValue();
			
			if (value instanceof Entity) {
				value = ((Entity) value).getID();
			}
			
			if (params[i].getField().equalsIgnoreCase("id")) {
				back = (Integer) value;
			}
			
			stmt.setObject(i + 1, value);
		}
		
		stmt.executeUpdate();
		stmt.close();
		
		if (back < 0) {
			stmt = conn.prepareStatement("CALL IDENTITY()");		// WARNING	potential breakage here if dealing with INSERTs outside ORM control
			
			ResultSet res = stmt.executeQuery();
			if (res.next()) {
				back = res.getInt(1);
			}
			res.close();
			stmt.close();
		}
		
		return back;
	}
	
	@Override
	public Object parseValue(int type, String value) {
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
		}
		
		return super.parseValue(type, value);
	}
	
	@Override
	public ResultSet getTables(Connection conn) throws SQLException {
		return conn.getMetaData().getTables(null, "PUBLIC", null, new String[] {"TABLE"});
	}
	
	@Override
	public void dispose() {
		Connection conn = null;
		try {
			conn = getConnection();
			Statement stmt = conn.createStatement();
			
			stmt.executeUpdate("SHUTDOWN");
			stmt.close();
		} catch (SQLException e) {
		} finally {
			try {
				conn.close();
			} catch (Throwable t) {
			}
		}
	}
	
	@Override
	protected String renderQuerySelect(Query query, PluggableTableNameConverter converter, boolean count) {
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
		return "GENERATED BY DEFAULT AS IDENTITY";
	}
	
	@Override
	protected String getDateFormat() {
		return "yyyy-MM-dd HH:mm:ss.SSS";
	}
	
	@Override
	protected String renderOnUpdate(DDLField field) {
		System.err.println("WARNING: @OnUpdate is a currently unsupported feature in HSQLDB");
		
		return "";
	}
	
	@Override
	protected String convertTypeToString(DatabaseType<?> type) {
		switch (type.getType()) {
			case Types.CLOB:
				return "LONGVARCHAR";
		}
		
		return super.convertTypeToString(type);
	}
	
	@Override
	protected String renderValue(Object value) {
		if (value instanceof Boolean) {
			if (value.equals(true)) {
				return "TRUE";
			} else {
				return "FALSE";
			}
		}
		
		return super.renderValue(value);
	}
}
