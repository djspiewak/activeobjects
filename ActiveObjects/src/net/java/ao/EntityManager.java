/*
 * Created on May 2, 2007
 */
package net.java.ao;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.java.ao.Utilities.convertDowncaseName;
import static net.java.ao.Utilities.convertSimpleClassName;

/**
 * @author Daniel Spiewak
 */
public final class EntityManager {
	private static EntityManager instance;
	
	private DatabaseProvider provider;
	
	private Map<CacheKey, Entity> cache;
	
	private EntityManager(DatabaseProvider provider) {
		this.provider = provider;
		
		cache = new HashMap<CacheKey, Entity>();
	}
	
	public <T extends Entity> T getEntity(int id, Class<T> type) {
		if (cache.containsKey(new CacheKey(id, type))) {
			return (T) cache.get(new CacheKey(id, type));
		}
		
		T back = (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[] {type}, new EntityProxy<T>(this, type));
		back.setID(id);
		
		cache.put(new CacheKey(id, type), back);
		
		return back;
	}
	
	public <T extends Entity> T[] getAllEntities(Class<T> type) throws SQLException {
		List<T> back = new ArrayList<T>();
		String table = convertDowncaseName(
				convertSimpleClassName(type.getCanonicalName()));
		
		Connection conn = provider.getConnection();
		try {
			PreparedStatement stmt = conn.prepareStatement("SELECT id FROM " + table);
			ResultSet res = stmt.executeQuery();
			
			while (res.next()) {
				back.add(getEntity(res.getInt("id"), type));
			}
			res.close();
			stmt.close();
		} finally {
			conn.close();
		}
		
		return back.toArray((T[]) Array.newInstance(type, back.size()));
	}

	public DatabaseProvider getProvider() {
		return provider;
	}

	public void setProvider(DatabaseProvider provider) {
		this.provider = provider;
	}

	public static synchronized EntityManager getInstance(DatabaseProvider provider) {
		if (instance == null) {
			instance = new EntityManager(provider);
		}
		
		return instance;
	}
	
	private static class CacheKey {
		private int id;
		private Class<? extends Entity> type;
		
		public CacheKey(int id, Class<? extends Entity> type) {
			this.id = id;
			this.type = type;
		}
		
		public int hashCode() {
			return type.hashCode() + (id << 4);
		}
		
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			
			if (obj instanceof CacheKey) {
				CacheKey key = (CacheKey) obj;
				
				if (id == key.id && type.equals(key.type)) {
					return true;
				}
			}
			
			return false;
		}
	}
}
