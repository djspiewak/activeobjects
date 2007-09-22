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
import net.java.ao.schema.ddl.DDLIndex;
import net.java.ao.schema.ddl.DDLTable;
import net.java.ao.types.DatabaseType;
import net.java.ao.types.TypeManager;

/**
 * <p>The superclass parent of all <code>DatabaseProvider</code>
 * implementations.  Various implementations allow for an abstraction
 * around database-specific functionality (such as DDL).  DatabaseProvider(s)
 * also handle the creation of new {@link Connection} instances and
 * fully encapsulate the raw JDBC driver.  <i>Any</i> database-specific
 * code should be placed in the database provider, rather than embedded
 * within the API logic.</p>
 * 
 * <p>This superclass contains a base-line, default implementation of most 
 * database-specific methods, thus requiring a minimum of work to implement
 * a new database provider.  For the sake of sanity (read: mine), this
 * base-line implementation is basically specific to MySQL.  Thus any
 * DatabaseProvider implementations are really specifying the 
 * <i>differences</i> between the database in question and MySQL.  To fully
 * utilize the default implementations provided in this class, this fact should
 * be kept in mind.</p>
 * 
 * <p>This class also handles the implementation details required to ensure
 * that only one active {@link Connection} instance is available per thread.  This is
 * in fact a very basic (and naive) form of connection pooling.  It should
 * <i>not</i> be relied upon for performance reasons.  Instead, a third-party
 * connection pool should be available in the classpath, enabling the use of
 * one of the {@link PoolProvider} implementations.  The purpose of the
 * thread-locked connection pooling in this class is to satisfy transactions
 * with external SQL statements.</p>
 * 
 * @author Daniel Spiewak
 */
public abstract class DatabaseProvider {
	private String uri, username, password;
	
	private Map<Thread, Connection> connections;
	private final ReadWriteLock connectionsLock = new ReentrantReadWriteLock();
	
	/**
	 * <p>The base constructor for <code>DatabaseProvider</code>.
	 * Initializes the JDBC uri, username and password values as specified.</p>
	 * 
	 * <p>Subclasses should implement a public constructor of this form, however
	 * it is not mandatory.</p>
	 * 
	 * @param uri	The JDBC URI which corresponds to the database being abstracted.
	 * @param username	The database username (note: for implementations which
	 * 	do not make use of this field, <code>null</code> is permitted).
	 * @param password		The database password (note: for implementations which
	 * 	do not make use of this field, <code>null</code> is permitted).
	 */
	protected DatabaseProvider(String uri, String username, String password) {
		this.uri = uri;
		
		this.username = username;
		this.password = password;
		
		connections = new HashMap<Thread, Connection>();
	}
	
	/**
	 * <p>Returns the JDBC Driver class which corresponds to the database being
	 * abstracted.  This should be implemented in such a way as to initialize
	 * and register the driver with JDBC.  For most drivers, this requires code in the
	 * following form:</p>
	 * 
	 * <pre>public Class&lt;? extends Driver&gt; getDriverClass() {
	 *     return (Class&lt;? extends Driver&gt;) Class.forName("com.mysql.jdbc.Driver");
	 * }</pre>
	 * 
	 * <p>The following does <i>not</i> fire the driver's static initializer and thus
	 * will (usually) not work:</p>
	 * 
	 * <pre>public Class&lt;? extends Driver&gt; getDriverClass() {
	 *     return com.mysql.jdbc.Driver.class;
	 * }</pre>
	 * 
	 * <p>If the driver is not on the classpath, a {@link ClassNotFoundException}
	 * can and should be thrown (certain auto-magic configuration sections of
	 * ActiveObjects depend upon this under certain circumstances).</p>
	 * 
	 * @returns The JDBC {@link Driver} implementation which corresponds to the
	 * 	relevant database.
	 */
	public abstract Class<? extends Driver> getDriverClass() throws ClassNotFoundException;
	
	/**
	 * <p>Generates the DDL fragment required to specify an INTEGER field as 
	 * auto-incremented.  For databases which do not support such flags (which
	 * is just about every database exception MySQL), <code>""</code> is an
	 * acceptable return value.  This method should <i>never</i> return <code>null</code>
	 * as it would cause the field rendering method to throw a {@link NullPointerException}.</p>
	 * 
	 * <p>This method is abstract (as opposed to the other methods which are
	 * either defined against MySQL or simply empty) because of the vast 
	 * differences in rendering auto-incremented fields across different
	 * databases.  Also, it seemed like a terribly good idea at the time and I haven't
	 * found a compelling reason to change it.</p>
	 */
	protected abstract String renderAutoIncrement();
	
	/**
	 * Top level delegating method for the process of rendering a database-agnostic
	 * {@link DDLAction} into the database-specific DDL statement(s).  It is
	 * doubtful that any implementations will have to override this method as the
	 * default implementation is database-agnostic.
	 * 
	 * @see #renderTable(DDLTable)
	 * @see #renderFunctions(DDLTable)
	 * @see #renderTriggers(DDLTable)
	 * @see #renderDropTriggers(DDLTable)
	 * @see #renderDropFunctions(DDLTable)
	 * @see #renderDropTable(DDLTable)
	 * @see #renderAlterTableAddColumn(DDLTable, DDLField)
	 * @see #renderAlterTableChangeColumn(DDLTable, DDLField, DDLField)
	 * @see #renderAlterTableDropColumn(DDLTable, DDLField)
	 * @see #renderAlterTableAddKey(DDLForeignKey)
	 * @see #renderAlterTableDropKey(DDLForeignKey)
	 * 
	 * @param action	The database-agnostic action to render.
	 * @returns An array of DDL statements specific to the database in question.
	 */
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
			
