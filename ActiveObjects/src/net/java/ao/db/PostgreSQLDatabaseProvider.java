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
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.ao.DBParam;
import net.java.ao.DatabaseFunction;
import net.java.ao.DatabaseProvider;
import net.java.ao.Entity;
import net.java.ao.schema.ddl.DDLField;
import net.java.ao.schema.ddl.DDLTable;

/**
 * @author Daniel Spiewak
 */
public class PostgreSQLDatabaseProvider extends DatabaseProvider {

	public PostgreSQLDatabaseProvider(String uri, String username, String password) {
		super(uri, username, password);
	}

	@Override
	public Class<? extends Driver> getDriverClass() throws ClassNotFoundException {
		return (Class<? extends Driver>) Class.forName("org.postgresql.Driver");
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
	public ResultSet getTables(Connection conn) throws SQLException {
		return conn.getMetaData().getTables("public", null, null, new String[] {"TABLE"});
	}

	@Override
	protected String renderAutoIncrement() {
		return "";
	}
	
	@Override
	protected String renderFieldType(DDLField field) {
		if (field.isAutoIncrement()) {
			return "SERIAL";
		}
		
		return super.renderFieldType(field);
	}
	
	@Override
	protected String convertTypeToString(int type) {
		if (type == Types.CLOB) {
			return "TEXT";
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
	
	@Override
	protected String renderFunction(DatabaseFunction func) {
		switch (func) {
			case CURRENT_DATE:
				return "now()";
				
			case CURRENT_TIMESTAMP:
				return "now()";
		}
		
		return super.renderFunction(func);
	}
	
	@Override
	protected String renderFunctionForField(DDLTable table, DDLField field) {
		Object onUpdate = field.getOnUpdate();
		if (onUpdate != null) {
			StringBuilder back = new StringBuilder();
			
			back.append("CREATE FUNCTION ").append(table.getName()).append('_').append(field.getName()).append("_onupdate()");
			back.append(" RETURNS trigger AS $$\nBEGIN\n");
			back.append("    NEW.").append(field.getName()).append(" := ").append(renderValue(onUpdate));
			back.append(";\n    RETURN NEW;\nEND;\n$$ LANGUAGE plpgsql");
			
			return back.toString();
		}
		
		return super.renderFunctionForField(table, field);
	}
	
	@Override
	protected String renderTriggerForField(DDLTable table, DDLField field) {
		Object onUpdate = field.getOnUpdate();
		if (onUpdate != null) {
			StringBuilder back = new StringBuilder();
			
			back.append("CREATE TRIGGER ").append(table.getName()).append('_').append(field.getName()).append("_onupdate\n");
			back.append(" BEFORE UPDATE OR INSERT ON ").append(field.getName()).append('\n');
			back.append("    FOR EACH ROW EXECUTE PROCEDURE ");
			back.append(table.getName()).append('_').append(field.getName()).append("_onupdate()");
		}
		
		return super.renderTriggerForField(table, field);
	}
	
	@Override
	protected String renderOnUpdate(DDLField field) {
		return "";
	}

	@Override
	public synchronized int insertReturningKeys(Connection conn, String table, DBParam... params) throws SQLException {
		int back = -1;
		for (DBParam param : params) {
			if (param.getField().trim().equalsIgnoreCase("id")) {
				back = (Integer) param.getValue();
				break;
			}
		}
		
		if (back < 0) {
			String sql = "SELECT NEXTVAL('" + table + "_id_seq')";
			
			Logger.getLogger("net.java.ao").log(Level.INFO, sql);
			PreparedStatement stmt = conn.prepareStatement(sql);
			
			ResultSet res = stmt.executeQuery();
			if (res.next()) {
				back = res.getInt(1);
			}
			res.close();
			stmt.close();
			
			List<DBParam> newParams = new ArrayList<DBParam>();
			newParams.addAll(Arrays.asList(params));
			
			newParams.add(new DBParam("id", back));
			params = newParams.toArray(new DBParam[newParams.size()]);
		}
		
		super.insertReturningKeys(conn, table, params);
		
		return back;
	}
	
	@Override
	protected int executeInsertReturningKeys(Connection conn, String sql, DBParam... params) throws SQLException {
		Logger.getLogger("net.java.ao").log(Level.INFO, sql);
		PreparedStatement stmt = conn.prepareStatement(sql);
		
		for (int i = 0; i < params.length; i++) {
			Object value = params[i].getValue();
			
			if (value instanceof Entity) {
				value = ((Entity) value).getID();
			}
			
			stmt.setObject(i + 1, value);
		}
		
		stmt.executeUpdate();
		stmt.close();
		
		return -1;
	}
}
