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

import java.sql.Connection;
import java.sql.SQLException;

import net.java.ao.DatabaseProvider;
import net.java.ao.PoolProvider;

import org.apache.commons.dbcp.BasicDataSource;

/**
 * @author Daniel Spiewak
 */
public class DBCPPoolProvider extends PoolProvider {
	private BasicDataSource ds;
	
	public DBCPPoolProvider(DatabaseProvider delegate) {
		super(delegate);
		
		ds = new BasicDataSource();
		try {
			ds.setDriverClassName(delegate.getDriverClass().getCanonicalName());
		} catch (ClassNotFoundException e) {
		}
		ds.setUsername(getUsername());
		ds.setPassword(getPassword());
		ds.setUrl(getURI());
		
		ds.setMaxActive(30);
	}
	
	@Override
	protected Connection getConnectionImpl() throws SQLException {
		return ds.getConnection();
	}

	@Override
	public void dispose() {
		try {
			ds.close();
		} catch (SQLException e) {
		}
		
		ds = null;
		
		super.dispose();
	}

	public static boolean isAvailable() {
		try {
			Class.forName("org.apache.commons.dbcp.BasicDataSource");
		} catch (ClassNotFoundException e) {
			return false;
		}
		
		return true;
	}
}
