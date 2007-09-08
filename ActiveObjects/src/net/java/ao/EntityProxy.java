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
class EntityProxy<T extends Entity> implements InvocationHandler {
	private static final Pattern WHERE_PATTERN = Pattern.compile("([\\d\\w]+)\\s*(=|>|<|LIKE|IS)");
	
	private int id;
	private Class<T> type;

	private EntityManager manager;

	private ImplementationWrapper<T> implementation;

	private final Map<String, Object> cache;
	private final Set<String> nullSet;
	private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();

	private final Set<String> dirtyFields;
	private final ReadWriteLock dirtyFieldsLock = new ReentrantReadWriteLock();
	
	private final Set<Class<? extends Entity>> toFlushRelations = new HashSet<Class<? extends Entity>>();
	private final ReadWriteLock toFlushLock = new ReentrantReadWriteLock();

	private List<PropertyChangeListener> listeners;

	public EntityProxy(EntityManager manager, Class<T> type, int id) {
		this.id = id;
		this.type = type;
		this.manager = manager;

		cache = new HashMap<String, Object>();
		nullSet = new HashSet<String>();
		dirtyFields = new LinkedHashSet<String>();

		listeners = new LinkedList<PropertyChangeListener>();
	}

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (method.getName().equals("getEntityType")) {
			return type;
		}

		if (implementation == null) {
			implementation = new ImplementationWrapper<T>((T) proxy);
		}

		MethodImplWrapper methodImpl = implementation.getMethod(method.getName(), method.getParameterTypes());
		if (methodImpl != null) {
			if (!Common.getCallingClass(1).equals(methodImpl.getMethod().getDeclaringClass()) && !methodImpl.getMethod().getDeclaringClass().equals(Object.class)) {
				return methodImpl.getMethod().invoke(methodImpl.getInstance(), args);
			}
		}

		if (method.getName().equals("getID")) {
			return getID();
		} else if (method.getName().equals("save")) {
			save((Entity) proxy);

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
			return equalsImpl((Entity) proxy, args[0]);
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
			return invokeGetter(getID(), tableName, accessorAnnotation.value(), method.getReturnType(), onUpdateAnnotation != null);
		} else if (oneToManyAnnotation != null && method.getReturnType().isArray() 
				&& Common.interfaceInheritsFrom(method.getReturnType().getComponentType(), Entity.class)) {
			Class<? extends Entity> type = (Class<? extends Entity>) method.getReturnType().getComponentType();

			return retrieveRelations((Entity) proxy, new String[0], new String[] { "id" }, (Class<? extends Entity>) type, oneToManyAnnotation.where());
		} else if (manyToManyAnnotation != null && method.getReturnType().isArray() 
				&& Common.interfaceInheritsFrom(method.getReturnType().getComponentType(), Entity.class)) {
			Class<? extends Entity> throughType = manyToManyAnnotation.value();
			Class<? extends Entity> type = (Class<? extends Entity>) method.getReturnType().getComponentType();

			return retrieveRelations((Entity) proxy, null, Common.getMappingFields(throughType, type), throughType, type, manyToManyAnnotation.where());
		} else if (Common.isAccessor(method)) {
			return invokeGetter(getID(), tableName, getManager().getFieldNameConverter().getName(type, method), 
					method.getReturnType(), onUpdateAnnotation == null);
		} else if (Common.isMutator(method)) {
			invokeSetter((T) proxy, getManager().getFieldNameConverter().getName(type, method), args[0], onUpdateAnnotation == null);

			return Void.TYPE;
		}

