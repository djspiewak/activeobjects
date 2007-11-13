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
package net.java.ao.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import net.java.ao.DatabaseProvider;
import net.java.ao.PoolProvider;
import snaq.db.ConnectionPoolManager;

/**
 * @author Daniel Spiewak
 */
public class DBPoolProvider extends PoolProvider {
	private ConnectionPoolManager pool;

	public DBPoolProvider(DatabaseProvider delegate) {
		super(delegate);
		
		String driverName = null;
		try {
			driverName = delegate.getDriverClass().getCanonicalName();
		} catch (ClassNotFoundException e) {
		}
		
		Properties props = new Properties();
		props.setProperty("drivers", driverName);
		
		props.setProperty("activeobjects.url", getURI());
		props.setProperty("activeobjects.user", getUsername());
		props.setProperty("activeobjects.password", getPassword());
		props.setProperty("activeobjects.maxpool", "180");
		props.setProperty("activeobjects.maxconn", "30");
		props.setProperty("activeobjects.cache", "true");
		
		ConnectionPoolManager.createInstance(props);
		try {
			pool = ConnectionPoolManager.getInstance();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected Connection getConnectionImpl() throws SQLException {
		return pool.getConnection("activeobjects");
	}
	
	@Override
	public void dispose() {
		pool.release();
	}
	
	public static boolean isAvailable() {
		try {
			Class.forName("snaq.db.ConnectionPool");
		} catch (ClassNotFoundException e) {
			return false;
		}
		
		return true;
	}
}
