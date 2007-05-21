/*
 * Created on May 2, 2007
 */
package net.java.ao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Types;

import net.java.ao.db.IDatabaseProvider;
import net.java.ao.db.SupportedDBProvider;

/**
 * @author Daniel Spiewak
 */
public abstract class DatabaseProvider implements IDatabaseProvider {
	private String uri, username, password;
	
	protected DatabaseProvider(String uri, String username, String password) {
		this.uri = uri;
		
		this.username = username;
		this.password = password;
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
	
	public Connection getConnection() throws SQLException {
		try {
			getDriverClass();
		} catch (ClassNotFoundException e) {
			return null;
		}
		
		Connection back = DriverManager.getConnection(getURI(), getUsername(), getPassword());
		
		return back;
	}
	
	public final static DatabaseProvider getInstance(String uri, String username, String password) {
		SupportedDBProvider provider = SupportedDBProvider.getProviderForURI(uri);
		if (provider == null) {
			throw new RuntimeException("Unable to locate a valid database provider for URI: " + uri);
		}
		
		DatabaseProvider back = provider.createInstance(uri, username, password);
		if (back == null) {
			throw new RuntimeException("Unable to instantiate database provider for URI: " + uri);
		}
		
		return back;
	}
	
	protected static String convertTypeToString(int type) {
		switch (type) {
			case Types.BIGINT:
				return "BIGINT";
				
			case Types.BINARY:
				return "BINARY";
				
			case Types.BIT:
				return "BIT";
				
			case Types.BLOB:
				return "BLOB";
				
			case Types.BOOLEAN:
				return "BOOLEAN";
				
			case Types.CHAR:
				return "CHAR";
				
			case Types.CLOB:
				return "CLOB";
				
			case Types.DATE:
				return "DATE";
				
			case Types.DECIMAL:
				return "DECIMAL";
				
			case Types.DOUBLE:
				return "DOUBLE";
				
			case Types.FLOAT:
				return "FLOAT";
				
			case Types.INTEGER:
				return "INTEGER";
				
			case Types.LONGVARBINARY:
				return "LONGVARBINARY";
			
			case Types.LONGVARCHAR:
				return "LONGVARCHAR";
				
			case Types.NULL:
				return "NULL";
				
			case Types.NUMERIC:
				return "NUMERIC";
				
			case Types.REAL:
				return "REAL";
				
			case Types.REF:
				return "REF";
				
			case Types.SMALLINT:
				return "SMALLINT";
				
			case Types.SQLXML:
				return "SQLXML";
				
			case Types.STRUCT:
				return "STRUCT";
				
			case Types.TIME:
				return "TIME";
				
			case Types.TIMESTAMP:
				return "TIMESTAMP";
				
			case Types.VARBINARY:
				return "VARBINARY";
				
			case Types.VARCHAR:
				return "VARCHAR";
		}
		
		return null;
	}
}
