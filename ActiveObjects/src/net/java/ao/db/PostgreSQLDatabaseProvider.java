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

import net.java.ao.Common;
import net.java.ao.DBParam;
import net.java.ao.DatabaseFunction;
import net.java.ao.DatabaseProvider;
import net.java.ao.RawEntity;
import net.java.ao.schema.ddl.DDLField;
import net.java.ao.schema.ddl.DDLForeignKey;
import net.java.ao.schema.ddl.DDLIndex;
import net.java.ao.schema.ddl.DDLTable;
import net.java.ao.types.DatabaseType;
import net.java.ao.types.TypeManager;

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
	protected boolean considerPrecision(DDLField field) {
		switch (field.getType().getType()) {
			case Types.INTEGER:
				return false;
				
			case Types.DOUBLE:
				return false;
		}
		
		return super.considerPrecision(field);
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
	protected String convertTypeToString(DatabaseType<?> type) {
		if (type.getType() == Types.CLOB) {
			return "TEXT";
		} else if (type.getType() == Types.BLOB) {
			return "BYTEA";
		} else if (type.getType() == Types.DOUBLE) {
			return "DOUBLE PRECISION";
		}
		
		return super.convertTypeToString(type);
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
	protected String[] renderAlterTableChangeColumn(DDLTable table, DDLField oldField, DDLField field) {
		List<String> back = new ArrayList<String>();
		
		String trigger = getTriggerNameForField(table, oldField);
		if (trigger != null) {
			StringBuilder str = new StringBuilder();
			str.append("DROP TRIGGER ").append(trigger);
			back.add(str.toString());
		}
		
		String function = getFunctionNameForField(table, oldField);
		if (function != null) {
			StringBuilder str = new StringBuilder();
			str.append("DROP FUNCTION ").append(function);
			back.add(str.toString());
		}
		
		boolean foundChange = false;
		if (!field.getName().equalsIgnoreCase(oldField.getName())) {
			foundChange = true;
			
			StringBuilder str = new StringBuilder();
			str.append("ALTER TABLE ").append(table.getName()).append(" RENAME COLUMN ");
			str.append(oldField.getName()).append(" TO ").append(field.getName());
			back.add(str.toString());
		}
		
		if (!field.getType().equals(oldField.getType())) {
			foundChange = true;
			
			StringBuilder str = new StringBuilder();
			str.append("ALTER TABLE ").append(table.getName()).append(" ALTER COLUMN ");
			str.append(field.getName()).append(" TYPE ");
			str.append(renderFieldType(field)).append(renderFieldPrecision(field));
			back.add(str.toString());
		}
		
		if (field.getDefaultValue() == null && oldField.getDefaultValue() == null) {
			// dummy case
		} else if (field.getDefaultValue() == null && oldField.getDefaultValue() != null) {
			foundChange = true;
			
			StringBuilder str = new StringBuilder();
			str.append("ALTER TABLE ").append(table.getName()).append(" ALTER COLUMN ");
			str.append(field.getName()).append(" DROP DEFAULT");
			back.add(str.toString());
		} else if (!field.getDefaultValue().equals(oldField.getDefaultValue())) {
			foundChange = true;
			
			StringBuilder str = new StringBuilder();
			str.append("ALTER TABLE ").append(table.getName()).append(" ALTER COLUMN ");
			str.append(field.getName()).append(" SET DEFAULT ").append(renderValue(field.getDefaultValue()));
			back.add(str.toString());
		}
		
		if (field.isNotNull() != oldField.isNotNull()) {
			foundChange = true;
			
			if (field.isNotNull()) {
				StringBuilder str = new StringBuilder();
				str.append("ALTER TABLE ").append(table.getName()).append(" ALTER COLUMN ");
				str.append(field.getName()).append(" SET NOT NULL");
				back.add(str.toString());
			} else {
				StringBuilder str = new StringBuilder();
				str.append("ALTER TABLE ").append(table.getName()).append(" ALTER COLUMN ");
				str.append(field.getName()).append(" DROP NOT NULL");
				back.add(str.toString());
			}
		}
		
		if (!foundChange) {
			System.err.println("WARNING: PostgreSQL doesn't fully support CHANGE TABLE statements");
			System.err.println("WARNING: Data contained in column '" + table.getName() + "." + oldField.getName() + "' will be lost");
			
			back.addAll(Arrays.asList(renderAlterTableDropColumn(table, oldField)));
			back.addAll(Arrays.asList(renderAlterTableAddColumn(table, field)));
		}
		
		String toRender = renderFunctionForField(table, field);
		if (toRender != null) {
			back.add(toRender);
		}
		
		toRender = renderTriggerForField(table, field);
		if (toRender != null) {
			back.add(toRender);
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
	protected String renderDropIndex(DDLIndex index) {
		StringBuilder back = new StringBuilder("DROP INDEX ");
		
		back.append(index.getName());
		
		return back.toString();
	}
	
	@Override
	protected String getFunctionNameForField(DDLTable table, DDLField field) {
		if (field.getOnUpdate() != null) {
			return table.getName() + '_' + field.getName() + "_onupdate()";
		}
		
		return super.getFunctionNameForField(table, field);
	}
	
	@Override
	protected String getTriggerNameForField(DDLTable table, DDLField field) {
		if (field.getOnUpdate() != null) {
			return table.getName() + '_' + field.getName() + "_onupdate";
		}
		
		return super.getTriggerNameForField(table, field);
	}

	@Override
	public synchronized <T> T insertReturningKey(Connection conn, Class<T> pkType, String pkField, boolean pkIdentity, 
			String table, DBParam... params) throws SQLException {
		T back = null;
		for (DBParam param : params) {
			if (param.getField().trim().equalsIgnoreCase(pkField)) {
				back = (T) param.getValue();
				break;
			}
		}
		
		if (back == null) {
			String sql = "SELECT NEXTVAL('" + table + "_" + pkField + "_seq')";
			
			Logger.getLogger("net.java.ao").log(Level.INFO, sql);
			PreparedStatement stmt = conn.prepareStatement(sql);
			
			ResultSet res = stmt.executeQuery();
			if (res.next()) {
				 back = TypeManager.getInstance().getType(pkType).pullFromDatabase(null, res, pkType, 1);
			}
			res.close();
			stmt.close();
			
			List<DBParam> newParams = new ArrayList<DBParam>();
			newParams.addAll(Arrays.asList(params));
			
			newParams.add(new DBParam(pkField, back));
			params = newParams.toArray(new DBParam[newParams.size()]);
		}
		
		super.insertReturningKey(conn, pkType, pkField, pkIdentity, table, params);
		
		return back;
	}
	
	@Override
	protected <T> T executeInsertReturningKey(Connection conn, Class<T> pkType, String pkField, String sql, 
			DBParam... params) throws SQLException {
		Logger.getLogger("net.java.ao").log(Level.INFO, sql);
		PreparedStatement stmt = conn.prepareStatement(sql);
		
		for (int i = 0; i < params.length; i++) {
			Object value = params[i].getValue();
			
			if (value instanceof RawEntity) {
				value = Common.getPrimaryKeyValue((RawEntity<Object>) value);
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
		
		return null;
	}
	
	@Override
	public void putNull(PreparedStatement stmt, int index) throws SQLException {
		stmt.setNull(index, Types.INTEGER);
	}
}
