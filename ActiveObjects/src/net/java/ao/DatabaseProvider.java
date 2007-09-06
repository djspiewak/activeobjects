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
package net.java.ao;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.java.ao.db.SupportedDBProvider;
import net.java.ao.db.SupportedPoolProvider;
import net.java.ao.schema.TableNameConverter;
import net.java.ao.schema.ddl.DDLAction;
import net.java.ao.schema.ddl.DDLField;
import net.java.ao.schema.ddl.DDLForeignKey;
import net.java.ao.schema.ddl.DDLTable;
import net.java.ao.types.DatabaseType;

/**
 * @author Daniel Spiewak
 */
public abstract class DatabaseProvider {
	private String uri, username, password;
	
	private Map<Thread, Connection> connections;
	private final ReadWriteLock connectionsLock = new ReentrantReadWriteLock();
	
	protected DatabaseProvider(String uri, String username, String password) {
		this.uri = uri;
		
		this.username = username;
		this.password = password;
		
		connections = new HashMap<Thread, Connection>();
	}
	
	public abstract Class<? extends Driver> getDriverClass() throws ClassNotFoundException;
	
	protected abstract String renderAutoIncrement();
	
	public String[] renderAction(DDLAction action) {
		List<String> back = new ArrayList<String>();
		
		switch (action.getActionType()) {
			case CREATE:
				back.add(renderTable(action.getTable()));
				back.addAll(Arrays.asList(renderFunctions(action.getTable())));
				back.addAll(Arrays.asList(renderTriggers(action.getTable())));
			break;
			
			case DROP:
				back.addAll(Arrays.asList(renderDropTriggers(action.getTable())));
				back.addAll(Arrays.asList(renderDropFunctions(action.getTable())));
				back.add(renderDropTable(action.getTable()));
			break;
			
			case ALTER_ADD_COLUMN:
				back.addAll(Arrays.asList(renderAlterTableAddColumn(action.getTable(), action.getField())));
			break;
			
			case ALTER_CHANGE_COLUMN:
				back.addAll(Arrays.asList(renderAlterTableChangeColumn(action.getTable(), action.getOldField(), action.getField())));
			break;
			
			case ALTER_DROP_COLUMN:
				back.addAll(Arrays.asList(renderAlterTableDropColumn(action.getTable(), action.getField())));
			break;
			
			case ALTER_ADD_KEY:
				back.add(renderAlterTableAddKey(action.getKey()));
			break;
			
			case ALTER_DROP_KEY:
				back.add(renderAlterTableDropKey(action.getKey()));
			break;
		}
		
		return back.toArray(new String[back.size()]);
	}
	
	public String renderQuery(Query query, TableNameConverter converter, boolean count) {
		StringBuilder sql = new StringBuilder();
		
		sql.append(renderQuerySelect(query, converter, count));
		sql.append(renderQueryJoins(query, converter));
		sql.append(renderQueryWhere(query));
		sql.append(renderQueryGroupBy(query));
		sql.append(renderQueryOrderBy(query));
		sql.append(renderQueryLimit(query));
		
		return sql.toString();
	}
	
	public Object parseValue(int type, String value) {
		if (value == null || value.equals("") || value.equals("NULL")) {
			return null;
		}
		
		List<DatabaseFunction> posFuncs = new ArrayList<DatabaseFunction>();
		for (DatabaseFunction func : DatabaseFunction.values()) {
			if (renderFunction(func).equalsIgnoreCase(value)) {
				posFuncs.add(func);
			}
		}
		if (posFuncs.size() > 0) {
			return posFuncs.toArray(new DatabaseFunction[posFuncs.size()]);
		}
		
		try {
			switch (type) {
				case Types.BIGINT:
					return Long.parseLong(value);

				case Types.BIT:
					return Byte.parseByte(value);

				case Types.BOOLEAN:
					int intValue = -1;
					try {
						intValue = Integer.parseInt(value);
					} catch (Throwable t) {
						return Boolean.parseBoolean(value);
					}

					return intValue == 0;

				case Types.CHAR:
					value.charAt(0);

				case Types.DATE:
					try {
						Calendar back = Calendar.getInstance();
						back.setTime(new SimpleDateFormat(getDateFormat()).parse(value));
						return back;
					} catch (ParseException e) {
						return null;
					}

				case Types.DECIMAL:
					return Double.parseDouble(value);

				case Types.DOUBLE:
					return Double.parseDouble(value);

				case Types.FLOAT:
					return Float.parseFloat(value);

				case Types.INTEGER:
					return Integer.parseInt(value);

				case Types.NUMERIC:
					return Integer.parseInt(value);

				case Types.REAL:
					return Double.parseDouble(value);

				case Types.SMALLINT:
					return Short.parseShort(value);

				case Types.TIMESTAMP:
					try {
						Calendar back = Calendar.getInstance();
						back.setTime(new SimpleDateFormat(getDateFormat()).parse(value));
						return back;
					} catch (ParseException e) {
						return null;
					}

				case Types.TINYINT:
					return Short.parseShort(value);

				case Types.VARCHAR:
					return value.substring(1, value.length() - 1);
			}
		} catch (Throwable t) {}
		
		return null;
	}
	
