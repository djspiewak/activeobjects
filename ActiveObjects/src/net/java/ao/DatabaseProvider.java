/*
 * Created on May 2, 2007
 */
package net.java.ao;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Daniel Spiewak
 */
public interface DatabaseProvider {
	
	public Connection getConnection() throws SQLException;
}
