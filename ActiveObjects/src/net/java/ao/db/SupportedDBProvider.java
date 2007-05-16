/*
 * Created on May 16, 2007
 */
package net.java.ao.db;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import net.java.ao.DatabaseProvider;

/**
 * @author Daniel Spiewak
 */
public enum SupportedDBProvider {
	MYSQL("jdbc:mysql", MySQLDatabaseProvider.class);
	
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