	public void setQueryStatementProperties(Statement stmt, Query query) throws SQLException {
	}
	
	public void setQueryResultSetProperties(ResultSet res, Query query) throws SQLException {
	}
	
	public ResultSet getTables(Connection conn) throws SQLException {
		return conn.getMetaData().getTables(null, null, "", null);
	}
	
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
	
	protected String renderQueryJoins(Query query, TableNameConverter converter) {
		StringBuilder sql = new StringBuilder();

		if (query.getJoins().size() > 0) {
			for (Class<? extends Entity> join : query.getJoins().keySet()) {
				sql.append(" JOIN ");
				sql.append(converter.getName(join));
				
				String on = query.getJoins().get(join);
				if (on != null) {
					sql.append(" ON ");
					sql.append(on);
				}
			}
		}
		
		return sql.toString();
	}
	
	protected String renderQueryWhere(Query query) {
		StringBuilder sql = new StringBuilder();
		
		String whereClause = query.getWhereClause();
		if (whereClause != null) {
			sql.append(" WHERE ");
			sql.append(whereClause);
		}
		
		return sql.toString();
	}
	
	protected String renderQueryGroupBy(Query query) {
		StringBuilder sql = new StringBuilder();
		
		String groupClause = query.getGroupClause();
		if (groupClause != null) {
			sql.append(" GROUP BY ");
			sql.append(groupClause);
		}
		
		return sql.toString();
	}
	
	protected String renderQueryOrderBy(Query query) {
		StringBuilder sql = new StringBuilder();
		
		String orderClause = query.getOrderClause();
		if (orderClause != null) {
			sql.append(" ORDER BY ");
			sql.append(orderClause);
		}
		
		return sql.toString();
	}
	
	protected String renderQueryLimit(Query query) {
		StringBuilder sql = new StringBuilder();
		
		int limit = query.getLimit();
		if (limit >= 0) {
			sql.append(" LIMIT ");
			sql.append(limit);
		}
		
		int offset = query.getOffset();
		if (offset > 0) {
			sql.append(" OFFSET ").append(offset);
		}
		
		return sql.toString();
	}
	
