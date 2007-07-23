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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import net.java.ao.DatabaseProvider;

/**
 * @author Daniel Spiewak
 */
public enum SupportedDBProvider {
	MYSQL("jdbc:mysql", MySQLDatabaseProvider.class),
	NETWORK_DERBY("jdbc:derby://", ClientDerbyDatabaseProvider.class),
	EMBEDDED_DERBY("jdbc:derby", EmbeddedDerbyDatabaseProvider.class),
	ORACLE_THIN("jdbc:oracle:thin", OracleDatabaseProvider.class),
	ORACLE_OCI("jdbc:oracle:oci", OracleDatabaseProvider.class),
	POSTGRESQL("jdbc:postgresql", PostgreSQLDatabaseProvider.class),
	MS_SQL_SERVER("jdbc:sqlserver", SQLServerDatabaseProvider.class),
	JTDS_MS_SQL_SERVER("jdbc:jtds:sqlserver", JTDSSQLServerDatabaseProvider.class),
	NETWORK_HSQLDB("jdbc:hsqldb://", HSQLDatabaseProvider.class),
	EMBEDDED_HSQLDB("jdbc:hsqldb", HSQLDatabaseProvider.class);
	
	private String prefix;
	private Class<? extends DatabaseProvider> type;
	
	private SupportedDBProvider(String prefix, Class<? extends DatabaseProvider> type) {
		this.prefix = prefix;
		this.type = type;
	}
	
	public String getPrefix() {
		return prefix;
	}
	
	public Class<? extends DatabaseProvider> getType() {
		return type;
	}
	
	public DatabaseProvider createInstance(String uri, String username, String password) {
		DatabaseProvider back = null;
		
		try {
			Constructor<? extends DatabaseProvider> constructor = type.getDeclaredConstructor(String.class, String.class, String.class);
			constructor.setAccessible(true);
			
			back = constructor.newInstance(uri, username, password);
		} catch (SecurityException e) {
		} catch (NoSuchMethodException e) {
		} catch (IllegalArgumentException e) {
		} catch (InstantiationException e) {
		} catch (IllegalAccessException e) {
		} catch (InvocationTargetException e) {
		}
		
		return back;
	}
	
	public static SupportedDBProvider getProviderForURI(String uri) {
		for (SupportedDBProvider provider : values()) {
			if (uri.trim().startsWith(provider.prefix.trim())) {
				return provider;
			}
		}
		
		return null;
	}
}
