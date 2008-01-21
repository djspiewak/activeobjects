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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import net.java.ao.schema.TableNameConverter;
import net.java.ao.schema.ddl.DDLAction;

/**
 * <p>Abstract superclass for connection pool library abstractions.  This class
 * handles some of the grunt work for implementing a connection pool
 * provider, such as returning <code>null</code> for <code>getDriverClass()</code>
 * and delegating interesting methods to the database-specific provider.</p>
 * 
 * @author Daniel Spiewak
 */
public abstract class PoolProvider extends DatabaseProvider {
	private final DatabaseProvider delegate;

	/**
	 * Creates a new instance with the given delegate provider.  By convention,
	 * all pool providers declare a public constructor.  However, this convention is
	 * not enforced, hence the protected status of this superclass constructor.
	 * 
	 * @param delegate	The {@link DatabaseProvider} instance to which most
	 * 	interesting method calls should be delegated (such as rendering and
	 * 	schema retrieval).
	 */
	protected PoolProvider(DatabaseProvider delegate) {
		super(delegate.getURI(), delegate.getUsername(), delegate.getPassword());
		
		this.delegate = delegate;
	}
	
	/**
	 * @return <code>null</code>
	 */
	@Override
	public Class<? extends Driver> getDriverClass() throws ClassNotFoundException {
		return null;
	}

	/**
	 * Retrieves the delegate {@link DatabaseProvider} instnace to which most
	 * interesting calls are passed.  This cannot be overridden by subclasses,
	 * for no particularlly good reason.
	 */
	public final DatabaseProvider getDelegate() {
		return delegate;
	}
	
	/**
	 * @see net.java.ao.DatabaseProvider#parseValue(int, String)
	 */
	@Override
	public Object parseValue(int type, String value) {
		return delegate.parseValue(type, value);
	}
	
	/**
	 * @see net.java.ao.DatabaseProvider#renderAction(DDLAction)
	 */
	@Override
	public String[] renderAction(DDLAction action) {
		return delegate.renderAction(action);
	}
	
	/**
	 * @see net.java.ao.DatabaseProvider#renderQuery(Query, TableNameConverter, boolean)
	 */
	@Override
	public String renderQuery(Query query, TableNameConverter converter, boolean count) {
		return delegate.renderQuery(query, converter, count);
	}
	
	/**
	 * @see net.java.ao.DatabaseProvider#setQueryStatementProperties(Statement, Query)
	 */
	@Override
	public void setQueryStatementProperties(Statement stmt, Query query) throws SQLException {
		delegate.setQueryStatementProperties(stmt, query);
	}
	
	/**
	 * @see net.java.ao.DatabaseProvider#setQueryResultSetProperties(ResultSet, Query)
	 */
	@Override
	public void setQueryResultSetProperties(ResultSet res, Query query) throws SQLException {
		delegate.setQueryResultSetProperties(res, query);
	}
	
	/**
	 * @see net.java.ao.DatabaseProvider#getTables(Connection)
	 */
	@Override
	public ResultSet getTables(Connection conn) throws SQLException {
		return delegate.getTables(conn);
	}
	
	/**
	 * @see net.java.ao.DatabaseProvider#insertReturningKey(Connection, Class, String, boolean, String, DBParam...)
	 */
	@Override
	public <T> T insertReturningKey(Connection conn, Class<T> pkType, String pkField, boolean pkIdentity, String table, DBParam... params) throws SQLException {
		return delegate.insertReturningKey(conn, pkType, pkField, pkIdentity, table, params);
	}
	
	@Override
	public void putNull(PreparedStatement stmt, int index) throws SQLException {
		delegate.putNull(stmt, index);
	}
	
	@Override
	protected void setPostConnectionProperties(Connection conn) throws SQLException {
		delegate.setPostConnectionProperties(conn);
	}
	
	@Override
	protected String renderAutoIncrement() {
		return "";
	}
	
	/**
	 * <p>Should release all resources held by the pool.  This is especially important
	 * to implement for pool providers, as conection pools may have connections
	 * which are being held (potentially) indefinitely.  It is important for developers
	 * to call this method to free resources, as well as it is important for custom
	 * implementation authors to implement the method to perform such a function.</p>
	 * 
	 * <p>Implementations should take the following form:</p>
	 * 
	 * <pre>public void dispose() {
	 *     connectionPool.freeAllConnections();
	 *     
	 *     super.dispose();
	 * }</pre>
	 * 
	 * <p>This method additionally delegates its call to the delegate provider instance,
	 * ensuring that (for databases which require it) database resources are appropriately
	 * freed.</p>
	 * 
	 * @see net.java.ao.DatabaseProvider#dispose()
	 */
	@Override
	public void dispose() {
		delegate.dispose();
		
		super.dispose();
	}
}