		return null;
	}

	public int getID() {
		return id;
	}

	public String getTableName() {
		return getManager().getTableNameConverter().getName(type);
	}

	@SuppressWarnings("unchecked")
	public void save(Entity entity) throws SQLException {
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

				sql.append(" WHERE id = ?");

				Logger.getLogger("net.java.ao").log(Level.INFO, sql.toString());
				PreparedStatement stmt = conn.prepareStatement(sql.toString());

				int index = 1;
				for (String field : dirtyFields) {
					if (nullSet.contains(field.toLowerCase())) {
						stmt.setString(index++, null);
					} else if (cache.containsKey(field.toLowerCase())) {
						Object obj = cache.get(field.toLowerCase());
						Class javaType = obj.getClass();
						
						if (obj instanceof Entity) {
							javaType = ((Entity) obj).getEntityType();
						}
						
						manager.getType(javaType).putToDatabase(index++, stmt, obj);
					}
				}
				stmt.setInt(index++, id);
				
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
		return (id + type.hashCode()) % (2 << 10);
	}

	public boolean equalsImpl(Entity proxy, Object obj) {
		if (proxy == obj) {
			return true;
		}

		if (obj instanceof Entity) {
			Entity entity = (Entity) obj;

			return entity.getID() == proxy.getID() && entity.getTableName().equals(proxy.getTableName());
		}

		return false;
	}

	public String toStringImpl() {
		return getManager().getTableNameConverter().getName(type) + " {id = " + getID() + "}";
	}

	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		if (obj instanceof EntityProxy) {
			EntityProxy<?> proxy = (EntityProxy<?>) obj;

			if (proxy.type.equals(type) && proxy.id == id) {
				return true;
			}
		}

		return false;
	}

	public int hashCode() {
		return type.hashCode();
	}

	void addToCache(String key, Object value) {
		if (key.trim().equalsIgnoreCase("id")) {
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

	private <V> V invokeGetter(int id, String table, String name, Class<V> type, boolean shouldCache) throws Throwable {
		V back = null;

		if (shouldCache) {
			cacheLock.writeLock().lock();
		}
		try {
			if (shouldCache && nullSet.contains(name.toLowerCase())) {
				return null;
			} else if (shouldCache && cache.containsKey(name.toLowerCase())) {
				Object value = cache.get(name.toLowerCase());

				if (instanceOf(value, type)) {
					return (V) value;
				} else if (Common.interfaceInheritsFrom(type, Entity.class) && value instanceof Integer) {
					value = getManager().get((Class<? extends Entity>) type, (Integer) value);

					cache.put(name.toLowerCase(), value);
					return (V) value;
				} else {
					cache.remove(name.toLowerCase()); // invalid cached value
				}
			}

			Connection conn = getConnectionImpl();

			try {
				String sql = "SELECT " + name + " FROM " + table + " WHERE id = ?";

				Logger.getLogger("net.java.ao").log(Level.INFO, sql);
				PreparedStatement stmt = conn.prepareStatement(sql);
				stmt.setInt(1, id);

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
		
		if (value instanceof Entity) {
			toFlushLock.writeLock().lock();
			try {
				toFlushRelations.add(((Entity) value).getEntityType());
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

	private <V extends Entity> V[] retrieveRelations(Entity entity, String[] inMapFields, String[] outMapFields, Class<V> type, String where) throws SQLException {
		return retrieveRelations(entity, inMapFields, outMapFields, type, type, where);
	}

	private <V extends Entity> V[] retrieveRelations(Entity entity, String[] inMapFields, String[] outMapFields, Class<? extends Entity> type, 
			Class<V> finalType, String where) throws SQLException {
		String[] fields = getFields(inMapFields, outMapFields, where);
		V[] cached = getManager().getRelationsCache().get(entity, finalType, type, fields);
		
		if (cached != null) {
			return cached;
		}
		
		List<V> back = new ArrayList<V>();
		String table = getManager().getTableNameConverter().getName(type);
		boolean oneToMany = type.equals(finalType);
		Preload preloadAnnotation = finalType.getAnnotation(Preload.class);
		
		Connection conn = getConnectionImpl();

		if (inMapFields == null || inMapFields.length == 0) {
			inMapFields = Common.getMappingFields(type, this.type);
		}

		try {
			StringBuilder sql = new StringBuilder();
			String returnField;
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
				
				sql.append("SELECT ");
				
				sql.append(finalTable).append(".id,");
				for (String field : preloadAnnotation.value()) {
					sql.append(finalTable).append('.').append(field).append(',');
				}
				sql.setLength(sql.length() - 1);
				
				sql.append(" FROM ").append(table).append(" INNER JOIN ");
				sql.append(finalTable).append(" ON ");
				sql.append(table).append('.').append(outMapFields[0]);
				sql.append(" = ").append(finalTable).append(".id");
				
				sql.append(" WHERE ").append(table).append('.').append(inMapFields[0]).append(" = ?");
				
				if (!where.trim().equals("")) {
					sql.append(" AND (").append(where).append(")");
				}
				
				numParams++;
				returnField = "id";
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

			for (int i = 0; i < numParams; i++) {
				stmt.setInt(i + 1, id);
			}

			ResultSet res = stmt.executeQuery();
			while (res.next()) {
				int returnValue = res.getInt(returnField);
				
				if (finalType.equals(this.type) && returnValue == id) {
					continue;
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
		getManager().getRelationsCache().put(entity, cached, fields, type);
		
		return cached;
	}
	
	private String[] getFields(String[] inMapFields, String[] outMapFields, String where) {
		List<String> back = new ArrayList<String>();
		back.addAll(Arrays.asList(outMapFields));
		
		if (inMapFields != null && inMapFields.length > 0) {
			if (!inMapFields[0].trim().equalsIgnoreCase("id")) {
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
