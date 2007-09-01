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
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;

import net.java.ao.DatabaseProvider;
import net.java.ao.PoolProvider;

/**
 * @author Daniel Spiewak
 */
public class ProxoolPoolProvider extends PoolProvider {
	private static final String CLASSNAME = "org.logicalcobwebs.proxool.ProxoolDriver";

	public ProxoolPoolProvider(DatabaseProvider delegate) {
		super(delegate);
	}
	
	@Override
	public Class<? extends Driver> getDriverClass() throws ClassNotFoundException {
		return (Class<? extends Driver>) Class.forName(CLASSNAME);
	}
	
	@Override
	protected Connection getConnectionImpl() throws SQLException {
		try {
			return DriverManager.getConnection("proxool.example:" + getDelegate().getDriverClass().getCanonicalName() 
					+ ":" + getDelegate().getURI());
		} catch (ClassNotFoundException e) {
			return null;
		}
	}
	
	public static boolean isAvailable() {
		try {
			Class.forName(CLASSNAME);
		} catch (ClassNotFoundException e) {
			return false;
		}
		
		return true;
	}
}
