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
package net.java.ao;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.ao.schema.OnUpdate;
import net.java.ao.types.DatabaseType;
import net.java.ao.types.TypeManager;

/**
 * @author Daniel Spiewak
 */
class EntityProxy<T extends RawEntity<K>, K> implements InvocationHandler {
	private static final Pattern WHERE_PATTERN = Pattern.compile("([\\d\\w]+)\\s*(=|>|<|LIKE|IS)");
	
	private K key;
	private Method pkAccessor;
	private String pkFieldName;
	
	private Class<T> type;

	private EntityManager manager;

	private ImplementationWrapper<T> implementation;

	private final Map<String, Object> cache;
	private final Set<String> nullSet;
	private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();

	private final Set<String> dirtyFields;
	private final ReadWriteLock dirtyFieldsLock = new ReentrantReadWriteLock();
	
	private final Set<Class<? extends RawEntity<?>>> toFlushRelations = new HashSet<Class<? extends RawEntity<?>>>();
	private final ReadWriteLock toFlushLock = new ReentrantReadWriteLock();

	private List<PropertyChangeListener> listeners;

	public EntityProxy(EntityManager manager, Class<T> type, K key) {
		this.key = key;
		this.type = type;
		this.manager = manager;
		
		pkAccessor = Common.getPrimaryKeyAccessor(type);
		pkFieldName = Common.getPrimaryKeyField(type, getManager().getFieldNameConverter());

		cache = new HashMap<String, Object>();
		nullSet = new HashSet<String>();
		dirtyFields = new LinkedHashSet<String>();

		listeners = new LinkedList<PropertyChangeListener>();
	}

	@SuppressWarnings("unchecked")
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (method.getName().equals("getEntityType")) {
			return type;
		}

		if (implementation == null) {
			implementation = new ImplementationWrapper<T>((T) proxy);
		}

		MethodImplWrapper methodImpl = implementation.getMethod(method.getName(), method.getParameterTypes());
		if (methodImpl != null) {
			if (!Common.getCallingClass(1).equals(methodImpl.getMethod().getDeclaringClass()) 
					&& !methodImpl.getMethod().getDeclaringClass().equals(Object.class)) {
				return methodImpl.getMethod().invoke(methodImpl.getInstance(), args);
			}
		}

		if (method.getName().equals(pkAccessor.getName())) {
			return getKey();
		} else if (method.getName().equals("save")) {
			save((RawEntity<K>) proxy);

			return Void.TYPE;
		} else if (method.getName().equals("getTableName")) {
			return getTableName();
		} else if (method.getName().equals("getEntityManager")) {
			return getManager();
		} else if (method.getName().equals("addPropertyChangeListener")) {
			addPropertyChangeListener((PropertyChangeListener) args[0]);
		} else if (method.getName().equals("removePropertyChangeListener")) {
			removePropertyChangeListener((PropertyChangeListener) args[0]);
		} else if (method.getName().equals("hashCode")) {
			return hashCodeImpl();
		} else if (method.getName().equals("equals")) {
			return equalsImpl((RawEntity<K>) proxy, args[0]);
		} else if (method.getName().equals("toString")) {
			return toStringImpl();
		}

		String tableName = getManager().getTableNameConverter().getName(type);

		Mutator mutatorAnnotation = method.getAnnotation(Mutator.class);
		Accessor accessorAnnotation = method.getAnnotation(Accessor.class);
		OneToMany oneToManyAnnotation = method.getAnnotation(OneToMany.class);
		ManyToMany manyToManyAnnotation = method.getAnnotation(ManyToMany.class);
		OnUpdate onUpdateAnnotation = method.getAnnotation(OnUpdate.class);

