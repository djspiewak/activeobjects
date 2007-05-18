/*
 * Created on May 17, 2007
 */
package net.java.ao.db;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;

import net.java.ao.DatabaseProvider;

import org.apache.commons.dbcp.BasicDataSource;

/**
 * @author Daniel Spiewak
 */
public class DBCPPoolProvider extends DatabaseProvider implements IPoolProvider {
	private BasicDataSource ds;
	
	public DBCPPoolProvider(DatabaseProvider delegate) {
		super(delegate.getURI(), delegate.getUsername(), delegate.getPassword());
		
		ds = new BasicDataSource();
		try {
			ds.setDriverClassName(delegate.getDriverClass().getCanonicalName());
		} catch (ClassNotFoundException e) {
		}
		ds.setUsername(getUsername());
		ds.setPassword(getPassword());
		ds.setUrl(getURI());
	}
	
	@Override
	public Connection getConnection() throws SQLException {
		return ds.getConnection();
	}
	
	public Class<? extends Driver> getDriverClass() throws ClassNotFoundException {
		return null;
	}

	public void close() {
		try {
			ds.close();
		} catch (SQLException e) {
		}
		
		ds = null;
	}
}
