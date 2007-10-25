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
import net.java.ao.EntityManager;

/**
 * <p>Contains a list of all internally supported database providers and their
 * associated JDBC prefixes (e.g. "jdbc:mysql").  This list is used in the
 * auto-magical database driver selection based on JDBC URI.</p>
 * 
 * <p>This list does <i>not</i> include third-party database providers.  Thus, 
 * if you implement a database provider for Sybase, you must pass it directly
 * to {@link EntityManager}; you cannot rely upon the auto-magical URI
 * parsing as it will not apply to the new provider.</p>
 * 
 * <p>This enum is designed primarily for INTERNAL use within AO.  While it
 * is perfectly acceptible to utilize this enum externally, the API may change
 * unnexpectedly, undocumented results may occur, you know the drill.</p>
 * 
 * @author Daniel Spiewak
 * @see net.java.ao.DatabaseProvider
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
	
	/**
	 * Returns the <code>SupportedDBProvider</code> which corresponds to the
	 * database provider which corresponds to the specified JDBC URI.  If no corresponding
	 * proivder is found, <code>null</code> is returned.
	 * 
	 * @param uri	The JDBC URI for which a database provider is required.
	 * @return The enum value which corresponds to the required database provider.
	 */
	public static SupportedDBProvider getProviderForURI(String uri) {
		for (SupportedDBProvider provider : values()) {
			if (uri.trim().startsWith(provider.prefix.trim())) {
				return provider;
			}
		}
		
		return null;
	}
}
