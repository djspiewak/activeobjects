/*
 * Copyright 2007, Daniel Spiewak
 * All rights reserved
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided
 *     with the distribution.
 *   * Neither the name of the ActiveObjects project nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.java.ao;

import static net.java.ao.Common.getTableName;

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
	private volatile IDatabaseProvider provider;
	
	private Map<Entity, EntityProxy<? extends Entity>> proxies;
	private final ReadWriteLock proxyLock = new ReentrantReadWriteLock();
	
	private Map<CacheKey, Entity> cache;
	private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();
	
	public EntityManager(IDatabaseProvider provider) {
		this.provider = provider;
		
		proxies = new WeakHashMap<Entity, EntityProxy<? extends Entity>>();
		cache = new WeakHashMap<CacheKey, Entity>();
	}
	
	public EntityManager(String uri, String username, String password) {
		this(DatabaseProvider.getInstance(uri, username, password));
	}
	
	public <T extends Entity> T[] get(Class<T> type, int... ids) {
		T[] back = (T[]) Array.newInstance(type, ids.length);
		int index = 0;
		
		for (int id : ids) {
			cacheLock.writeLock().lock();
			try {
				T entity = (T) cache.get(new CacheKey(id, type));
				if (entity != null) {
					back[index++] = entity;
					continue;
				}
	
				EntityProxy<T> proxy = new EntityProxy<T>(this, type);
				entity = (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[] {type}, proxy);
				entity.setID(id);
	
				proxyLock.writeLock().lock();
				try {
					proxies.put(entity, proxy);
				} finally {
					proxyLock.writeLock().unlock();
				}
				
				cache.put(new CacheKey(id, type), entity);
				
				back[index++] = entity;
			} finally {
				cacheLock.writeLock().unlock();
			}
		}
		
		return back;
	}
	
	public <T extends Entity> T get(Class<T> type, int id) {
		return get(type, new int[] {id})[0];
	}
	
	public <T extends Entity> T create(Class<T> type, DBParam... params) throws SQLException {
		T back = null;
		String table = getTableName(type);
		
		Connection conn = DBEncapsulator.getInstance(provider).getConnection();
		try {
			StringBuilder sql = new StringBuilder("INSERT INTO " + table + " (");
			
			for (DBParam param : params) {
				sql.append(param.getField());
				sql.append(',');
			}
			sql.setLength(sql.length() - 1);
			
			sql.append(") VALUES (");
			
			for (@SuppressWarnings("unused") DBParam param : params) {
				sql.append("?,");
			}
			sql.setLength(sql.length() - 1);
			
			sql.append(")");
			PreparedStatement stmt = conn.prepareStatement(sql.toString());
			
			for (int i = 0; i < params.length; i++) {
				stmt.setObject(i + 1, params[i].getValue());
			}
			
			stmt.executeUpdate();
			
			ResultSet res = stmt.getGeneratedKeys();
			if (res.next()) {
				 back = get(type, res.getInt(1));
			}
			res.close();
			stmt.close();
		} finally {
			DBEncapsulator.getInstance(provider).closeConnection(conn);
		}
		
		return back;
	}
	
	/**
	 * The actual table name is aliased as "prime" (for use in the criteria and within the join).
	 */
	public <T extends Entity> T[] find(Class<T> type, String join, String criteria, Object... parameters) throws SQLException {
		List<T> back = new ArrayList<T>();
		String table = getTableName(type);
		
		Connection conn = DBEncapsulator.getInstance(provider).getConnection();
		try {
			PreparedStatement stmt = conn.prepareStatement("SELECT prime.id FROM " + table + " prime "
					+ (join != null ? join : "") + (criteria != null ? " WHERE " + criteria : ""));
			
			if (criteria != null) {
				for (int i = 0; i < parameters.length; i++) {
					if (parameters[i] instanceof Entity) {
						parameters[i] = ((Entity) parameters[i]).getID();
					}
					
					stmt.setObject(i + 1, parameters[i]);
				}
			}
			
			ResultSet res = stmt.executeQuery();
			while (res.next()) {
				back.add(get(type, res.getInt("prime.id")));
			}
			res.close();
			stmt.close();
		} finally {
			DBEncapsulator.getInstance(provider).closeConnection(conn);
		}
		
		return back.toArray((T[]) Array.newInstance(type, back.size()));
	}
	
	public <T extends Entity> T[] find(Class<T> type, String criteria, Object... parameters) throws SQLException {
		return find(type, null, criteria, parameters);
	}
	
	public <T extends Entity> T[] find(Class<T> type) throws SQLException {
		return find(type, null, null, (Object[]) null);
	}
	
	public <T extends Entity> T[] findWithSQL(Class<T> type, String idField, String sql, Object... parameters) throws SQLException {
		List<T> back = new ArrayList<T>();
		
		Connection conn = DBEncapsulator.getInstance(provider).getConnection();
		try {
			PreparedStatement stmt = conn.prepareStatement(sql);
			
			for (int i = 0; i < parameters.length; i++) {
				if (parameters[i] instanceof Entity) {
					parameters[i] = ((Entity) parameters[i]).getID();
				}
				
				stmt.setObject(i + 1, parameters[i]);
			}
			
			ResultSet res = stmt.executeQuery();
			while (res.next()) {
				back.add(get(type, res.getInt(idField)));
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

	<T extends Entity> EntityProxy<T> getProxyForEntity(T entity) {
		proxyLock.readLock().lock();
		try {
			return (EntityProxy<T>) proxies.get(entity);
		} finally {
			proxyLock.readLock().unlock();
		}
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
