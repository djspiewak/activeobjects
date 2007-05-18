/*
 * Created on May 17, 2007
 */
package net.java.ao.db;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;

/**
 * @author Daniel Spiewak
 */
public interface IDatabaseProvider {
	public Connection getConnection() throws SQLException;
	
	public Class<? extends Driver> getDriverClass() throws ClassNotFoundException;
}
