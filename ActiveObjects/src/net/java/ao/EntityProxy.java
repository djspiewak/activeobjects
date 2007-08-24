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
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.ao.schema.OnUpdate;

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

	public EntityProxy(EntityManager manager, Class<T> type) {
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

		if (method.getName().equals("setID")) {
			setID((Integer) args[0]);

			return Void.TYPE;
		} else if (method.getName().equals("getID")) {
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

		String tableName = getManager().getNameConverter().getName(type);

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
		} else if (method.getName().startsWith("get")) {
			String name = Common.convertDowncaseName(method.getName().substring(3));
			if (Common.interfaceInheritsFrom(method.getReturnType(), Entity.class)) {
				name += "ID";
			}

			return invokeGetter(getID(), tableName, name, method.getReturnType(), onUpdateAnnotation == null);
		} else if (method.getName().startsWith("is")) {
			String name = Common.convertDowncaseName(method.getName().substring(2));
			if (Common.interfaceInheritsFrom(method.getReturnType(), Entity.class)) {
				name += "ID";
			}

			return invokeGetter(getID(), tableName, name, method.getReturnType(), onUpdateAnnotation == null);
		} else if (method.getName().startsWith("set")) {
			String name = Common.convertDowncaseName(method.getName().substring(3));
			if (Common.interfaceInheritsFrom(method.getParameterTypes()[0], Entity.class)) {
				name += "ID";
			}
			invokeSetter((T) proxy, name, args[0], onUpdateAnnotation == null);

			return Void.TYPE;
		}

		return null;
	}

	public int getID() {
		return id;
	}

	public String getTableName() {
		return getManager().getNameConverter().getName(type);
	}

	public void setID(int id) {
		this.id = id;
	}

	public void save(Entity entity) throws SQLException {
		dirtyFieldsLock.writeLock().lock();
		try {
			if (dirtyFields.isEmpty()) {
				return;
			}

			String table = getTableName();
			Connection conn = getConnectionImpl();

			cacheLock.readLock().lock();
			try {
				StringBuilder sql = new StringBuilder("UPDATE " + table + " SET ");

				for (String field : dirtyFields) {
					sql.append(field);

					if (cache.containsKey(field)) {
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
					if (cache.containsKey(field)) {
						convertValue(stmt, index++, cache.get(field));
					} else if (nullSet.contains(field)) {
						stmt.setString(index++, null);
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
				
				getManager().getRelationsCache().remove(entity, type, dirtyFields.toArray(new String[dirtyFields.size()]));
				
				stmt.executeUpdate();

				dirtyFields.removeAll(dirtyFields);

				stmt.close();
			} finally {
				getManager().getRelationsCache().unlock();
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
		return (int) (new Random(getID()).nextFloat() * getID()) + getID() % (2 << 15);
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
		return getManager().getNameConverter().getName(type) + " {id = " + getID() + "}";
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
				nullSet.add(key);
			} else if (!cache.containsKey(key)) {
				cache.put(key, value);
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
				if (!dirtyFields.contains(fieldName)) {
					cache.remove(fieldName);
					nullSet.remove(fieldName);
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
		return DBEncapsulator.getInstance(getManager().getProvider()).getConnection();
	}

	private void closeConnectionImpl(Connection conn) throws SQLException {
		DBEncapsulator.getInstance(getManager().getProvider()).closeConnection(conn);
	}

	private <V> V invokeGetter(int id, String table, String name, Class<V> type, boolean shouldCache) throws Throwable {
		V back = null;

		if (shouldCache) {
			cacheLock.writeLock().lock();
		}
		try {
			if (shouldCache && nullSet.contains(name)) {
				return null;
			} else if (shouldCache && cache.containsKey(name)) {
				Object value = cache.get(name);

				if (instanceOf(value, type)) {
					return (V) value;
				} else if (Common.interfaceInheritsFrom(type, Entity.class) && value instanceof Integer) {
					value = getManager().get((Class<? extends Entity>) type, (Integer) value);

					cache.put(name, value);
					return (V) value;
				} else {
					cache.remove(name); // invalid cached value
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
				cache.put(name, back);
				
				if (back == null) {
					nullSet.add(name);
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
			if (cache.containsKey(name)) {
				oldValue = cache.get(name);
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
			dirtyFields.add(name);
		} finally {
			dirtyFieldsLock.writeLock().unlock();
		}
	}

	private void invokeSetterImpl(String name, Object value) throws Throwable {
		cacheLock.writeLock().lock();
		try {
			cache.put(name, value);
			
			if (value != null) {
				nullSet.remove(name);
			} else {
				nullSet.add(name);
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
		String[] fields = getFields(outMapFields, where);
		V[] cached = getManager().getRelationsCache().get(entity, finalType, fields);
		
		if (cached != null && where.trim().equals("")) {
			return cached;
		}
		
		List<V> back = new ArrayList<V>();
		String table = getManager().getNameConverter().getName(type);
		
		Connection conn = getConnectionImpl();

		if (inMapFields == null || inMapFields.length == 0) {
			inMapFields = Common.getMappingFields(type, this.type);
		}

		try {
			StringBuilder sql = new StringBuilder();
			String returnField;
			int numParams = 0;
			
			if (inMapFields.length == 1 && outMapFields.length == 1) {
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

				back.add(getManager().get(finalType, returnValue));
			}
			res.close();
			stmt.close();
		} finally {
			closeConnectionImpl(conn);
		}
		
		cached = back.toArray((V[]) Array.newInstance(finalType, back.size()));
		
		if (type.equals(finalType)) {		// only cache one-to-many
			getManager().getRelationsCache().put(entity, cached, fields);
		}
		
		return cached;
	}
	
	private String[] getFields(String[] mapFields, String where) {
		List<String> back = new ArrayList<String>();
		back.addAll(Arrays.asList(mapFields));
		
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

		if (type.equals(Integer.class) || type.equals(int.class)) {
			return (V) new Integer(res.getInt(field));
		} else if (type.equals(Long.class) || type.equals(long.class)) {
			return (V) new Long(res.getLong(field));
		} else if (type.equals(Short.class) || type.equals(short.class)) {
			return (V) new Short(res.getShort(field));
		} else if (type.equals(Float.class) || type.equals(float.class)) {
			return (V) new Float(res.getFloat(field));
		} else if (type.equals(Double.class) || type.equals(double.class)) {
			return (V) new Double(res.getDouble(field));
		} else if (type.equals(Byte.class) || type.equals(byte.class)) {
			return (V) new Byte(res.getByte(field));
		} else if (type.equals(Boolean.class) || type.equals(boolean.class)) {
			return (V) new Boolean(res.getBoolean(field));
		} else if (type.equals(String.class)) {
			return (V) res.getString(field);
		} else if (type.equals(URL.class)) {
			try {
				return (V) new URL(res.getString(field));
			} catch (MalformedURLException e) {
				throw (SQLException) new SQLException().initCause(e);
			}
		} else if (Common.typeInstanceOf(type, Calendar.class)) {
			Calendar back = Calendar.getInstance();
			back.setTimeInMillis(res.getTimestamp(field).getTime());

			return (V) back;
		} else if (Common.typeInstanceOf(type, Date.class)) {
			return (V) new Date(res.getTimestamp(field).getTime());
		} else if (type.equals(URL.class)) {
			return (V) res.getURL(field);
		} else if (type.equals(InputStream.class)) {
			return (V) res.getBlob(field).getBinaryStream();
		} else if (Common.interfaceInheritsFrom(type, Entity.class)) {
			return (V) getManager().get((Class<? extends Entity>) type, res.getInt(field));
		} else {
			throw new RuntimeException("Unrecognized type: " + type.toString());
		}
	}

	private void convertValue(PreparedStatement stmt, int index, Object value) throws SQLException {
		if (value instanceof Integer) {
			stmt.setInt(index, (Integer) value);
		} else if (value instanceof Long) {
			stmt.setLong(index, (Long) value);
		} else if (value instanceof Short) {
			stmt.setShort(index, (Short) value);
		} else if (value instanceof Float) {
			stmt.setFloat(index, (Float) value);
		} else if (value instanceof Double) {
			stmt.setDouble(index, (Double) value);
		} else if (value instanceof Byte) {
			stmt.setByte(index, (Byte) value);
		} else if (value instanceof Boolean) {
			stmt.setBoolean(index, (Boolean) value);
		} else if (value instanceof String) {
			stmt.setString(index, (String) value);
		} else if (value instanceof URL) {
			stmt.setString(index, value.toString());
		} else if (value instanceof Calendar) {
			stmt.setTimestamp(index, new Timestamp(((Calendar) value).getTimeInMillis()));
		} else if (value instanceof Date) {
			stmt.setTimestamp(index, new Timestamp(((Date) value).getTime()));
		} else if (value instanceof Entity) {
			stmt.setInt(index, ((Entity) value).getID());
		} else if (value == null) {
			stmt.setString(index, null);
		} else {
			throw new RuntimeException("Unrecognized type: " + value.getClass().toString());
		}
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
