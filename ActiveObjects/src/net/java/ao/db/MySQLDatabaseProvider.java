/*
 * Created on May 16, 2007
 */
package net.java.ao.db;

import java.sql.Driver;

import net.java.ao.DatabaseProvider;

/**
 * @author Daniel Spiewak
 */
public class MySQLDatabaseProvider extends DatabaseProvider {

	public MySQLDatabaseProvider(String uri, String username, String password) {
		super(uri, username, password);
	}

	public Class<? extends Driver> getDriverClass() throws ClassNotFoundException {
		return (Class<? extends Driver>) Class.forName("com.mysql.jdbc.Driver");
	}
}