	public String getURI() {
		return uri;
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public final Connection getConnection() throws SQLException {
		connectionsLock.writeLock().lock();
		try {
			Connection conn = connections.get(Thread.currentThread());
			if (conn != null && !conn.isClosed()) {
				return conn;
			}
			
			conn = new DelegateConnection(getConnectionImpl());
			connections.put(Thread.currentThread(), conn);
			
			return conn;
		} finally {
			connectionsLock.writeLock().unlock();
		}
	}
	
	protected Connection getConnectionImpl() throws SQLException {
		try {
			getDriverClass();
		} catch (ClassNotFoundException e) {
			return null;
		}
		
		Connection conn = DriverManager.getConnection(getURI(), getUsername(), getPassword());
		setPostConnectionProperties(conn);
		
		return conn;
	}
	
	public void dispose() {
	}
	
	protected void setPostConnectionProperties(Connection conn) throws SQLException {
	}
	
	protected String renderConstraintsForTable(DDLTable table) {
		StringBuilder back = new StringBuilder();
		
		for (DDLForeignKey key : table.getForeignKeys()) {
			back.append("    ").append(renderForeignKey(key)).append(",\n");
		}
		
		return back.toString();
	}
	
	protected String renderForeignKey(DDLForeignKey key) {
		StringBuilder back = new StringBuilder();
		
		back.append("CONSTRAINT ").append(key.getFKName());
		back.append(" FOREIGN KEY (").append(key.getField()).append(") REFERENCES ");
		back.append(key.getTable()).append('(').append(key.getForeignField()).append(")");
		
		return back.toString();
	}
	
	protected String convertTypeToString(DatabaseType<?> type) {
		return type.getDefaultName();
	}
	
	protected String renderTable(DDLTable table) {
		StringBuilder back = new StringBuilder("CREATE TABLE ");
		back.append(table.getName());
		back.append(" (\n");
		
		List<String> primaryKeys = new LinkedList<String>();
		StringBuilder append = new StringBuilder();
		for (DDLField field : table.getFields()) {
			back.append("    ").append(renderField(field)).append(",\n");
			
			if (field.isPrimaryKey()) {
				primaryKeys.add(field.getName());
			}
		}
		
		append.append(renderConstraintsForTable(table));
		
		back.append(append);
		
		if (primaryKeys.size() > 0) {
			back.append("    PRIMARY KEY(");
			back.append(primaryKeys.get(0));
			
			for (int i = 1; i < primaryKeys.size(); i++) {
				back.append(",");
				back.append(primaryKeys.get(i));
			}
			back.append(")\n");
		}
		
		back.append(")");
		
		String tailAppend = renderAppend();
		if (tailAppend != null) {
			back.append(' ');
			back.append(tailAppend);
		}
		
		return back.toString();
	}
	
	protected String renderDropTable(DDLTable table) {
		return "DROP TABLE " + table.getName();
	}
	
	protected String[] renderDropFunctions(DDLTable table) {
		return new String[0];
	}
	
	protected String[] renderDropTriggers(DDLTable table) {
		return new String[0];
	}

	protected String[] renderFunctions(DDLTable table) {
		List<String> back = new ArrayList<String>();
		
		for (DDLField field : table.getFields()) {
			String function = renderFunctionForField(table, field);
			if (function != null) {
				back.add(function);
			}
		}
		
		return back.toArray(new String[back.size()]);
	}
	
	protected String[] renderTriggers(DDLTable table) {
		List<String> back = new ArrayList<String>();
		
		for (DDLField field : table.getFields()) {
			String trigger = renderTriggerForField(table, field);
			if (trigger != null) {
				back.add(trigger);
			}
		}
		
		return back.toArray(new String[back.size()]);
	}
	
	protected String[] renderAlterTableAddColumn(DDLTable table, DDLField field) {
		List<String> back = new ArrayList<String>();
		
		back.add("ALTER TABLE " + table.getName() + " ADD COLUMN " + renderField(field));
		
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
	
	protected String[] renderAlterTableChangeColumn(DDLTable table, DDLField oldField, DDLField field) {
		List<String> back = new ArrayList<String>();
		StringBuilder current = new StringBuilder();
		
		String trigger = getTriggerNameForField(table, oldField);
		if (trigger != null) {
			current.setLength(0);
			current.append("DROP TRIGGER ").append(trigger);
			
			back.add(current.toString());
		}
		
		String function = getFunctionNameForField(table, oldField);
		if (function != null) {
			current.setLength(0);
			current.append("DROP FUNCTION ").append(function);
			
			back.add(current.toString());
		}
		
		current.setLength(0);
		current.append("ALTER TABLE ").append(table.getName()).append(" CHANGE COLUMN ").append(oldField.getName()).append(' ');
		current.append(renderField(field));
		back.add(current.toString());
		
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
	
	protected String[] renderAlterTableDropColumn(DDLTable table, DDLField field) {
		List<String> back = new ArrayList<String>();
		StringBuilder current = new StringBuilder();
		
		String trigger = getTriggerNameForField(table, field);
		if (trigger != null) {
			current.setLength(0);
			current.append("DROP TRIGGER ").append(trigger);
			
			back.add(current.toString());
		}
		
		String function = getFunctionNameForField(table, field);
		if (function != null) {
			current.setLength(0);
			current.append("DROP FUNCTION ").append(function);
			
			back.add(current.toString());
		}
		
		current.setLength(0);
		current.append("ALTER TABLE ").append(table.getName()).append(" DROP COLUMN ").append(field.getName());
		back.add(current.toString());
		
		return back.toArray(new String[back.size()]);
	}
	
	protected String renderAlterTableAddKey(DDLForeignKey key) {
		StringBuilder back = new StringBuilder();
		
		back.append("ALTER TABLE ").append(key.getDomesticTable()).append(" ADD ");
		back.append(renderForeignKey(key));
		
		return back.toString();
	}
	
	protected String renderAlterTableDropKey(DDLForeignKey key) {
		return "ALTER TABLE " + key.getDomesticTable() + " DROP FOREIGN KEY " + key.getFKName();
	}

	protected String renderAppend() {
		return null;
	}
	
	protected String renderField(DDLField field) {
		StringBuilder back = new StringBuilder();
		
		back.append(field.getName());
		back.append(" ");
		back.append(renderFieldType(field));
		
		if (considerPrecision(field) && field.getPrecision() > 0) {
			back.append('(');
			if (field.getScale() > 0) {
				back.append(field.getPrecision());
				back.append(',');
				back.append(field.getScale());
			} else {
				back.append(field.getPrecision());
			}
			back.append(')');
		}
		
		if (field.isAutoIncrement()) {
			back.append(' ');
			back.append(renderAutoIncrement());
		} else if (field.getDefaultValue() != null) {
			back.append(" DEFAULT ");
			back.append(renderValue(field.getDefaultValue()));
		}

		if (field.isNotNull()) {
			back.append(" NOT NULL");
		}
		
		if (field.getOnUpdate() != null) {
			back.append(renderOnUpdate(field));
		}
		if (field.isUnique()) {
			back.append(" UNIQUE");
		}
		
		return back.toString();
	}
	
	protected String renderValue(Object value) {
		if (value == null) {
			return "NULL";
		} else if (value instanceof Calendar) {
			return "'" + renderCalendar((Calendar) value) + "'";
		} else if (value instanceof Boolean) {
			return ((Boolean) value ? "1" : "0");
		} else if (value instanceof DatabaseFunction) {
			return renderFunction((DatabaseFunction) value);
		}
		
		return value.toString();
	}
	
	protected String renderCalendar(Calendar calendar) {
		return new SimpleDateFormat(getDateFormat()).format(calendar.getTime());
	}
	
	protected String getDateFormat() {
		return "yyyy-MM-dd HH:mm:ss";
	}
	
	protected String renderFieldType(DDLField field) {
		return convertTypeToString(field.getType());
	}

	protected String renderFunction(DatabaseFunction func) {
		switch (func) {
			case CURRENT_DATE:
				return "CURRENT_DATE";
				
			case CURRENT_TIMESTAMP:
				return "CURRENT_TIMESTAMP";
		}
		
		return null;
	}
	
	protected String renderOnUpdate(DDLField field) {
		StringBuilder back = new StringBuilder();
		
		back.append(" ON UPDATE ");
		back.append(renderValue(field.getOnUpdate()));
		
		return back.toString();
	}
	
	protected boolean considerPrecision(DDLField field) {
		return true;
	}

	protected String getTriggerNameForField(DDLTable table, DDLField field) {
		return null;
	}
	
	protected String renderTriggerForField(DDLTable table, DDLField field) {
		return null;
	}
	
	protected String getFunctionNameForField(DDLTable table, DDLField field) {
		return null;
	}
	
	protected String renderFunctionForField(DDLTable table, DDLField field) {
		return null;
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
			sql.append("DEFAULT");
		}
		
		sql.append(")");
		
		return executeInsertReturningKeys(conn, sql.toString(), params);
	}
	
	protected int executeInsertReturningKeys(Connection conn, String sql, DBParam... params) throws SQLException {
		int back = -1;
		Logger.getLogger("net.java.ao").log(Level.INFO, sql);
		PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		
		for (int i = 0; i < params.length; i++) {
			Object value = params[i].getValue();
			
			if (value instanceof Entity) {
				value = ((Entity) value).getID();
			}
			
			stmt.setObject(i + 1, value);
		}
		
		stmt.executeUpdate();
		
		ResultSet res = stmt.getGeneratedKeys();
		if (res.next()) {
			 back = res.getInt(1);
		}
		res.close();
		stmt.close();
		
		return back;
	}
	
	protected boolean isNumericType(int type) {
		switch (type) {
			case Types.BIGINT: return true;
			case Types.BIT: return true;
			case Types.DECIMAL: return true;
			case Types.DOUBLE: return true;
			case Types.FLOAT: return true;
			case Types.INTEGER: return true;
			case Types.NUMERIC: return true;
			case Types.REAL: return true;
			case Types.SMALLINT: return true;
			case Types.TINYINT: return true;
		}
		
		return false;
	}
	
	public final static DatabaseProvider getInstance(String uri, String username, String password) {
		return getInstance(uri, username, password, true);		// enable pooling by default (if available)
	}
	
	public final static DatabaseProvider getInstance(String uri, String username, String password, boolean enablePooling) {
		SupportedDBProvider provider = SupportedDBProvider.getProviderForURI(uri);
		if (provider == null) {
			throw new RuntimeException("Unable to locate a valid database provider for URI: " + uri);
		}
		
		DatabaseProvider back = provider.createInstance(uri, username, password);
		if (back == null) {
			throw new RuntimeException("Unable to instantiate database provider for URI: " + uri);
		}
		
		if (enablePooling) {
			for (SupportedPoolProvider supportedProvider : SupportedPoolProvider.values()) {
				Class<? extends PoolProvider> providerClass = supportedProvider.getProvider();
				
				try {
					if ((Boolean) providerClass.getMethod("isAvailable").invoke(null)) {
						back = providerClass.getConstructor(DatabaseProvider.class).newInstance(back);
						break;
					}
				} catch (IllegalArgumentException e) {
					continue;
				} catch (SecurityException e) {
					continue;
				} catch (IllegalAccessException e) {
					continue;
				} catch (InvocationTargetException e) {
					continue;
				} catch (NoSuchMethodException e) {
					continue;
				} catch (InstantiationException e) {
					continue;
				}
			}
		}
		
		return back;
	}
}