		if (mutatorAnnotation != null) {
			invokeSetter((T) proxy, mutatorAnnotation.value(), args[0], onUpdateAnnotation != null);
			return Void.TYPE;
		} else if (accessorAnnotation != null) {
			return invokeGetter(getKey(), tableName, accessorAnnotation.value(), method.getReturnType(), onUpdateAnnotation != null);
		} else if (oneToManyAnnotation != null && method.getReturnType().isArray() 
				&& Common.interfaceInheritsFrom(method.getReturnType().getComponentType(), RawEntity.class)) {
			Class<? extends RawEntity<?>> type = (Class<? extends RawEntity<?>>) method.getReturnType().getComponentType();

			return retrieveRelations((RawEntity<K>) proxy, new String[0], 
					new String[] { Common.getPrimaryKeyField(type, getManager().getFieldNameConverter()) }, 
					(Class<? extends RawEntity>) type, oneToManyAnnotation.where());
		} else if (manyToManyAnnotation != null && method.getReturnType().isArray() 
				&& Common.interfaceInheritsFrom(method.getReturnType().getComponentType(), RawEntity.class)) {
			Class<? extends RawEntity<?>> throughType = (Class<? extends RawEntity<?>>) manyToManyAnnotation.value();
			Class<? extends RawEntity<?>> type = (Class<? extends RawEntity<?>>) method.getReturnType().getComponentType();

			return retrieveRelations((RawEntity<K>) proxy, null, 
					Common.getMappingFields(getManager().getFieldNameConverter(), 
							throughType, type), throughType, (Class<? extends RawEntity>) type, 
							manyToManyAnnotation.where());
		} else if (Common.isAccessor(method)) {
			return invokeGetter(getKey(), tableName, getManager().getFieldNameConverter().getName(type, method), 
					method.getReturnType(), onUpdateAnnotation == null);
		} else if (Common.isMutator(method)) {
			invokeSetter((T) proxy, getManager().getFieldNameConverter().getName(type, method), args[0], onUpdateAnnotation == null);

			return Void.TYPE;
		}

