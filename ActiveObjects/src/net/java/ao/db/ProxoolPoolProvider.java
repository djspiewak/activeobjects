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
import java.util.Properties;

import net.java.ao.DatabaseProvider;
import net.java.ao.PoolProvider;

import org.logicalcobwebs.proxool.ProxoolException;
import org.logicalcobwebs.proxool.ProxoolFacade;

/**
 * @author Daniel Spiewak
 */
public class ProxoolPoolProvider extends PoolProvider {
	private static final String CLASSNAME = "org.logicalcobwebs.proxool.ProxoolDriver";
	
	private final String alias;

	public ProxoolPoolProvider(DatabaseProvider delegate) throws ProxoolException {
		this(delegate, "activeobjects");
	}
	
	public ProxoolPoolProvider(DatabaseProvider delegate, String alias) throws ProxoolException {
		super(delegate);
		
		this.alias = alias;
		
		try {
			getDriverClass();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		Properties props = new Properties();
		props.setProperty("proxool.maximum-connection-count", "30");
		props.setProperty("user", getUsername());
		props.setProperty("password", getPassword());
		
		String driverClass = null;
		try {
			driverClass = delegate.getDriverClass().getCanonicalName();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		String driverUrl = getURI();
		String url = "proxool." + alias + ":" + driverClass + ":" + driverUrl;
		
		ProxoolFacade.registerConnectionPool(url, props);
	}
	
	@Override
	public Class<? extends Driver> getDriverClass() throws ClassNotFoundException {
		return (Class<? extends Driver>) Class.forName(CLASSNAME);
	}
	
	@Override
	protected Connection getConnectionImpl() throws SQLException {
		return DriverManager.getConnection("proxool." + alias);
	}
	
	@Override
	public void dispose() {
		try {
			ProxoolFacade.removeConnectionPool(alias);
		} catch (ProxoolException e) {
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