			case CREATE_INDEX:
				back.add(renderCreateIndex(action.getIndex()));
			break;
			
			case DROP_INDEX:
				back.add(renderDropIndex(action.getIndex()));
			break;
		}
		
		return back.toArray(new String[back.size()]);
	}
	
	/**
	 * <p>Top level delegating method for rendering a database-agnostic
	 * {@link Query} object into its (potentially) database-specific
	 * query statement.  This method invokes the various <code>renderQuery*</code>
	 * methods to construct its output, thus it is doubtful that any subclasses
	 * will have to override it.  Rather, one of the delegate methods
	 * should be considered.</p>
	 * 
	 * <p>An example of a database-specific query rendering would be the
	 * following <code>Query</code>:</p>
	 * 
	 * <pre>Query.select().from(Person.class).limit(10)</pre>
	 * 
	 * <p>On MySQL, this would render to <code>SELECT id FROM people LIMIT 10</code>
	 * However, on SQL Server, this same Query would render as
	 * <code>SELECT TOP 10 id FROM people</code></p>
	 * 
	 * @see #renderQuerySelect(Query, TableNameConverter, boolean)
	 * @see #renderQueryJoins(Query, TableNameConverter)
	 * @see #renderQueryWhere(Query)
	 * @see #renderQueryGroupBy(Query)
	 * @see #renderQueryOrderBy(Query)
	 * @see #renderQueryLimit(Query)
	 * 
	 * @param query	The database-agnostic Query object to be rendered in a
	 * 	potentially database-specific way.
	 * @param converter	Used to convert {@link Entity} classes into table names.
	 * @param count	If <code>true</code>, render the Query as a <code>SELECT COUNT(*)</code>
	 * 	rather than a standard field-data query.
	 * @returns A syntactically complete SQL statement potentially specific to the
	 * 	database.
	 */
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
	
	/**
	 * <p>Parses the database-agnostic <code>String</code> value relevant to the specified SQL
	 * type in <code>int</code> form (as defined by {@link Types} and returns
	 * the Java value which corresponds.  This method is completely database-agnostic, as are
	 * all of all of its delegate methods.</p>
	 * 
	 * <p><b>WARNING:</b> This method is being considered for removal to another
	 * class (perhaps {@link TypeManager}?) as it is not a database-specific function and thus
	 * confuses the purpose of this class.  Do not rely upon it heavily.  (better yet, don't rely on it
	 * at all from external code.  It's not designed to be part of the public API)</p>
	 * 
	 * @param type		The JDBC integer type of the database field against which to parse the
	 * 	value.
	 * @param value	The database-agnostic String value to parse into a proper Java object
	 * 	with respect to the specified SQL type.
	 * @returns	A Java value which corresponds to the specified String.
	 */
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
			for (Class<? extends RawEntity> join : query.getJoins().keySet()) {
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
			
			conn = DelegateConnectionHandler.newInstance(getConnectionImpl());
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
		
		if (primaryKeys.size() > 1) {
			throw new RuntimeException("Entities may only have one primary key");
		}
		
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
	
	protected String renderCreateIndex(DDLIndex index) {
		StringBuilder back = new StringBuilder();
		
		back.append("CREATE INDEX ").append(index.getName());
		back.append(" ON ").append(index.getTable()).append('(').append(index.getField()).append(')');
		
		return back.toString();
	}
	
	protected String renderDropIndex(DDLIndex index) {
		StringBuilder back = new StringBuilder();
		
		back.append("DROP INDEX ").append(index.getName());
		back.append(" ON ").append(index.getTable());
		
		return back.toString();
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
			String autoIncrementValue = renderAutoIncrement();
			if (!autoIncrementValue.trim().equals("")) {
				back.append(' ').append(autoIncrementValue);
			}
		} else if (field.getDefaultValue() != null) {
			back.append(" DEFAULT ");
			back.append(renderValue(field.getDefaultValue()));
		}
		
		if (field.isUnique()) {
			back.append(' ').append(renderUnique());
		}

		if (field.isNotNull() || field.isUnique()) {
			back.append(" NOT NULL");
		}
		
		if (field.getOnUpdate() != null) {
			back.append(renderOnUpdate(field));
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
		} else if (value instanceof Number) {
			return value.toString();
		}
		
		return "'" + value.toString() + "'";
	}
	
	protected String renderCalendar(Calendar calendar) {
		return new SimpleDateFormat(getDateFormat()).format(calendar.getTime());
	}
	
	protected String renderUnique() {
		return "UNIQUE";
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
	
	@SuppressWarnings("unused")
	public int insertReturningKeys(Connection conn, String pkField, String table, DBParam... params) throws SQLException {
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
			sql.append("DEFAULT");
		}
		
		sql.append(")");
		
		return executeInsertReturningKeys(conn, pkField, sql.toString(), params);
	}
	
	protected int executeInsertReturningKeys(Connection conn, String pkField, String sql, DBParam... params) throws SQLException {
		int back = -1;
		Logger.getLogger("net.java.ao").log(Level.INFO, sql);
		PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		
		for (int i = 0; i < params.length; i++) {
			Object value = params[i].getValue();
			
			if (value instanceof RawEntity) {
				value = Common.getPrimaryKeyValue((RawEntity) value);
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
