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

import static net.java.ao.Common.convertDowncaseName;
import static net.java.ao.Common.getCallingClass;
import static net.java.ao.Common.getMappingFields;
import static net.java.ao.Common.interfaceInheritsFrom;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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

/**
 * @author Daniel Spiewak
 */
class EntityProxy<T extends Entity> implements InvocationHandler, Serializable {
	private final static Map<EntityProxy<?>, EntityManager> managers = Collections.synchronizedMap(
			new HashMap<EntityProxy<?>, EntityManager>());

	private int id;
	private Class<T> type;
	
	private Object implementation;
	
	private final transient Map<String, Object> cache;
	private final transient ReadWriteLock cacheLock = new ReentrantReadWriteLock();
	
	private final transient Set<String> dirtyFields;
	private final transient ReadWriteLock dirtyFieldsLock = new ReentrantReadWriteLock();
	
	private List<PropertyChangeListener> listeners;

	public EntityProxy(EntityManager manager, Class<T> type) {
		this.type = type;
		managers.put(this, manager);
		
		cache = new HashMap<String, Object>();
		dirtyFields = new LinkedHashSet<String>();
		
		listeners = new LinkedList<PropertyChangeListener>();
	}

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Object implementation = getImplementation((T) proxy);
		
		if (implementation != null) {
			if (getCallingClass(1) != implementation.getClass()) {
				try {
					Method implMethod = implementation.getClass().getMethod(method.getName(), method.getParameterTypes());
					
					return implMethod.invoke(implementation, args);
				} catch (Throwable t) {
				}
			}
		}
		
		if (method.getName().equals("setID")) {
			setID((Integer) args[0]);

			return Void.TYPE;
		} else if (method.getName().equals("getID")) {
			return getID();
		} else if (method.getName().equals("save")) {
			save();
			
			return Void.TYPE;
		} else if (method.getName().equals("getTableName")) {
			return getTableName();
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

		if (mutatorAnnotation != null) {
			invokeSetter(id, tableName, mutatorAnnotation.value(), args[0]);
			return Void.TYPE;
		} else if (accessorAnnotation != null) {
			return invokeGetter(getID(), tableName, accessorAnnotation.value(), method.getReturnType());
		} else if (oneToManyAnnotation != null && method.getReturnType().isArray() 
				&& interfaceInheritsFrom(method.getReturnType().getComponentType(), Entity.class)) {
			Class<? extends Entity> type = (Class<? extends Entity>) method.getReturnType().getComponentType();
			String otherTableName = getManager().getNameConverter().getName(type);
			
			return retrieveRelations(otherTableName, new String[] {"id"}, getID(), (Class<? extends Entity>) type);
		} else if (manyToManyAnnotation != null && method.getReturnType().isArray() 
				&& interfaceInheritsFrom(method.getReturnType().getComponentType(), Entity.class)) {
			Class<? extends Entity> throughType = manyToManyAnnotation.value();
			Class<? extends Entity> type = (Class<? extends Entity>) method.getReturnType().getComponentType();
			String otherTableName = getManager().getNameConverter().getName(throughType);
			
			return retrieveRelations(otherTableName, getMappingFields(throughType, type), getID(), throughType, type);
		} else if (method.getName().startsWith("get")) {
			String name = convertDowncaseName(method.getName().substring(3));
			if (interfaceInheritsFrom(method.getReturnType(), Entity.class)) {
				name += "ID";
			}
			
			return invokeGetter(getID(), tableName, name, method.getReturnType());
		} else if (method.getName().startsWith("is")) {
			String name = convertDowncaseName(method.getName().substring(2));
			if (interfaceInheritsFrom(method.getReturnType(), Entity.class)) {
				name += "ID";
			}
			
			return invokeGetter(getID(), tableName, name, method.getReturnType());
		} else if (method.getName().startsWith("set")) {
			String name = convertDowncaseName(method.getName().substring(3));
			if (interfaceInheritsFrom(method.getParameterTypes()[0], Entity.class)) {
				name += "ID";
			}
			invokeSetter(id, tableName, name, args[0]);
			
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
	
	public void save() throws SQLException {
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
					}
				}
				stmt.setInt(index++, id);

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
		cacheLock.writeLock().lock();
		try {
			if (!cache.containsKey(key)) {
				cache.put(key, value);
			}
		} finally {
			cacheLock.writeLock().unlock();
		}
	}
	
	Class<T> getType() {
		return type;
	}
	
	private EntityManager getManager() {
		return managers.get(this);
	}
	
	private Connection getConnectionImpl() throws SQLException {
		return DBEncapsulator.getInstance(getManager().getProvider()).getConnection();
	}
	
	private void closeConnectionImpl(Connection conn) throws SQLException {
		DBEncapsulator.getInstance(getManager().getProvider()).closeConnection(conn);
	}

