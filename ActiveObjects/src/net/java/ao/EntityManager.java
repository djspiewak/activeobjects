/*
 * Created on May 2, 2007
 */
package net.java.ao;

import static net.java.ao.Utilities.convertDowncaseName;
import static net.java.ao.Utilities.convertSimpleClassName;
import static net.java.ao.Utilities.getTableName;

import java.lang.reflect.Array;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.java.ao.db.IDatabaseProvider;

/**
 * @author Daniel Spiewak
 */
public final class EntityManager {
	private static EntityManager instance;
	
	private volatile IDatabaseProvider provider;
	
	private Map<Entity, EntityProxy<? extends Entity>> proxies;
	private final ReadWriteLock proxyLock = new ReentrantReadWriteLock();
	
	private Map<CacheKey, Entity> cache;
	private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();
	
	private EntityManager(IDatabaseProvider provider) {
		this.provider = provider;
		
		proxies = new WeakHashMap<Entity, EntityProxy<? extends Entity>>();
		cache = new WeakHashMap<CacheKey, Entity>();
	}
	
	public <T extends Entity> T getEntity(int id, Class<T> type) {
		cacheLock.writeLock().lock();
		try {
			if (cache.containsKey(new CacheKey(id, type))) {
				return (T) cache.get(new CacheKey(id, type));
			}

			EntityProxy<T> proxy = new EntityProxy<T>(this, type);
			T back = (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[] {type}, proxy);
			back.setID(id);

			proxyLock.writeLock().lock();
			try {
				proxies.put(back, proxy);
			} finally {
				proxyLock.writeLock().unlock();
			}
			
			cache.put(new CacheKey(id, type), back);
			
			return back;
		} finally {
			cacheLock.writeLock().unlock();
		}
	}
	
	public <T extends Entity> T createEntity(Class<T> type) throws SQLException {
		T back = null;
		String table = getTableName(type);
		
		Connection conn = DBEncapsulator.getInstance(provider).getConnection();
		try {
			PreparedStatement stmt = conn.prepareStatement("INSERT INTO " + table + " () VALUES ()");
			stmt.executeUpdate();
			
			ResultSet res = stmt.getGeneratedKeys();
			if (res.next()) {
				 back = getEntity(res.getInt(1), type);
			}
			res.close();
			stmt.close();
		} finally {
			DBEncapsulator.getInstance(provider).closeConnection(conn);
		}
		
		return back;
	}
	
	public <T extends Entity> T[] getAllEntities(Class<T> type) throws SQLException {
		List<T> back = new ArrayList<T>();
		String table = convertDowncaseName(
				convertSimpleClassName(type.getCanonicalName()));
		
		Connection conn = DBEncapsulator.getInstance(provider).getConnection();
		try {
			PreparedStatement stmt = conn.prepareStatement("SELECT id FROM " + table);
			ResultSet res = stmt.executeQuery();
			
			while (res.next()) {
				back.add(getEntity(res.getInt("id"), type));
			}
			res.close();
			stmt.close();
		} finally {
			DBEncapsulator.getInstance(provider).closeConnection(conn);
		}
		
		return back.toArray((T[]) Array.newInstance(type, back.size()));
	}

	public IDatabaseProvider getProvider() {
		return provider;
	}

	public void setProvider(IDatabaseProvider provider) {
		this.provider = provider;
	}
	
	<T extends Entity> EntityProxy<T> getProxyForEntity(T entity) {
		proxyLock.readLock().lock();
		try {
			return (EntityProxy<T>) proxies.get(entity);
		} finally {
			proxyLock.readLock().unlock();
		}
	}

	public static synchronized EntityManager getInstance(IDatabaseProvider provider) {
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
