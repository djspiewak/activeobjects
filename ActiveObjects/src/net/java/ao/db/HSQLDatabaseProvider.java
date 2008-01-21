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

import net.java.ao.Common;
import net.java.ao.DBParam;
import net.java.ao.DatabaseProvider;
import net.java.ao.Query;
import net.java.ao.RawEntity;
import net.java.ao.schema.TableNameConverter;
import net.java.ao.schema.ddl.DDLField;
import net.java.ao.schema.ddl.DDLForeignKey;
import net.java.ao.schema.ddl.DDLIndex;
import net.java.ao.schema.ddl.DDLTable;
import net.java.ao.types.DatabaseType;
import net.java.ao.types.TypeManager;

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
	
	@Override
	@SuppressWarnings("unused")
	public <T> T insertReturningKey(Connection conn, Class<T> pkType, String pkField, boolean pkIdentity, String table, DBParam... params) throws SQLException {
		StringBuilder sql = new StringBuilder("INSERT INTO " + table + " (");
		
		for (DBParam param : params) {
			sql.append(param.getField());
			sql.append(',');
		}
		if (params.length > 0) {
			sql.setLength(sql.length() - 1);
		} else {
			sql.append(pkField);
		}
		
		sql.append(") VALUES (");
		
		for (DBParam param : params) {
			sql.append("?,");
		}
		if (params.length > 0) {
			sql.setLength(sql.length() - 1);
		} else {
			sql.append("NULL");
		}
		
		sql.append(")");
		
		return executeInsertReturningKey(conn, pkType, pkField, sql.toString(), params);
	}
	
	@Override
	protected synchronized <T> T executeInsertReturningKey(Connection conn, Class<T> pkType, String pkField, String sql, 
			DBParam... params) throws SQLException {
		T back = null;
		
		Logger.getLogger("net.java.ao").log(Level.INFO, sql);
		PreparedStatement stmt = conn.prepareStatement(sql);
		
		for (int i = 0; i < params.length; i++) {
			Object value = params[i].getValue();
			
			if (value instanceof RawEntity) {
				value = Common.getPrimaryKeyValue((RawEntity<Object>) value);
			}
			
			if (params[i].getField().equalsIgnoreCase(pkField)) {
				back = (T) value;
			}
			
			if (value == null) {
				putNull(stmt, i + 1);
			} else {
				DatabaseType<Object> type = (DatabaseType<Object>) TypeManager.getInstance().getType(value.getClass());
				type.putToDatabase(i + 1, stmt, value);
			}
		}
		
		stmt.executeUpdate();
		stmt.close();
		
		if (back == null) {
			stmt = conn.prepareStatement("CALL IDENTITY()");		// WARNING	potential breakage here if dealing with INSERTs outside ORM control
			
			ResultSet res = stmt.executeQuery();
			if (res.next()) {
				 back = TypeManager.getInstance().getType(pkType).pullFromDatabase(null, res, pkType, 1);
			}
			res.close();
			stmt.close();
		}
		
		return back;
	}
	
	@Override
	public Object parseValue(int type, String value) {
		if (value == null || value.equals("") || value.equals("NULL")) {
			return null;
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
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException t) {
				}
			}
		}
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
					int offset = Math.max(query.getOffset(), 0);
					
					sql.append("LIMIT ").append(offset).append(' ');
					sql.append(limit).append(' ');
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
	protected String renderUnique() {
		return "";
	}
	
	@Override
	protected String renderConstraintsForTable(DDLTable table) {
		StringBuilder back = new StringBuilder(super.renderConstraintsForTable(table));
		
		for (DDLField field : table.getFields()) {
			if (field.isUnique()) {
				back.append("    UNIQUE(").append(field.getName()).append("),\n");
			}
		}
		
		return back.toString();
	}
	
	@Override
	protected String convertTypeToString(DatabaseType<?> type) {
		switch (type.getType()) {
			case Types.CLOB:
				return "LONGVARCHAR";
				
			case Types.BLOB:
				return "BINARY";
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
	protected String renderValue(Object value) {
		if (value instanceof Boolean) {
			if (value.equals(true)) {
				return "TRUE";
			}
			
			return "FALSE";
		}
		
		return super.renderValue(value);
	}
	
	@Override
	protected String renderAlterTableChangeColumnStatement(DDLTable table, DDLField oldField, DDLField field) {
		StringBuilder current = new StringBuilder();
		
		current.append("ALTER TABLE ").append(table.getName()).append(" ALTER COLUMN ");
		current.append(renderField(field));
		
		return current.toString();
	}
	
	@Override
	protected String renderAlterTableDropKey(DDLForeignKey key) {
		StringBuilder back = new StringBuilder("ALTER TABLE ");
		
		back.append(key.getDomesticTable()).append(" DROP CONSTRAINT ").append(key.getFKName());
		
		return back.toString();
	}
	
	@Override
	protected String renderDropIndex(DDLIndex index) {
		StringBuilder back = new StringBuilder("DROP INDEX ");
		
		back.append(index.getName());
		
		return back.toString();
	}
}
