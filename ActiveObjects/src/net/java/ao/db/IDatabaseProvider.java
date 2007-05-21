/*
 * Created on May 17, 2007
 */
package net.java.ao.db;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;

import net.java.ao.db.ddl.DDLTable;

/**
 * @author Daniel Spiewak
 */
public interface IDatabaseProvider {
	public Class<? extends Driver> getDriverClass() throws ClassNotFoundException;
	public Connection getConnection() throws SQLException;
	
	public String render(DDLTable table);
}