	private <V> V invokeGetter(int id, String table, String name, Class<V> type) throws Throwable {
		V back = null;
		
		cacheLock.writeLock().lock();
		try {
			if (cache.containsKey(name)) {
				return (V) cache.get(name);
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
	
			if (back != null) {
				cache.put(name, back);
			}
		} finally {
			cacheLock.writeLock().unlock();
		}
		
		return back;
	}

	private void invokeSetter(int id, String table, String name, Object value) throws Throwable {
		boolean saveable = interfaceInheritsFrom(type, SaveableEntity.class);
		
		Object oldValue = invokeGetter(id, table, name, value.getClass());
		invokeSetterImpl(name, value, saveable);
		
		boolean veto = false;
		PropertyChangeEvent evt = new PropertyChangeEvent(this, name, oldValue, value);
		for (PropertyChangeListener l : listeners) {
			l.propertyChange(evt);
		}
		
		if (veto) {
			invokeSetterImpl(name, oldValue, saveable);
		}
		
		dirtyFieldsLock.writeLock().lock();
		try {
			dirtyFields.add(name);
		} finally {
			dirtyFieldsLock.writeLock().unlock();
		}
		
		if (!saveable) {
			save();
		}
	}
	
	private void invokeSetterImpl(String name, Object value, boolean saveable) throws Throwable {
		cacheLock.writeLock().lock();
		try {
			cache.put(name, value);
		} finally {
			cacheLock.writeLock().unlock();
		}
	}
	
	private <V extends Entity> V[] retrieveRelations(String table, String[] outMapFields, int id, Class<V> type) throws SQLException {
		return retrieveRelations(table, outMapFields, id, type, type);
	}
	
	private <V extends Entity> V[] retrieveRelations(String table, String[] outMapFields, int id, Class<? extends Entity> type, 
			Class<V> finalType) throws SQLException {
		List<V> back = new ArrayList<V>();
		Connection conn = getConnectionImpl();
		
		String[] inMapFields = getMappingFields(type, this.type);
		
		try {
			StringBuilder sql = new StringBuilder("SELECT DISTINCT a.outMap AS outMap FROM (");
			
			int numParams = 0;
			for (String outMap : outMapFields) {
				for (String inMap : inMapFields) {
					sql.append("SELECT ");
					sql.append(outMap);
					sql.append(" AS outMap,");
					sql.append(inMap);
					sql.append(" AS inMap FROM ");
					sql.append(table);
					sql.append(" WHERE ");
					sql.append(inMap);
					sql.append(" = ? UNION ");
					
					numParams++;
				}
			}
			
			sql.setLength(sql.length() - " UNION ".length());
			sql.append(") a");

			Logger.getLogger("net.java.ao").log(Level.INFO, sql.toString());
			PreparedStatement stmt = conn.prepareStatement(sql.toString());
			
			for (int i = 0; i < numParams; i++) {
				stmt.setInt(i + 1, id);
			}
			
			ResultSet res = stmt.executeQuery();
			while (res.next()) {
				if (finalType.equals(this.type) && res.getInt("outMap") == id) {
					continue;
				}
				
				back.add(getManager().get(finalType, res.getInt("outMap")));
			}
			res.close();
			stmt.close();
		} finally {
			closeConnectionImpl(conn);
		}
		
		return back.toArray((V[]) Array.newInstance(finalType, back.size()));
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
		} else if (type.equals(String.class)) {
			return (V) res.getString(field);
		} else if (type.equals(URL.class)) {
			try {
				return (V) new URL(res.getString(field));
			} catch (MalformedURLException e) {
				throw new SQLException(e);
			}
		} else if (type.equals(Calendar.class)) {
			Calendar back = Calendar.getInstance();
			back.setTimeInMillis(res.getTimestamp(field).getTime());

			return (V) back;
		} else if (type.equals(Date.class)) {
			return (V) new Date(res.getTimestamp(field).getTime());
		} else if (type.equals(URL.class)) {
			return (V) res.getURL(field);
		} else if (type.equals(InputStream.class)) {
			return (V) res.getBlob(field).getBinaryStream();
		} else if (interfaceInheritsFrom(type, Entity.class)) {
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
		} else {
			throw new RuntimeException("Unrecognized type: " + value.getClass().toString());
		}
	}
	
	private Object getImplementation(T proxy) {
		if (implementation == Void.TYPE) {
			return null;
		} else if (implementation != null) {
			return implementation;
		}
		
		try {
			Implementation impl = type.getAnnotation(Implementation.class);
			if (impl == null) {
				implementation = Void.TYPE;
			} else {
				Constructor<?> constructor = impl.value().getConstructor(type);
				constructor.setAccessible(true);
				
				implementation = constructor.newInstance(proxy);
				
				return implementation;
			}
		} catch (SecurityException e) {
			implementation = Void.TYPE;
		} catch (IllegalArgumentException e) {
			implementation = Void.TYPE;
		} catch (IllegalAccessException e) {
			implementation = Void.TYPE;
		} catch (NoSuchMethodException e) {
			implementation = Void.TYPE;
		} catch (InstantiationException e) {
			implementation = Void.TYPE;
		} catch (InvocationTargetException e) {
			implementation = Void.TYPE;
		}
		
		return null;
	}
	
	// special call from ObjectOutputStream
	private void writeObject(ObjectOutputStream oos) throws IOException {
		try {
			save();
		} catch (SQLException e) {
			throw new IOException(e);
		}
		
		oos.defaultWriteObject();
	}
}
