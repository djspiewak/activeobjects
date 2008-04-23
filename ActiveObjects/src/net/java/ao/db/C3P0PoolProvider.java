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

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;

import net.java.ao.DatabaseProvider;
import net.java.ao.PoolProvider;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.DataSources;

/**
 * @author Daniel Spiewak
 */
public class C3P0PoolProvider extends PoolProvider {
	private ComboPooledDataSource cpds;

	public C3P0PoolProvider(DatabaseProvider delegate) {
		super(delegate);

		cpds = new ComboPooledDataSource();
		try {
			cpds.setDriverClass(delegate.getDriverClass().getCanonicalName());
		} catch (PropertyVetoException e) {
		} catch (ClassNotFoundException e) {
		}
		cpds.setJdbcUrl(getURI());
		cpds.setUser(getUsername());
		cpds.setPassword(getPassword());
		
		cpds.setMaxPoolSize(30);
		cpds.setMaxStatements(180);
	}
	
	@Override
	protected Connection getConnectionImpl() throws SQLException {
		return cpds.getConnection();
	}
	
	@Override
	public void dispose() {
		super.dispose();
		
		try {
			DataSources.destroy(cpds);
		} catch (SQLException e) {
		}
	}
	
	public static boolean isAvailable() {
		try {
			Class.forName("com.mchange.v2.c3p0.ComboPooledDataSource");
		} catch (ClassNotFoundException e) {
			return false;
		}
		
		return true;
	}
}