		return null;
	}

	public K getKey() {
		return key;
	}

	public String getTableName() {
		return getManager().getTableNameConverter().getName(type);
	}

	@SuppressWarnings("unchecked")
	public void save(RawEntity entity) throws SQLException {
		dirtyFieldsLock.writeLock().lock();
		try {
			if (dirtyFields.isEmpty()) {
				return;
			}

			String table = getTableName();
			TypeManager manager = TypeManager.getInstance();
			Connection conn = getConnectionImpl();

			cacheLock.readLock().lock();
			try {
				StringBuilder sql = new StringBuilder("UPDATE " + table + " SET ");

				for (String field : dirtyFields) {
					sql.append(field);

					if (cache.containsKey(field.toLowerCase())) {
						sql.append(" = ?,");
					} else {
						sql.append(" = NULL,");
					}
				}

				if (dirtyFields.size() > 0) {
					sql.setLength(sql.length() - 1);
				}

				sql.append(" WHERE ").append(pkFieldName).append(" = ?");

				Logger.getLogger("net.java.ao").log(Level.INFO, sql.toString());
				PreparedStatement stmt = conn.prepareStatement(sql.toString());

				int index = 1;
				for (String field : dirtyFields) {
					if (nullSet.contains(field.toLowerCase())) {
						stmt.setString(index++, null);
					} else if (cache.containsKey(field.toLowerCase())) {
						Object obj = cache.get(field.toLowerCase());
						Class javaType = obj.getClass();
						
						if (obj instanceof RawEntity) {
							javaType = ((RawEntity) obj).getEntityType();
						}
						
						manager.getType(javaType).putToDatabase(index++, stmt, obj);
					}
				}
				((DatabaseType) Common.getPrimaryKeyType(type)).putToDatabase(index++, stmt, key);
				
				toFlushLock.writeLock().lock();
				try {
					getManager().getRelationsCache().remove(toFlushRelations.toArray(new Class[toFlushRelations.size()]));
					toFlushRelations.clear();
				} finally {
					toFlushLock.writeLock().unlock();
				}
				
				getManager().getRelationsCache().remove(entity, dirtyFields.toArray(new String[dirtyFields.size()]));
				
				stmt.executeUpdate();

				dirtyFields.removeAll(dirtyFields);

				stmt.close();
			} finally {
				cacheLock.readLock().unlock();

				closeConnectionImpl(conn);
			}
		} finally {
			dirtyFieldsLock.writeLock().unlock();
		}
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		listeners.add(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		listeners.remove(listener);
	}

	public int hashCodeImpl() {
		return (key.hashCode() + type.hashCode()) % (2 << 15);
	}

	public boolean equalsImpl(RawEntity<K> proxy, Object obj) {
		if (proxy == obj) {
			return true;
		}

		if (obj instanceof RawEntity) {
			RawEntity<?> entity = (RawEntity<?>) obj;
			
			String ourTableName = getManager().getTableNameConverter().getName(proxy.getEntityType());
			String theirTableName = getManager().getTableNameConverter().getName(entity.getEntityType());

			return Common.getPrimaryKeyValue(entity).equals(key) && theirTableName.equals(ourTableName);
		}

		return false;
	}

	public String toStringImpl() {
		return getManager().getTableNameConverter().getName(type) + " {" + pkFieldName + " = " + key.toString() + "}";
	}

	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		if (obj instanceof EntityProxy) {
			EntityProxy<?, ?> proxy = (EntityProxy<?, ?>) obj;

			if (proxy.type.equals(type) && proxy.key == key) {
				return true;
			}
		}

		return false;
	}

	public int hashCode() {
		return type.hashCode();
	}

	void addToCache(String key, Object value) {
		if (key.trim().equalsIgnoreCase(pkFieldName)) {
			return;
		}

		cacheLock.writeLock().lock();
		try {
			if (value == null) {
				nullSet.add(key.toLowerCase());
			} else if (!cache.containsKey(key.toLowerCase())) {
				cache.put(key.toLowerCase(), value);
			}
		} finally {
			cacheLock.writeLock().unlock();
		}
	}

	Class<T> getType() {
		return type;
	}

	// any dirty fields are kept in the cache, since they have yet to be saved
	void flushCache() {
		cacheLock.writeLock().lock();
		dirtyFieldsLock.readLock().lock();
		try {
			for (String fieldName : cache.keySet()) {
				if (!dirtyFields.contains(fieldName.toLowerCase())) {
					cache.remove(fieldName.toLowerCase());
					nullSet.remove(fieldName.toLowerCase());
				}
			}
		} finally {
			dirtyFieldsLock.readLock().unlock();
			cacheLock.writeLock().unlock();
		}
	}
	
	private EntityManager getManager() {
		return manager;
	}

	private Connection getConnectionImpl() throws SQLException {
		return getManager().getProvider().getConnection();
	}

	private void closeConnectionImpl(Connection conn) throws SQLException {
		conn.close();
	}

	private <V> V invokeGetter(K key, String table, String name, Class<V> type, boolean shouldCache) throws Throwable {
		V back = null;

		if (shouldCache) {
			cacheLock.writeLock().lock();
		}
		try {
			if (shouldCache && nullSet.contains(name.toLowerCase())) {
				if (type.isPrimitive()) {
					if (type.equals(boolean.class)) {
						return (V) new Boolean(false);
					} else if (type.equals(char.class)) {
						return (V) new Character(' ');
					} else if (type.equals(int.class)) {
						return (V) new Integer(0);
					} else if (type.equals(short.class)) {
						return (V) new Short("0");
					} else if (type.equals(long.class)) {
						return (V) new Long("0");
					} else if (type.equals(float.class)) {
						return (V) new Float("0");
					} else if (type.equals(double.class)) {
						return (V) new Double("0");
					} else if (type.equals(byte.class)) {
						return (V) new Byte("0");
					}
				}
				
				return null;
			} else if (shouldCache && cache.containsKey(name.toLowerCase())) {
				Object value = cache.get(name.toLowerCase());

				if (instanceOf(value, type)) {
					return (V) value;
				} else if (Common.interfaceInheritsFrom(type, RawEntity.class) && value instanceof Integer) {
					value = getManager().get((Class<? extends RawEntity<Object>>) type, value);

					cache.put(name.toLowerCase(), value);
					return (V) value;
				} else {
					cache.remove(name.toLowerCase()); // invalid cached value
				}
			}

			Connection conn = getConnectionImpl();

			try {
				String sql = "SELECT " + name + " FROM " + table + " WHERE " + pkFieldName + " = ?";

				Logger.getLogger("net.java.ao").log(Level.INFO, sql);
				PreparedStatement stmt = conn.prepareStatement(sql);
				Common.getPrimaryKeyType(this.type).putToDatabase(1, stmt, key);

				ResultSet res = stmt.executeQuery();
				if (res.next()) {
					back = convertValue(res, name, type);
				}
				res.close();
				stmt.close();
			} finally {
				closeConnectionImpl(conn);
			}

			if (shouldCache) {
				cache.put(name.toLowerCase(), back);
				
				if (back == null) {
					nullSet.add(name.toLowerCase());
				}
			}
		} finally {
			if (shouldCache) {
				cacheLock.writeLock().unlock();
			}
		}

		return back;
	}

	private void invokeSetter(T entity, String name, Object value, boolean shouldCache) throws Throwable {
		Object oldValue = null;
		
		cacheLock.readLock().lock();
		try {
			if (cache.containsKey(name.toLowerCase())) {
				oldValue = cache.get(name.toLowerCase());
			}
		} finally {
			cacheLock.readLock().unlock();
		}
		
		if (value instanceof RawEntity) {
			toFlushLock.writeLock().lock();
			try {
				toFlushRelations.add(((RawEntity<?>) value).getEntityType());
			} finally {
				toFlushLock.writeLock().unlock();
			}
		}
		
		invokeSetterImpl(name, value);

		PropertyChangeEvent evt = new PropertyChangeEvent(entity, name, oldValue, value);
		for (PropertyChangeListener l : listeners) {
			l.propertyChange(evt);
		}

		dirtyFieldsLock.writeLock().lock();
		try {
			dirtyFields.add(name.toLowerCase());
		} finally {
			dirtyFieldsLock.writeLock().unlock();
		}
	}

	private void invokeSetterImpl(String name, Object value) throws Throwable {
		cacheLock.writeLock().lock();
		try {
			cache.put(name.toLowerCase(), value);
			
			if (value != null) {
				nullSet.remove(name.toLowerCase());
			} else {
				nullSet.add(name.toLowerCase());
			}
		} finally {
			cacheLock.writeLock().unlock();
		}
	}

	private <V extends RawEntity<K>> V[] retrieveRelations(RawEntity<K> entity, String[] inMapFields, String[] outMapFields, 
			Class<V> type, String where) throws SQLException {
		return retrieveRelations(entity, inMapFields, outMapFields, type, type, where);
	}

	private <V extends RawEntity<K>> V[] retrieveRelations(RawEntity<K> entity, String[] inMapFields, String[] outMapFields, 
			Class<? extends RawEntity<?>> type, Class<V> finalType, String where) throws SQLException {
		if (inMapFields == null || inMapFields.length == 0) {
			inMapFields = Common.getMappingFields(getManager().getFieldNameConverter(), 
					(Class<? extends RawEntity<?>>) type, this.type);
		}
		String[] fields = getFields(Common.getPrimaryKeyField(finalType, getManager().getFieldNameConverter()), 
				inMapFields, outMapFields, where);
		
		V[] cached = getManager().getRelationsCache().get(entity, finalType, type, fields);
		if (cached != null) {
			return cached;
		}
		
		List<V> back = new ArrayList<V>();
		List<RawEntity<?>> throughValues = new ArrayList<RawEntity<?>>();
		
		String table = getManager().getTableNameConverter().getName(type);
		boolean oneToMany = type.equals(finalType);
		Preload preloadAnnotation = finalType.getAnnotation(Preload.class);
		
		Connection conn = getConnectionImpl();

		try {
			StringBuilder sql = new StringBuilder();
			String returnField;
			String throughField = null;
			int numParams = 0;
			
			if (oneToMany && inMapFields.length == 1 && outMapFields.length == 1 && preloadAnnotation != null) {
				sql.append("SELECT ");		// one-to-many preload
				
				sql.append(outMapFields[0]).append(',');
				for (String field : preloadAnnotation.value()) {
					sql.append(field).append(',');
				}
				sql.setLength(sql.length() - 1);
				
				sql.append(" FROM ").append(table);
				
				sql.append(" WHERE ").append(inMapFields[0]).append(" = ?");
				
				if (!where.trim().equals("")) {
					sql.append(" AND (").append(where).append(")");
				}
				
				numParams++;
				returnField = outMapFields[0];
			} else if (!oneToMany && inMapFields.length == 1 && outMapFields.length == 1 && preloadAnnotation != null) {
				String finalTable = getManager().getTableNameConverter().getName(finalType);		// many-to-many preload
				
				returnField = finalTable + "__aointernal__id";
				throughField = table + "__aointernal__id";
				
				sql.append("SELECT ");
				
				String finalPKField = Common.getPrimaryKeyField(finalType, getManager().getFieldNameConverter());
				
				sql.append(finalTable).append('.').append(finalPKField);
				sql.append(" AS ").append(returnField).append(',');
				sql.append(table).append('.').append(Common.getPrimaryKeyField(type, getManager().getFieldNameConverter()));
				sql.append(" AS ").append(throughField).append(',');
				for (String field : preloadAnnotation.value()) {
					sql.append(finalTable).append('.').append(field).append(',');
				}
				sql.setLength(sql.length() - 1);
				
				sql.append(" FROM ").append(table).append(" INNER JOIN ");
				sql.append(finalTable).append(" ON ");
				sql.append(table).append('.').append(outMapFields[0]);
				sql.append(" = ").append(finalTable).append('.').append(finalPKField);
				
				sql.append(" WHERE ").append(table).append('.').append(inMapFields[0]).append(" = ?");
				
				if (!where.trim().equals("")) {
					sql.append(" AND (").append(where).append(")");
				}
				
				numParams++;
			} else if (inMapFields.length == 1 && outMapFields.length == 1) {
				sql.append("SELECT ").append(outMapFields[0]).append(" FROM ").append(table);
				sql.append(" WHERE ").append(inMapFields[0]).append(" = ?");
				
				if (!where.trim().equals("")) {
					sql.append(" AND (").append(where).append(")");
				}
				
				numParams++;
				returnField = outMapFields[0];
			} else {
				sql.append("SELECT DISTINCT a.outMap AS outMap FROM (");
				returnField = "outMap";
				
				for (String outMap : outMapFields) {
					for (String inMap : inMapFields) {
						sql.append("SELECT ");
						sql.append(outMap);
						sql.append(" AS outMap,");
						sql.append(inMap);
						sql.append(" AS inMap FROM ");
						sql.append(table);
						sql.append(" WHERE ");
						sql.append(inMap).append(" = ?");
						
						if (!where.trim().equals("")) {
							sql.append(" AND (").append(where).append(")");
						}
						
						sql.append(" UNION ");

						numParams++;
					}
				}

				sql.setLength(sql.length() - " UNION ".length());
				sql.append(") a");
			}

			Logger.getLogger("net.java.ao").log(Level.INFO, sql.toString());
			PreparedStatement stmt = conn.prepareStatement(sql.toString());
			
			DatabaseType<K> dbType = (DatabaseType<K>) TypeManager.getInstance().getType(key.getClass());
			for (int i = 0; i < numParams; i++) {
				dbType.putToDatabase(i + 1, stmt, key);
			}

			dbType = Common.getPrimaryKeyType(finalType);
			DatabaseType<Object> throughDBType = Common.getPrimaryKeyType((Class<? extends RawEntity<Object>>) type);
			
			ResultSet res = stmt.executeQuery();
			while (res.next()) {
				K returnValue = dbType.convert(getManager(), res, (Class<? extends K>) type, returnField);
				
				if (finalType.equals(this.type) && returnValue.equals(key)) {
					continue;
				}
				
				if (throughField != null) {
					throughValues.add(getManager().get((Class<? extends RawEntity<Object>>) type, 
							throughDBType.convert(getManager(), res, type, throughField)));
				}

				V returnValueEntity = getManager().get(finalType, returnValue);
				ResultSetMetaData md = res.getMetaData();
				for (int i = 0; i < md.getColumnCount(); i++) {
					getManager().getProxyForEntity(returnValueEntity).addToCache(md.getColumnName(i + 1), res.getObject(i + 1));
				}

				back.add(returnValueEntity);
			}
			res.close();
			stmt.close();
		} finally {
			closeConnectionImpl(conn);
		}
		
		cached = back.toArray((V[]) Array.newInstance(finalType, back.size()));

		getManager().getRelationsCache().put(entity, 
				(throughValues.size() > 0 ? throughValues.toArray(new RawEntity[throughValues.size()]) : cached), cached, fields);	
		
		return cached;
	}
	
	private String[] getFields(String pkField, String[] inMapFields, String[] outMapFields, String where) {
		List<String> back = new ArrayList<String>();
		back.addAll(Arrays.asList(outMapFields));
		
		if (inMapFields != null && inMapFields.length > 0) {
			if (!inMapFields[0].trim().equalsIgnoreCase(pkField)) {
				back.addAll(Arrays.asList(inMapFields));
			}
		}
		
		Matcher matcher = WHERE_PATTERN.matcher(where);
		while (matcher.find()) {
			back.add(matcher.group(1));
		}
		
		return back.toArray(new String[back.size()]);
	}

	private <V> V convertValue(ResultSet res, String field, Class<V> type) throws SQLException {
		if (res.getString(field) == null) {
			return null;
		}
		
		TypeManager manager = TypeManager.getInstance();
		DatabaseType<V> databaseType = manager.getType(type);
		
		if (databaseType == null) {
			throw new RuntimeException("UnrecognizedType: " + type.toString());
		}
		
		return databaseType.convert(getManager(), res, type, field);
	}

	private boolean instanceOf(Object value, Class<?> type) {
		if (type.isPrimitive()) {
			if (type.equals(boolean.class)) {
				return instanceOf(value, Boolean.class);
			} else if (type.equals(char.class)) {
				return instanceOf(value, Character.class);
			} else if (type.equals(byte.class)) {
				return instanceOf(value, Byte.class);
			} else if (type.equals(short.class)) {
				return instanceOf(value, Short.class);
			} else if (type.equals(int.class)) {
				return instanceOf(value, Integer.class);
			} else if (type.equals(long.class)) {
				return instanceOf(value, Long.class);
			} else if (type.equals(float.class)) {
				return instanceOf(value, Float.class);
			} else if (type.equals(double.class)) {
				return instanceOf(value, Double.class);
			}
		} else {
			return type.isInstance(value);
		}

		return false;
	}
}
