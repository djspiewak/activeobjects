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
import net.java.ao.schema.OnUpdate;
import net.java.ao.schema.TableNameConverter;
import net.java.ao.schema.ddl.DDLAction;
import net.java.ao.schema.ddl.DDLActionType;
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
	 * @return The JDBC {@link Driver} implementation which corresponds to the
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
	 * @see #renderSequences(DDLTable)
	 * @see #renderDropTriggers(DDLTable)
	 * @see #renderDropFunctions(DDLTable)
	 * @see #renderDropSequences(DDLTable)
	 * @see #renderDropTable(DDLTable)
	 * @see #renderAlterTableAddColumn(DDLTable, DDLField)
	 * @see #renderAlterTableChangeColumn(DDLTable, DDLField, DDLField)
	 * @see #renderAlterTableDropColumn(DDLTable, DDLField)
	 * @see #renderAlterTableAddKey(DDLForeignKey)
	 * @see #renderAlterTableDropKey(DDLForeignKey)
	 * 
	 * @param action	The database-agnostic action to render.
	 * @return An array of DDL statements specific to the database in question.
	 */
	public String[] renderAction(DDLAction action) {
		List<String> back = new ArrayList<String>();
		
		switch (action.getActionType()) {
			case CREATE:
				back.add(renderTable(action.getTable()));
				back.addAll(Arrays.asList(renderFunctions(action.getTable())));
				back.addAll(Arrays.asList(renderSequences(action.getTable())));
				back.addAll(Arrays.asList(renderTriggers(action.getTable())));
				
				for (DDLIndex index : action.getTable().getIndexes()) {
					DDLAction newAction = new DDLAction(DDLActionType.CREATE_INDEX);
					newAction.setIndex(index);
					back.addAll(Arrays.asList(renderAction(newAction)));
				}
			break;
			
			case DROP:
				for (DDLIndex index : action.getTable().getIndexes()) {
					DDLAction newAction = new DDLAction(DDLActionType.DROP_INDEX);
					newAction.setIndex(index);
					back.addAll(Arrays.asList(renderAction(newAction)));
				}

				back.addAll(Arrays.asList(renderDropTriggers(action.getTable())));
                back.addAll(Arrays.asList(renderDropSequences(action.getTable())));
				back.addAll(Arrays.asList(renderDropFunctions(action.getTable())));
				back.add(renderDropTable(action.getTable()));
			break;
			
			case ALTER_ADD_COLUMN:
				back.addAll(Arrays.asList(renderAlterTableAddColumn(action.getTable(), action.getField())));
				
				for (DDLIndex index : action.getTable().getIndexes()) {
					if (index.getField().equals(action.getField().getName())) {
						DDLAction newAction = new DDLAction(DDLActionType.CREATE_INDEX);
						newAction.setIndex(index);
						back.addAll(Arrays.asList(renderAction(newAction)));
					}
				}
			break;
			
			case ALTER_CHANGE_COLUMN:
				back.addAll(Arrays.asList(renderAlterTableChangeColumn(action.getTable(), action.getOldField(), action.getField())));
			break;
			
			case ALTER_DROP_COLUMN:
				for (DDLIndex index : action.getTable().getIndexes()) {
					if (index.getField().equals(action.getField().getName())) {
						DDLAction newAction = new DDLAction(DDLActionType.DROP_INDEX);
						newAction.setIndex(index);
						back.addAll(Arrays.asList(renderAction(newAction)));
					}
				}
				
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
	 * @return A syntactically complete SQL statement potentially specific to the
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
	 * @return	A Java value which corresponds to the specified String.
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
				break;
				
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
	
	/**
	 * <p>Allows the provider to set database-specific options on a
	 * {@link Statement} instance prior to its usage in a SELECT
	 * query.  This is to allow things like emulation of the
	 * LIMIT feature on databases which don't support it within
	 * the SQL implementation.</p>
	 * 
	 * <p>This method is only called on SELECTs.</p>
	 * 
	 * @param stmt	The instance against which the properties 
	 * 		should be set.
	 * @param query	The query which is being executed against
	 * 		the statement instance. 
	 */
	public void setQueryStatementProperties(Statement stmt, Query query) throws SQLException {
	}
	
	/**
	 * Allows the provider to set database-specific options on a
	 * {@link ResultSet} instance prior to its use by the library.
	 * This allows for features such as row offsetting even on
	 * databases that don't support it (such as Oracle, Derby,
	 * etc).
	 * 
	 * @param res	The <code>ResultSet</code> to modify.
	 * @param query	The query instance which was run to produce
	 * 		the result set.
	 */
	public void setQueryResultSetProperties(ResultSet res, Query query) throws SQLException {
	}
	
	/**
	 * <p>Returns a result set of all of the tables (and associated
	 * meta) in the database.  The fields of the result set must
	 * correspond with those specified in the
	 * <code>DatabaseMetaData#getTables(String, String, String, String[])</code>
	 * method.  In fact, the default implementation meerly calls
	 * this method passing <code>(null, null, "", null)</code>.
	 * For databases (such as PostgreSQL) where this is unsuitable,
	 * different parameters can be specified to the <code>getTables</code>
	 * method in the override, or an entirely new implementation
	 * written, as long as the result set corresponds in fields to
	 * the JDBC spec.</p>
	 * 
	 * <p>Databases which do not support this function (such as Oracle)
	 * should <i>not</i> throw an exception.  Instead, they should
	 * print a warning to stderr and return <code>null</code>.
	 * ActiveObjects will interpret a <code>null</code> result set
	 * as signifying no tables in the database, usually leading to a
	 * complete recreation of the schema (raw migration).</p>
	 * 
	 * @param conn	The connection to use in retrieving the database tables.
	 * @return	A result set of tables (and meta) corresponding in fields
	 * 		to the JDBC specification.
	 * @see java.sql.DatabaseMetaData#getTables(String, String, String, String[])
	 */
	public ResultSet getTables(Connection conn) throws SQLException {
		return conn.getMetaData().getTables(null, null, "", null);
	}
	
	/**
	 * <p>Renders the SELECT portion of a given {@link Query} instance in the
	 * manner required by the database-specific SQL implementation.  Usually,
	 * this is as simple as <code>"SELECT id FROM table"</code> or <code>"SELECT DISTINCT
	 * * FROM table"</code>.  However, some databases require the limit and offset
	 * parameters to be specified as part of the SELECT clause.  For example,
	 * on HSQLDB, a Query for the "id" field limited to 10 rows would render
	 * SELECT like this: <code>SELECT TOP 10 id FROM table</code>.</p>
	 * 
	 * <p>There is usually no need to call this method directly.  Under normal
	 * operations it functions as a delegate for {@link #renderQuery(Query, TableNameConverter, boolean)}.</p>
	 * 
	 * @param query	The Query instance from which to determine the SELECT properties.
	 * @param converter	The name converter to allow conversion of the query entity
	 * 		interface into a proper table name.
	 * @param count	Whether or not the query should be rendered as a <code>SELECT COUNT(*)</code>.
	 * @return	The database-specific SQL rendering of the SELECT portion of the query.
	 */
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
	
	/**
	 * <p>Renders the JOIN portion of the query in the database-specific SQL
	 * dialect.  Very few databases deviate from the standard in this matter,
	 * thus the default implementation is usually sufficient.</p>
	 * 
	 * <p>An example return value: <code>" JOIN table1 ON table.id = table1.value"</code></p>
	 * 
	 * <p>There is usually no need to call this method directly.  Under normal
	 * operations it functions as a delegate for {@link #renderQuery(Query, TableNameConverter, boolean)}.</p>
	 * 
	 * @param query	The Query instance from which to determine the JOIN properties.
	 * @param converter	The name converter to allow conversion of the query entity
	 * 		interface into a proper table name.
	 * @return	The database-specific SQL rendering of the JOIN portion of the query.
	 */
	protected String renderQueryJoins(Query query, TableNameConverter converter) {
		StringBuilder sql = new StringBuilder();

		if (query.getJoins().size() > 0) {
			for (Class<? extends RawEntity<?>> join : query.getJoins().keySet()) {
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
	
	/**
	 * <p>Renders the WHERE portion of the query in the database-specific SQL
	 * dialect.  Very few databases deviate from the standard in this matter,
	 * thus the default implementation is usually sufficient.</p>
	 * 
	 * <p>An example return value: <code>" WHERE name = ? OR age &lt; 20"</code></p>
	 * 
	 * <p>There is usually no need to call this method directly.  Under normal
	 * operations it functions as a delegate for {@link #renderQuery(Query, TableNameConverter, boolean)}.</p>
	 * 
	 * @param query	The Query instance from which to determine the WHERE properties.
	 * @return	The database-specific SQL rendering of the WHERE portion of the query.
	 */
	protected String renderQueryWhere(Query query) {
		StringBuilder sql = new StringBuilder();
		
		String whereClause = query.getWhereClause();
		if (whereClause != null) {
			sql.append(" WHERE ");
			sql.append(whereClause);
		}
		
		return sql.toString();
	}
	
	/**
	 * <p>Renders the GROUP BY portion of the query in the database-specific SQL
	 * dialect.  Very few databases deviate from the standard in this matter,
	 * thus the default implementation is usually sufficient.</p>
	 * 
	 * <p>An example return value: <code>" GROUP BY name"</code></p>
	 * 
	 * <p>There is usually no need to call this method directly.  Under normal
	 * operations it functions as a delegate for {@link #renderQuery(Query, TableNameConverter, boolean)}.</p>
	 * 
	 * @param query	The Query instance from which to determine the GROUP BY  properties.
	 * @return	The database-specific SQL rendering of the GROUP BY portion of the query.
	 */
	protected String renderQueryGroupBy(Query query) {
		StringBuilder sql = new StringBuilder();
		
		String groupClause = query.getGroupClause();
		if (groupClause != null) {
			sql.append(" GROUP BY ");
			sql.append(groupClause);
		}
		
		return sql.toString();
	}
	
	/**
	 * <p>Renders the ORDER BY portion of the query in the database-specific SQL
	 * dialect.  Very few databases deviate from the standard in this matter,
	 * thus the default implementation is usually sufficient.</p>
	 * 
	 * <p>An example return value: <code>" ORDER BY name ASC"</code></p>
	 * 
	 * <p>There is usually no need to call this method directly.  Under normal
	 * operations it functions as a delegate for {@link #renderQuery(Query, TableNameConverter, boolean)}.</p>
	 * 
	 * @param query	The Query instance from which to determine the ORDER BY properties.
	 * @return	The database-specific SQL rendering of the ORDER BY portion of the query.
	 */
	protected String renderQueryOrderBy(Query query) {
		StringBuilder sql = new StringBuilder();
		
		String orderClause = query.getOrderClause();
		if (orderClause != null) {
			sql.append(" ORDER BY ");
			sql.append(orderClause);
		}
		
		return sql.toString();
	}
	
	/**
	 * <p>Renders the LIMIT portion of the query in the database-specific SQL
	 * dialect.  There is wide variety in database implementations of this
	 * particular SQL clause.  In fact, many database do not support it at all.
	 * If the database in question does not support LIMIT, this method should
	 * be overridden to return an empty String.  For such databases, LIMIT
	 * should be implemented by overriding {@link #setQueryResultSetProperties(ResultSet, Query)}
	 * and {@link #setQueryStatementProperties(Statement, Query)}.</p>
	 * 
	 * <p>An example return value: <code>" LIMIT 10,2"</code></p>
	 * 
	 * <p>There is usually no need to call this method directly.  Under normal
	 * operations it functions as a delegate for {@link #renderQuery(Query, TableNameConverter, boolean)}.</p>
	 * 
	 * @param query	The Query instance from which to determine the LIMIT properties.
	 * @return	The database-specific SQL rendering of the LIMIT portion of the query.
	 */
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
	
	/**
	 * Retrieves the JDBC URI in use by the provider to obtain connections
	 * when necessary.  This should always return a valid URI, even in
	 * implementations such as connection pools which don't use the URI
	 * directly.
	 * 
	 * @return	A JDBC URI.
	 */
	public String getURI() {
		return uri;
	}
	
	/**
	 * Retrieves the username used to authenticate against the database.
	 * 
	 * @return	The database username.
	 */
	public String getUsername() {
		return username;
	}
	
	/**
	 * Retrieves the password used to authenticate against the database.
	 * 
	 * @return	The database password.
	 */
	public String getPassword() {
		return password;
	}
	
	/**
	 * <p>Retrieves a JDBC {@link Connection} instance which corresponds
	 * to the database represented by the provider instance.  This Connection
	 * can be used to execute arbitrary JDBC operations against the database.
	 * Also, this is the method used by the whole of ActiveObjects itself to
	 * get database connections when required.</p>
	 * 
	 * <p>All {@link Connection} instances are pooled internally by thread.
	 * Thus, there is never more than one connection per thread.  This is
	 * necessary to allow arbitrary JDBC operations within a transaction
	 * without breaking transaction integrity.  Developers using this method
	 * should bear this fact in mind and consider the {@link Connection}
	 * instance immutable.  The only exception is if one is <i>absolutely</i>
	 * certain that the JDBC code in question is not being executed within
	 * a transaction.</p>
	 * 
	 * <p>Despite the fact that there is only a single connection per thread,
	 * the {@link Connection} instances returned from this method should
	 * still be treated as bona fide JDBC connections.  They can and
	 * <i>should</i> be closed when their usage is complete.  This is
	 * especially important when actual connection pooling is in use and
	 * non-disposal of connections can lead to a crash as the connection
	 * pool runs out of resources.  The developer need not concern themselves
	 * with the single-connection-per-thread issue when closing the connection
	 * as the call to <code>close()</code> will be intercepted and ignored
	 * if necessary.</p>
	 * 
	 * <p>Due to the fact that this method must implement some thread-specific
	 * operations, it is declared <code>final</code> and thus is not
	 * overridable in subclasses.  Database providers which need to override
	 * the connection fetching mechanism (such as pool providers) should
	 * instead override the {@link #getConnectionImpl()} method.</p>
	 * 
	 * @return	A new connection to the database or <code>null</code>
	 * 		if the driver could not be loaded.
	 */
	public final Connection getConnection() throws SQLException {
		connectionsLock.writeLock().lock();
		try {
			Connection conn = connections.get(Thread.currentThread());
			if (conn != null && !conn.isClosed()) {
				return conn;
			}
			
			Connection connectionImpl = getConnectionImpl();
			if (connectionImpl == null) {
				throw new SQLException("Unable to create connection");
			}
			
			conn = DelegateConnectionHandler.newInstance(connectionImpl);
			setPostConnectionProperties(conn);
			connections.put(Thread.currentThread(), conn);
			
			return conn;
		} finally {
			connectionsLock.writeLock().unlock();
		}
	}
	
	/**
	 * <p>Creates a new connection to the database prepresented by the
	 * provider instance.  This method should not attempt to do any
	 * caching of any kind (unless implemented by a connection pool
	 * library).  Prior to creating the database connection, this
	 * method makes a call to {@link #getDriverClass()} to ensure that
	 * the JDBC driver has been loaded.  The return value is not
	 * checked for validity.</p>
	 * 
	 * <p>This method is <i>never</i> called directly.  Instead, the
	 * {@link #getConnection()} method should be used.</p>
	 * 
	 * @return	A new connection to the database or <code>null</code>
	 * 		if the driver could not be loaded.
	 */
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
	
	/**
	 * Frees any resources held by the database provider or delegate
	 * libraries (such as connection pools).  This method should be
	 * once usage of the provider is complete to ensure that all
	 * connections are committed and closed.
	 */
	public void dispose() {
		connectionsLock.writeLock().lock();
		try {
			for (Connection conn : connections.values()) {
				if (conn instanceof DelegateConnection) {
					((DelegateConnection) conn).setCloseable(true);
				}
				
				conn.close();
			}
		} catch (SQLException e) {
		} finally {
			connectionsLock.writeLock().unlock();
		}
	}
	
	/**
	 * Called to make any post-creation modifications to a new
	 * {@link Connection} instance.  This is used for databases
	 * such as Derby which require the schema to be set after
	 * the connection is created.
	 * 
	 * @param conn	The connection to modify according to the database
	 * 		requirements.
	 */
	protected void setPostConnectionProperties(Connection conn) throws SQLException {
	}
	
	/**
	 * Renders the foreign key constraints in database-specific DDL for
	 * the table in question.  Actually, this method only loops through
	 * the foreign keys and renders indentation and line-breaks.  The
	 * actual rendering is done in a second delegate method.
	 * 
	 * @param table	The database-agnostic DDL representation of the table
	 * 		in question.
	 * @return	The String rendering of <i>all</i> of the foreign keys for
	 * 		the table.
	 * @see #renderForeignKey(DDLForeignKey)
	 */
	protected String renderConstraintsForTable(DDLTable table) {
		StringBuilder back = new StringBuilder();
		
		for (DDLForeignKey key : table.getForeignKeys()) {
			back.append("    ").append(renderForeignKey(key)).append(",\n");
		}
		
		return back.toString();
	}
	
	/**
	 * Renders the specified foreign key representation into the
	 * database-specific DDL.  The implementation <i>must</i> name the
	 * foreign key according to the <code>DDLForeignKey#getFKName()</code>
	 * value otherwise migrations will no longer function appropriately.
	 * 
	 * @param key	The database-agnostic foreign key representation.
	 * @return	The database-pecific DDL fragment corresponding to the
	 * 		foreign key in question.
	 */
	protected String renderForeignKey(DDLForeignKey key) {
		StringBuilder back = new StringBuilder();
		
		back.append("CONSTRAINT ").append(key.getFKName());
		back.append(" FOREIGN KEY (").append(key.getField()).append(") REFERENCES ");
		back.append(key.getTable()).append('(').append(key.getForeignField()).append(")");
		
		return back.toString();
	}
	
	/**
	 * Converts the specified type into the database-specific DDL String
	 * value.  By default, this delegates to the <code>DatabaseType#getDefaultName()</code>
	 * method.  Subclass implementations should be sure to make a <code>super</code>
	 * call in order to ensure that both default naming and future special
	 * cases are handled appropriately.
	 * 
	 * @param type	The type instance to convert to a DDL string.
	 * @return	The database-specific DDL representation of the type (e.g. "VARCHAR").
	 * @see net.java.ao.types.DatabaseType#getDefaultName()
	 */
	protected String convertTypeToString(DatabaseType<?> type) {
		return type.getDefaultName();
	}
	
	/**
	 * Renders the specified table representation into the corresponding
	 * database-specific DDL statement.  For legacy reasons, this only allows
	 * single-statement table creation.  Additional statements (triggers,
	 * functions, etc) must be created in one of the other delegate methods
	 * for DDL creation.  This method does a great deal of delegation to
	 * other <code>DatabaseProvider</code> methods for functions such as
	 * field rendering, foreign key rendering, etc.
	 * 
	 * @param table	The database-agnostic table representation.
	 * @return	The database-specific DDL statements which correspond to the
	 * 		specified table creation.
	 */
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
	
	/**
	 * Generates the appropriate database-specific DDL statement to
	 * drop the specified table representation.  The default implementation
	 * is merely <code>"DROP TABLE tablename"</code>.  This is suitable
	 * for every database that I am aware of.  Any dependant database
	 * objects (such as triggers, functions, etc) must be rendered in
	 * one of the other delegate methods (such as <code>renderDropTriggers(DDLTable)</code>).
	 * 
	 * @param table	The table representation which is to be dropped.
	 * @return	A database-specific DDL statement which drops the specified 
	 * 		table.
	 */
	protected String renderDropTable(DDLTable table) {
		return "DROP TABLE " + table.getName();
	}
	
	/**
	 * Generates the database-specific DDL statements required to drop all
	 * associated functions for the given table representation.  The default
	 * implementation is to return an empty array.  Some databases (such
	 * as PostgreSQL) require triggers to fire functions, unlike most
	 * databases which allow triggers to function almost like standalone
	 * functions themselves.  For such databases, dropping a table means
	 * not only dropping the table and the associated triggers, but also
	 * the functions associated with the triggers themselves.
	 * 
	 * @param table	The table representation against which all functions which
	 * 		correspond (directly or indirectly) must be dropped.
	 * @return	An array of database-specific DDL statement(s) which drop the
	 * 		required functions.
	 */
	protected String[] renderDropFunctions(DDLTable table) {
		return new String[0];
	}
	
	/**
	 * Generates the database-specific DDL statements required to drop all
	 * associated triggers for the given table representation.  The default
	 * implementation is to return an empty array.  Most databases require
	 * the <code>@OnUpdate</code> function to be implemented using triggers
	 * explicitly (rather than the implicit MySQL syntax).  For such
	 * databases, some tables will thus have triggers which are associated 
	 * directly with the table.  It is these triggers which must be
	 * dropped prior to the dropping of the table itself.  For databases
	 * which associate functions with triggers (such as PostgreSQL), these
	 * functions will be dropped using another delegate method and need
	 * not be dealt with in this method's implementation.
	 * 
	 * @param table	The table representation against which all triggers which
	 * 		correspond (directly or indirectly) must be dropped.
	 * @return	An array of database-specific DDL statement(s) which drop the
	 * 		required triggers.
	 */
	protected String[] renderDropTriggers(DDLTable table) {
		return new String[0];
	}
    /**
	 * Generates the database-specific DDL statements required to drop all
     * associated sequences for the given table representation.  The default
     * implementation is to return an empty array.  This is an Oracle specific
     * method used for primary key management
     * 
     * @param table The table representation against which all triggers which
     *      correspond (directly or indirectly) must be dropped.
     * @return  An array of database-specific DDL statement(s) which drop the
     *      required triggers.
     */
    protected String[] renderDropSequences(DDLTable table) {
        return new String[0];
    }
	
	/**
	 * <p>Generates the database-specific DDL statements required to create
	 * all of the functions necessary for the given table.  For most
	 * databases, this will simply return an empty array.  The 
	 * functionality is required for databases such as PostgreSQL which
	 * require a function to be explicitly declared and associated when
	 * a trigger is created.</p>
	 * 
	 * <p>Most of the work for this functionality is delegated to the
	 * {@link #renderFunctionForField(DDLTable, DDLField)} method.</p>
	 * 
	 * @param table	The table for which the functions must be generated.
	 * @return	An array of DDL statements to execute.
	 */
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
	
	/**
	 * <p>Generates the database-specific DDL statements required to create
	 * all of the triggers necessary for the given table.  For MySQL, this
	 * will likely return an empty array.  The functionality is required
	 * for databases which do not provide an implicit syntax for the
	 * <code>@OnUpdate</code> functionality.  In MySQL, it is possible to
	 * provide this functionality with the 
	 * <code>field TIMESTAMP ON UPDATE CURRENT_DATE</code> style syntax.
	 * This syntax is not common to all databases, hence triggers must be
	 * used to provide the functionality.</p>
	 * 
	 * <p>Most of the work for this functionality is delegated to the
	 * {@link #renderTriggerForField(DDLTable, DDLField)} method.</p>
	 * 
	 * @param table	The table for which the triggers must be generated.
	 * @return	An array of DDL statements to execute.
	 */
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
	
    /**
     * <p>Generates the database-specific DDL statements required to create
     * all of the sequences necessary for the given table. This is an Oracle specific
     * method used for primary key management
     * 
     * 
     * @param table The table for which the triggers must be generated.
     * @return  An array of DDL statements to execute.
     */
    protected String[] renderSequences(DDLTable table) {
        
        return new String[0];
    }	
	
	/**
	 * Generates the database-specific DDL statements required to add
	 * a column to an existing table.  Included in the return value
	 * should be the statements required to add all necessary functions
	 * and triggers to ensure that the column acts appropriately.  For
	 * example, if the field is tagged with an <code>@OnUpdate</code>
	 * annotation, chances are there will be a trigger and possibly a
	 * function along with the ALTER statement.  These "extra"
	 * functions are properly ordered and will only be appended if
	 * their values are not <code>null</code>.  Because of this, very
	 * few database providers will need to override this method.
	 * 
	 * @param table	The table which should receive the new column.
	 * @param field	The column to add to the specified table.
	 * @return	An array of DDL statements to execute.
	 * @see #renderFunctionForField(DDLTable, DDLField)
	 * @see #renderTriggerForField(DDLTable, DDLField)
	 */
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
	
	/**
	 * <p>Generates the database-specific DDL statements required to change
	 * the given column from its old specification to the given DDL value.
	 * This method will also generate the appropriate statements to remove
	 * old triggers and functions, as well as add new ones according to the 
	 * requirements of the new field definition.</p>
	 * 
	 * <p>The default implementation of this method functions in the manner
	 * specified by the MySQL database.  Some databases will have to perform
	 * more complicated actions (such as dropping and re-adding the field)
	 * in order to satesfy the same use-case.  Such databases should print
	 * a warning to stderr to ensure that the end-developer is aware of
	 * such restrictions.</p>
	 * 
	 * <p>Thus, the specification for this method <i>allows</i> for data
	 * loss.  Nevertheless, if the database supplies a mechanism to
	 * accomplish the task without data loss, it should be applied.</p>
	 * 
	 * <p>For maximum flexibility, the default implementation of this method
	 * only deals with the dropping and addition of functions and triggers.
	 * The actual generation of the ALTER TABLE statement is done in the
	 * {@link #renderAlterTableChangeColumnStatement(DDLTable, DDLField, DDLField)}
	 * method.</p>
	 * 
	 * @param table	The table containing the column to change.
	 * @param oldField	The old column definition.
	 * @param field	The new column definition (defining the resultant DDL).
	 * @return	An array of DDL statements to be executed.
	 * @see #getTriggerNameForField(DDLTable, DDLField)
	 * @see #getFunctionNameForField(DDLTable, DDLField)
	 * @see #renderFunctionForField(DDLTable, DDLField)
	 * @see #renderTriggerForField(DDLTable, DDLField)
	 */
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
		
		back.add(renderAlterTableChangeColumnStatement(table, oldField, field));
		
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
	
	/**
	 * Generates the database-specific DDL statement only for altering a table and
	 * changing a column.  This method must only generate a single statement as it
	 * does not need to concern itself with functions or triggers associated with
	 * the column.  This method is only to be called as a delegate for the
	 * {@link #renderAlterTableChangeColumn(DDLTable, DDLField, DDLField)} method,
	 * for which it is a primary delegate.  The default implementation of this
	 * method functions according to the MySQL specification.
	 * 
	 * @param table	The table containing the column to change.
	 * @param oldField	The old column definition.
	 * @param field	The new column definition (defining the resultant DDL).
	 * @return	A single DDL statement which is to be executed.
	 * @see #renderField(DDLField)
	 */
	protected String renderAlterTableChangeColumnStatement(DDLTable table, DDLField oldField, DDLField field) {
		StringBuilder current = new StringBuilder();
		current.append("ALTER TABLE ").append(table.getName()).append(" CHANGE COLUMN ");
		current.append(oldField.getName()).append(' ');
		current.append(renderField(field));
		return current.toString();
	}

	/**
	 * Generates the database-specific DDL statements required to remove
	 * the specified column from the given table.  This should also
	 * generate the necessary statements to drop all triggers and functions
	 * associated with the column in question.  If the database being
	 * implemented has a non-standard syntax for dropping functions and/or
	 * triggers, it may be required to override this method, even if the
	 * syntax to drop columns is standard.
	 * 
	 * @param table	The table from which to drop the column.
	 * @param field	The column definition to remove from the table.
	 * @return	An array of DDL statements to be executed.
	 * @see #getTriggerNameForField(DDLTable, DDLField)
	 * @see #getFunctionNameForField(DDLTable, DDLField)
	 */
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
	
	/**
	 * Generates the database-specific DDL statement required to add a
	 * foreign key to a table.  For databases which do not support such
	 * a statement, a warning should be printed to stderr and a
	 * <code>null</code> value returned.
	 * 
	 * @param key	The foreign key to be added.  As this instance contains
	 * 		all necessary data (such as domestic table, field, etc), no
	 * 		additional parameters are required.
	 * @return	A DDL statement to be executed, or <code>null</code>.
	 * @see #renderForeignKey(DDLForeignKey)
	 */
	protected String renderAlterTableAddKey(DDLForeignKey key) {
		StringBuilder back = new StringBuilder();
		
		back.append("ALTER TABLE ").append(key.getDomesticTable()).append(" ADD ");
		back.append(renderForeignKey(key));
		
		return back.toString();
	}
	
	/**
	 * Generates the database-specific DDL statement required to remove a
	 * foreign key from a table.  For databases which do not support such
	 * a statement, a warning should be printed to stderr and a
	 * <code>null</code> value returned.  This method assumes that the
	 * {@link #renderForeignKey(DDLForeignKey)} method properly names
	 * the foreign key according to the {@link DDLForeignKey#getFKName()}
	 * method.
	 * 
	 * @param key	The foreign key to be removed.  As this instance contains
	 * 		all necessary data (such as domestic table, field, etc), no
	 * 		additional parameters are required.
	 * @return	A DDL statement to be executed, or <code>null</code>.
	 */
	protected String renderAlterTableDropKey(DDLForeignKey key) {
		return "ALTER TABLE " + key.getDomesticTable() + " DROP FOREIGN KEY " + key.getFKName();
	}
	
	/**
	 * Generates the database-specific DDL statement required to create
	 * a new index.  The syntax for this operation is highly standardized
	 * and thus it is unlikely this method will be overridden.  If the
	 * database in question does not support indexes, a warning should
	 * be printed to stderr and <code>null</code> returned.
	 * 
	 * @param index	The index to create.  This single instance contains all
	 * 		of the data necessary to create the index, thus no separate
	 * 		parameters (such as a <code>DDLTable</code>) are required.
	 * @return	A DDL statement to be executed, or <code>null</code>.
	 */
	protected String renderCreateIndex(DDLIndex index) {
		StringBuilder back = new StringBuilder();
		
		back.append("CREATE INDEX ").append(index.getName());
		back.append(" ON ").append(index.getTable()).append('(').append(index.getField()).append(')');
		
		return back.toString();
	}
	
	/**
	 * Generates the database-specific DDL statement required to drop
	 * an index.  The syntax for this operation is highly standardized
	 * and thus it is unlikely this method will be overridden.  If the
	 * database in question does not support indexes, a warning should
	 * be printed to stderr and <code>null</code> returned.
	 * 
	 * @param index	The index to drop.  This single instance contains all
	 * 		of the data necessary to drop the index, thus no separate
	 * 		parameters (such as a <code>DDLTable</code>) are required.
	 * @return	A DDL statement to be executed, or <code>null</code>.
	 */
	protected String renderDropIndex(DDLIndex index) {
		StringBuilder back = new StringBuilder();
		
		back.append("DROP INDEX ").append(index.getName());
		back.append(" ON ").append(index.getTable());
		
		return back.toString();
	}

	/**
	 * <p>Generates any database-specific options which must be appended
	 * to the end of a table definition.  The only database I am aware
	 * of which requires this is MySQL.  For example:</p>
	 * 
	 * <pre>CREATE TABLE test (
	 *     id INTEGER NOT NULL AUTO_INCREMENT,
	 *     name VARCHAR(45),
	 *     PRIMARY KEY(id)
	 * ) ENGINE=InnoDB;</pre>
	 * 
	 * <p>The "<code>ENGINE=InnoDB</code>" clause is what is returned by
	 * this method.  The default implementation simply returns 
	 * <code>null</code>, signifying that no append should be rendered.</p>
	 * 
	 * @return	A DDL clause to be appended to the CREATE TABLE DDL, or <code>null</code>
	 */
	protected String renderAppend() {
		return null;
	}
	
	/**
	 * <p>Generates the database-specific DDL fragment required to render the
	 * field and its associated type.  This includes all field attributes,
	 * such as <code>@NotNull</code>, <code>@AutoIncrement</code> (if
	 * supported by the database at the field level) and so on.  Sample
	 * return value:</p>
	 * 
	 * <pre>name VARCHAR(255) DEFAULT "Skye" NOT NULL</pre>
	 * 
	 * <p>Certain databases don't allow defined precision for certain types
	 * (such as Derby and INTEGER).  The logic for whether or not to render
	 * precision should not be within this method, but delegated to the
	 * {@link #considerPrecision(DDLField)} method.</p>
	 * 
	 * <p>Almost all functionality within this method is delegated to other
	 * methods within the implementation.  As such, it is almost never
	 * necessary to override this method directly.  An exception to this
	 * would be a database like PostgreSQL which requires a different type
	 * for auto-incremented fields.</p>
	 * 
	 * @param field	The field to be rendered.
	 * @return	A DDL fragment to be embedded in a statement elsewhere.
	 */
	protected String renderField(DDLField field) {
		StringBuilder back = new StringBuilder();
		
		back.append(field.getName());
		back.append(" ");
		back.append(renderFieldType(field));
		back.append(renderFieldPrecision(field));
		
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
			String renderUnique = renderUnique();
			
			if (!renderUnique.trim().equals("")) {
				back.append(' ').append(renderUnique);
			}
		}

		if (field.isNotNull() || field.isUnique()) {
			back.append(" NOT NULL");
		}
		
		if (field.getOnUpdate() != null) {
			back.append(renderOnUpdate(field));
		}
		
		return back.toString();
	}

	/**
	 * <p>Renders the statement fragment for the given field representative of
	 * its precision only.  Consider the following statement:</p>
	 * 
	 * <code>ALTER TABLE ADD COLUMN name VARCHAR(255)</code>
	 * 
	 * <p>In this statement, the bit which is rendered by this method is the
	 * "<code>(255)</code>" (without quotes).  This is intended to allow
	 * maximum flexibility in field type rendering (as required by PostgreSQL
	 * and others which sometimes render types separately from the rest of
	 * the field info).  The default implementation should suffice for every
	 * conceivable database.  Any sort of odd functionality relating to
	 * type precision rendering should be handled in the {@link #considerPrecision(DDLField)}
	 * method if possible.</p>
	 * 
	 * @param field	The field for which the precision must be rendered.
	 * @return	A DDL fragment which will be concatenated into a statement later.
	 */
	protected String renderFieldPrecision(DDLField field) {
		StringBuilder back = new StringBuilder();
		
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
		
		return back.toString();
	}
	
	/**
	 * Renders the given Java instance in a database-specific way.  This
	 * method handles special cases such as {@link Calendar},
	 * {@link Boolean} (which is always rendered as 0/1), functions,
	 * <code>null</code> and numbers.  All other values are rendered (by 
	 * default) as <code>'value.toString()'</code> (the String value 
	 * enclosed within single quotes).  Implementations are encouraged to 
	 * override this method as necessary.
	 * 
	 * @param value	The Java instance to be rendered as a database literal.
	 * @return	The database-specific String rendering of the instance in 
	 * 		question.
	 * @see #renderCalendar(Calendar)
	 * @see #renderFunction(DatabaseFunction)
	 */
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
	
	/**
	 * Renders the provided {@link Calendar} instance as a TIMESTAMP literal
	 * in the database-specific format.  The return value should <i>not</i>
	 * be enclosed within quotes, as this is accomplished within other
	 * functions when rendering is required.  This method is actually a
	 * boiler-plate usage of the {@link SimpleDateFormat} class, using the
	 * date format defined within the {@link #getDateFormat()} method.
	 * 
	 * @param calendar	The time instance to be rendered.
	 * @return	The database-specific String representation of the time.
	 */
	protected String renderCalendar(Calendar calendar) {
		return new SimpleDateFormat(getDateFormat()).format(calendar.getTime());
	}
	
	/**
	 * Renders the <code>UNIQUE</code> constraint as defined by the
	 * database-specific DDL syntax.  This method is a delegate of other, more
	 * complex methods such as {@link #renderField(DDLField)}.  The default
	 * implementation just returns <code>UNIQUE</code>.  Implementations may
	 * override this method to return an empty {@link String} if the database
	 * in question does not support the constraint.
	 * 
	 * @return	The database-specific rendering of <code>UNIQUE</code>.
	 */
	protected String renderUnique() {
		return "UNIQUE";
	}
	
	/**
	 * Returns the database-specific TIMESTAMP text format as defined by
	 * the {@link SimpleDateFormat} syntax.  This format should include
	 * the time down to the second (or even more precise, if allowed by
	 * the database).  The default implementation returns the format for
	 * MySQL, which is: <code>yyyy-MM-dd HH:mm:ss</code>
	 * 
	 * @return The database-specific TIMESTAMP text format
	 */
	protected String getDateFormat() {
		return "yyyy-MM-dd HH:mm:ss";
	}
	
	/**
	 * Renders the database-specific DDL type for the field in question.
	 * This method merely delegates to the {@link #convertTypeToString(DatabaseType)}
	 * method, passing the field type.  Thus, it is rarely necessary
	 * (if ever) to override this method.  It may be deprecated in a
	 * future release.
	 * 
	 * @param field	The field which contains the type to be rendered.
	 * @return	The database-specific type DDL rendering.
	 */
	protected String renderFieldType(DDLField field) {
		return convertTypeToString(field.getType());
	}

	/**
	 * <p>Renders the specified {@link DatabaseFunction} in its 
	 * database-specific form.  For example, for MySQL the
	 * <code>CURRENT_DATE</code> enum value would be rendered as
	 * "<code>CURRENT_DATE</code>" (without the quotes).  For functions
	 * which do not have a database equivalent, a default literal value
	 * of the appropriate type should be returned.  For example, if MySQL
	 * did <i>not</i> define either a CURRENT_DATE or a CURRENT_TIMESTAMP
	 * function, the appropriate return value for both functions would
	 * be <code>'0000-00-00 00:00:00'</code> (including the quotes).
	 * This is to prevent migrations from failing even in cases where
	 * non-standard functions are used.</p>
	 * 
	 * <p>As of 1.0, no unconventional functions are allowed by the 
	 * {@link DatabaseFunction} enum, thus no database should have any
	 * problems with any allowed functions.</p>
	 * 
	 * @param func	The abstract function to be rendered.
	 * @return	The database-specific DDL representation of the function
	 * 		in question.
	 */
	protected String renderFunction(DatabaseFunction func) {
		switch (func) {
			case CURRENT_DATE:
				return "CURRENT_DATE";
				
			case CURRENT_TIMESTAMP:
				return "CURRENT_TIMESTAMP";
		}
		
		return null;
	}
	
	/**
	 * <p>Renders the appropriate field suffix to allow for the
	 * {@link OnUpdate} functionality.  For most databases (read:
	 * all but MySQL) this will return an empty String.  This is
	 * because few databases provide an implicit ON UPDATE syntax for
	 * fields.  As such, most databases will be compelled to return
	 * an empty String and implement the functionality using triggers.
	 * 
	 * @param field	The field for which the ON UPDATE clause should 
	 * 		be rendered.
	 * @return	The database-specific ON UPDATE field clause.
	 */
	protected String renderOnUpdate(DDLField field) {
		StringBuilder back = new StringBuilder();
		
		back.append(" ON UPDATE ");
		back.append(renderValue(field.getOnUpdate()));
		
		return back.toString();
	}
	
	/**
	 * <p>Determines whether or not the database allows explicit precisions
	 * for the field in question.  This is to support databases such as
	 * Derby which do not support precisions for certain types.  By
	 * default, this method returns <code>true</code>.</p>
	 * 
	 * <p>More often than not, all that is required for this determination 
	 * is the type.  As such, the method signature may change in a future 
	 * release.</p>
	 * 
	 * @param field	The field for which precision should/shouldn't be rendered.
	 * @return	<code>true</code> if precision should be rendered, otherwise
	 * 		<code>false</code>.
	 */
	protected boolean considerPrecision(DDLField field) {
		return true;
	}

	/**
	 * Retrieves the name of the trigger which corresponds to the field
	 * in question (if any).  If no trigger will be automatically created
	 * for the specified field, <code>null</code> should be returned.
	 * This function is to allow for databases which require the use of
	 * triggers on a field to allow for certain functionality (like
	 * ON UPDATE).  The default implementation returns <code>null</code>.
	 * 
	 * @param table	The table which contains the field for which a trigger
	 * 		may or may not exist.
	 * @param field	The field for which a previous migration may have
	 * 		created a trigger.
	 * @return	The unique name of the trigger which was created for the 
	 * 		field, or <code>null</code> if none.
	 * @see #renderTriggerForField(DDLTable, DDLField)
	 */
	protected String getTriggerNameForField(DDLTable table, DDLField field) {
		return null;
	}
	
	/**
	 * Renders the trigger which corresponds to the specified field, or
	 * <code>null</code> if none.  This is to allow for databases which
	 * require the use of triggers to provide functionality such as ON
	 * UPDATE.  The default implementation returns <code>null</code>.
	 * 
	 * @param table	The table containing the field for which a trigger
	 * 		may need to be rendered.
	 * @param field	The field for which the trigger should be rendered,
	 * 		if any.
	 * @return	A database-specific DDL statement creating a trigger for
	 * 		the field in question, or <code>null</code>.
	 * @see #getTriggerNameForField(DDLTable, DDLField)
	 */
	protected String renderTriggerForField(DDLTable table, DDLField field) {
		return null;
	}

	/**
	 * Retrieves the name of the function which corresponds to the field
	 * in question (if any).  If no function will be automatically created
	 * for the specified field, <code>null</code> should be returned.
	 * This method is to allow for databases which require the use of
	 * explicitly created functions which correspond to triggers (e.g.
	 * PostgreSQL).  Few providers will need to override the default
	 * implementation of this method, which returns <code>null</code>.
	 * 
	 * @param table	The table which contains the field for which a function
	 * 		may or may not exist.
	 * @param field	The field for which a previous migration may have
	 * 		created a function.
	 * @return	The unique name of the function which was created for the 
	 * 		field, or <code>null</code> if none.
	 */
	protected String getFunctionNameForField(DDLTable table, DDLField field) {
		return null;
	}
	
	/**
	 * Renders the function which corresponds to the specified field, or
	 * <code>null</code> if none.  This is to allow for databases which
	 * require the use of triggers and explicitly created functions to 
	 * provide functionality such as ON UPDATE (e.g. PostgreSQL).  The 
	 * default implementation returns <code>null</code>.
	 * 
	 * @param table	The table containing the field for which a function
	 * 		may need to be rendered.
	 * @param field	The field for which the function should be rendered,
	 * 		if any.
	 * @return	A database-specific DDL statement creating a function for
	 * 		the field in question, or <code>null</code>.
	 * @see #getFunctionNameForField(DDLTable, DDLField)
	 */
	protected String renderFunctionForField(DDLTable table, DDLField field) {
		return null;
	}
	
	/**
	 * <p>Generates an INSERT statement to be used to create a new row in the
	 * database, returning the primary key value.  This method also invokes
	 * the delegate method, {@link #executeInsertReturningKey(Connection, Class, String, String, DBParam...)}
	 * passing the appropriate parameters and query.  This method is required
	 * because some databases do not support the JDBC parameter
	 * <code>RETURN_GENERATED_KEYS</code> (such as HSQLDB and PostgreSQL).
	 * Also, some databases (such as MS SQL Server) require odd tricks to
	 * support explicit value passing to auto-generated fields.  This method
	 * should take care of any extra queries or odd SQL generation required
	 * to implement both auto-generated primary key returning, as well as
	 * explicit primary key value definition.</p>
	 * 
	 * <p>Overriding implementations of this method should be sure to use the
	 * {@link Connection} instance passed to the method, <i>not</i> a new
	 * instance generated using the {@link #getConnection()} method.  This is
	 * because this method is in fact a delegate invoked by {@link EntityManager}
	 * as part of the entity creation process and may be part of a transaction,
	 * a bulk creation or some more complicated operation.  Both optimization
	 * and usage patterns on the API dictate that the specified connection
	 * instance be used.  Implementations may assume that the given connection
	 * instance is never <code>null</code>.</p>
	 * 
	 * <p>The default implementation of this method should be sufficient for any
	 * fully compliant ANSI SQL database with a properly implemented JDBC
	 * driver.  Note that this method should <i>not</i> not actually execute
	 * the SQL it generates, but pass it on to the {@link #executeInsertReturningKey(Connection, Class, String, String, DBParam...)}
	 * method, allowing for functional delegation and better extensibility. 
	 * However, this method may execute any additional statements required to
	 * prepare for the INSERTion (as in the case of MS SQL Server which requires
	 * some config parameters to be set on the database itself prior to INSERT).</p>
	 * 
	 * @param conn	The connection to be used in the eventual execution of the
	 * 		generated SQL statement.
	 * @param pkType	The Java type of the primary key value.  Can be used to
	 * 		perform a linear search for a specified primary key value in the
	 * 		<code>params</code> list.  The return value of the method must be of
	 * 		the same type.
	 * @param pkField	The database field which is the primary key for the
	 * 		table in question.  Can be used to perform a linear search for a 
	 * 		specified primary key value in the <code>params</code> list.
	 * @param pkIdentity	Flag indicating whether or not the primary key field
	 * 		is auto-incremented by the database (IDENTITY field).
	 * @param table	The name of the table into which the row is to be INSERTed.
	 * @param params	A varargs array of parameters to be passed to the
	 * 		INSERT statement.  This may include a specified value for the
	 * 		primary key.
	 * @throws	SQLException	If the INSERT fails in the delegate method, or
	 * 		if any additional statements fail with an exception.
	 * @see #executeInsertReturningKey(Connection, Class, String, String, DBParam...)
	 */
	@SuppressWarnings("unused")
	public <T> T insertReturningKey(Connection conn, Class<T> pkType, String pkField, 
			boolean pkIdentity, String table, DBParam... params) throws SQLException {
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
		
		return executeInsertReturningKey(conn, pkType, pkField, sql.toString(), params);
	}
	
	/**
	 * <p>Delegate method to execute an INSERT statement returning any auto-generated
	 * primary key values.  This method is primarily designed to be called as a delegate
	 * from the {@link #insertReturningKey(Connection, Class, String, boolean, String, DBParam...)}
	 * method.  The idea behind this method is to allow custom implementations to
	 * override this method to potentially execute other statements (such as getting the
	 * next value in a sequence) rather than the default implementaiton which uses the
	 * JDBC constant, <code>RETURN_GENERATED_KEYS</code>.  Any database which has a 
	 * fully-implemented JDBC driver should have no problems with the default 
	 * implementation of this method.</p>
	 * 
	 * <p>Part of the design behind splitting <code>insertReturningKey</code> and
	 * <code>executeInsertReturningKey</code> is so that logic for generating the actual
	 * INSERT statement need not be duplicated throughout the code and in custom
	 * implementations providing trivial changes to the default algorithm.  This method
	 * should avoid actually generating SQL if at all possible.</p>
	 * 
	 * <p>This method should iterate through the passed <code>DBParam(s)</code> to
	 * ensure that no primary key value was explicitly specified.  If one was, it
	 * should be used in leiu of one which is auto-generated by the database.  Also,
	 * it is this value which should be returned if specified, rather than the value
	 * which <i>would</i> have been generated or <code>null</code>.  As such, this method
	 * should always return exactly the value of the primary key field in the row which
	 * was just inserted, regardless of what that value may be.</p>
	 * 
	 * <p>In cases where the database mechanism for getting the next primary key value
	 * is not thread safe, this method should be declared <code>synchronized</code>,
	 * or some thread synchronization technique employed.  Unfortunately, it is not
	 * always possible to ensure that no other INSERT could (potentially) "steal" the
	 * expected value out from under the algorithm.  Such scenarios are to be avoided
	 * when possible, but the algorithm need not take extremely escoteric concurrency
	 * cases into account.  (see the HSQLDB provider for an example of such a
	 * less-than-thorough asynchronous algorithm)</p>
	 * 
	 * <p><b>IMPORTANT:</b> The INSERT {@link Statement} <i>must</i> use the specified
	 * connection, rather than a new one retrieved from {@link #getConnection()} or
	 * equivalent.  This is because the INSERT may be part of a bulk insertion, a
	 * transaction, or possibly another such operation.  It is also important to note
	 * that this method should not close the connection.  Doing so could cause the
	 * entity creation algorithm to fail at a higher level up the stack.</p>
	 * 
	 * @param conn	The database connection to use in executing the INSERT statement.
	 * @param pkType	The Java class type of the primary key field (for use both in
	 * 		searching the <code>params</code> as well as performing value conversion
	 * 		of auto-generated DB values into proper Java instances).
	 * @param pkField	The database field which is the primary key for the
	 * 		table in question.  Can be used to perform a linear search for a 
	 * 		specified primary key value in the <code>params</code> list.
	 * @param params	A varargs array of parameters to be passed to the
	 * 		INSERT statement.  This may include a specified value for the
	 * 		primary key.
	 * @throws	SQLException	If the INSERT fails in the delegate method, or
	 * 		if any additional statements fail with an exception.
	 * @see #insertReturningKey(Connection, Class, String, boolean, String, DBParam...)
	 */
	protected <T> T executeInsertReturningKey(Connection conn, Class<T> pkType, String pkField, 
			String sql, DBParam... params) 
				throws SQLException {
		T back = null;
		Logger.getLogger("net.java.ao").log(Level.INFO, sql);
		PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		
		for (int i = 0; i < params.length; i++) {
			Object value = params[i].getValue();
			
			if (value instanceof RawEntity) {
				value = Common.getPrimaryKeyValue((RawEntity<?>) value);
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
		
		if (back == null) {
			ResultSet res = stmt.getGeneratedKeys();
			if (res.next()) {
				back = TypeManager.getInstance().getType(pkType).pullFromDatabase(null, res, pkType, 1);
			}
			res.close();
		}
		
		stmt.close();
		
		return back;
	}

	/**
	 * TODO
	 */
	public void putNull(PreparedStatement stmt, int index) throws SQLException {
		stmt.setString(index, null);
	}

	/**
	 * Simple helper function used to determine of the specified JDBC
	 * type is representitive of a numeric type.  The definition of
	 * numeric type in this case may be assumed to be any type which
	 * has a corresponding (or coercibly corresponding) Java class
	 * which is a subclass of {@link Number}.  The default implementation
	 * should be suitable for every conceivable use-case.
	 * 
	 * @param type	The JDBC type which is to be tested.
	 * @return	<code>true</code> if the specified type represents a numeric
	 * 		type, otherwise <code>false</code>.
	 */
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
	
	/**
	 * Auto-magically retrieves the appropriate provider instance for the
	 * specified JDBC URI, passing it the given username and password.  This
	 * method actually delegates all of its interesting work to
	 * {@link #getInstance(String, String, String, boolean)}, passing <code>true</code>
	 * to enable auto-magical connection pool configuratoin by default.
	 * 
	 * @param uri	The JDBC URI for which a provider must be obtained.
	 * @param username	The database username (note: for implementations which
	 * 		do not make use of this field, <code>null</code> is permitted).
	 * @param password		The database password (note: for implementations which
	 * 		do not make use of this field, <code>null</code> is permitted).
	 * @return	A database provider corresponding to the database referenced by
	 * 		the <code>uri</code>.
	 */
	public final static DatabaseProvider getInstance(String uri, String username, String password) {
		return getInstance(uri, username, password, true);		// enable pooling by default (if available)
	}
	
	/**
	 * <p>Auto-magically retrieves the appropriate provider instance for the
	 * specified JDBC URI, passing it the given username and password.  Depending
	 * on the value of the <code>enablePooling</code> parameter, a connection
	 * pool library may also be auto-magically selected based on the CLASSPATH
	 * and the appropriate {@link PoolProvider} implementation returned, delegating
	 * to the actual requested provider.  If no pool provider can be located, the
	 * raw provider is returned (unpooled), irregardless of the <code>enablePooling</code>
	 * parameter.  If no database provider can be found for the given JDBC URI
	 * prefix, a {@link RuntimeException} will be thrown.  This should probably
	 * be changed to something a little less drastic, like returning <code>null</code>.</p>
	 * 
	 * <p>Technically speaking, this method doesn't perform the actual logic
	 * to find the database and/or pool providers.  Both searches are delegated
	 * to the {@link SupportedDBProvider} and {@link SupportedPoolProvider} enums,
	 * respectively.  The exception to this is that the convention for determining
	 * whether or not a pool provider is actually availalble <i>is</i> defined
	 * here.  The convention imposed is that all pool providers contained within
	 * the {@link SupportedPoolProvider} enum must define a static <code>isAvailable</code>
	 * method which tests for the existance of some critical class on the CLASSPATH.
	 * If this method is not found, the provider will be assumed to be unfound and
	 * the search will continue.  This convention isn't required for third-party
	 * pool providers, but it is to be enforced for any pool providers supplied by
	 * ActiveObjects itself.</p>
	 * 
	 * @param uri	The JDBC URI for which a provider must be obtained.
	 * @param username	The database username (note: for implementations which
	 * 		do not make use of this field, <code>null</code> is permitted).
	 * @param password		The database password (note: for implementations which
	 * 		do not make use of this field, <code>null</code> is permitted).
	 * @param enablePooling	A flag indicating whether or not connection pooling
	 * 		should be enabled (if possible).  Note that this flag is ignored if
	 * 		no connection pool library could be found on the CLASSPATH.
	 * @return	A database provider corresponding to the database referenced by
	 * 		the <code>uri</code>.
	 * @throws	RuntimeException	If no database provider is found for the
	 * 		specified URI prefix, or if the provider class could not be instantiated.
	 */
	public final static DatabaseProvider getInstance(String uri, String username, String password, 
			boolean enablePooling) {
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
				} catch (Throwable t) {
					continue;
				}
			}
		}
		
		return back;
	}
}