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

import net.java.ao.cache.CacheLayer;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.OnUpdate;
import net.java.ao.types.DatabaseType;
import net.java.ao.types.TypeManager;

/**
 * @author Daniel Spiewak
 */
class EntityProxy<T extends RawEntity<K>, K> implements InvocationHandler {
	private static final Pattern WHERE_PATTERN = Pattern.compile("([\\d\\w]+)\\s*(=|>|<|LIKE|IS)");
	
	static boolean ignorePreload = false;	// hack for testing
	
	private K key;
	private Method pkAccessor;
	private String pkFieldName;
	private Class<T> type;

	private EntityManager manager;
	
	private Map<String, ReadWriteLock> locks;
	private final ReadWriteLock locksLock = new ReentrantReadWriteLock();
	
	private ImplementationWrapper<T> implementation;
	private List<PropertyChangeListener> listeners;

	public EntityProxy(EntityManager manager, Class<T> type, K key) {
		this.key = key;
		this.type = type;
		this.manager = manager;
		
		pkAccessor = Common.getPrimaryKeyAccessor(type);
		pkFieldName = Common.getPrimaryKeyField(type, getManager().getFieldNameConverter());
		
		locks = new HashMap<String, ReadWriteLock>();

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
			Class<?> callingClass = Common.getCallingClass(1);
			if ((callingClass == null || !callingClass.equals(methodImpl.getMethod().getDeclaringClass())) 
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
		
		checkConstraints(method, args);

		String tableName = getManager().getTableNameConverter().getName(type);

		Class<?> attributeType = Common.getAttributeTypeFromMethod(method);
		String polyFieldName = null;
		
		if (attributeType != null) {
			polyFieldName = (attributeType.getAnnotation(Polymorphic.class) == null ? null : 
				getManager().getFieldNameConverter().getPolyTypeName(method));
		}
		
		Mutator mutatorAnnotation = method.getAnnotation(Mutator.class);
		Accessor accessorAnnotation = method.getAnnotation(Accessor.class);
		OneToOne oneToOneAnnotation = method.getAnnotation(OneToOne.class);
		OneToMany oneToManyAnnotation = method.getAnnotation(OneToMany.class);
		ManyToMany manyToManyAnnotation = method.getAnnotation(ManyToMany.class);
		
		AnnotationDelegate annotations = Common.getAnnotationDelegate(getManager().getFieldNameConverter(), method);
		
		OnUpdate onUpdateAnnotation = annotations.getAnnotation(OnUpdate.class);
		Transient transientAnnotation = annotations.getAnnotation(Transient.class);

		// check annotations first, they trump all
		if (mutatorAnnotation != null) {
			invokeSetter((T) proxy, mutatorAnnotation.value(), args[0], onUpdateAnnotation != null, polyFieldName);
			return Void.TYPE;
		} else if (accessorAnnotation != null) {
			return invokeGetter(getKey(), tableName, accessorAnnotation.value(), polyFieldName, 
					method.getReturnType(), onUpdateAnnotation == null && transientAnnotation == null);
		} else if (oneToOneAnnotation != null && Common.interfaceInheritsFrom(method.getReturnType(), RawEntity.class)) {
			Class<? extends RawEntity<?>> type = (Class<? extends RawEntity<?>>) method.getReturnType();

			Object[] back = retrieveRelations((RawEntity<K>) proxy, new String[0], 
					new String[] { Common.getPrimaryKeyField(type, getManager().getFieldNameConverter()) }, 
					(Class<? extends RawEntity>) type, oneToOneAnnotation.where(), 
					Common.getPolymorphicFieldNames(getManager().getFieldNameConverter(), type, this.type));
			
			return back.length == 0 ? null : back[0];
		} else if (oneToManyAnnotation != null && method.getReturnType().isArray() 
				&& Common.interfaceInheritsFrom(method.getReturnType().getComponentType(), RawEntity.class)) {
			Class<? extends RawEntity<?>> type = (Class<? extends RawEntity<?>>) method.getReturnType().getComponentType();

			return retrieveRelations((RawEntity<K>) proxy, new String[0], 
					new String[] { Common.getPrimaryKeyField(type, getManager().getFieldNameConverter()) }, 
					(Class<? extends RawEntity>) type, oneToManyAnnotation.where(), 
					Common.getPolymorphicFieldNames(getManager().getFieldNameConverter(), type, this.type));
		} else if (manyToManyAnnotation != null && method.getReturnType().isArray() 
				&& Common.interfaceInheritsFrom(method.getReturnType().getComponentType(), RawEntity.class)) {
			Class<? extends RawEntity<?>> throughType = manyToManyAnnotation.value();
			Class<? extends RawEntity<?>> type = (Class<? extends RawEntity<?>>) method.getReturnType().getComponentType();

			return retrieveRelations((RawEntity<K>) proxy, null, 
					Common.getMappingFields(getManager().getFieldNameConverter(), 
							throughType, type), throughType, (Class<? extends RawEntity>) type, 
							manyToManyAnnotation.where(),
							Common.getPolymorphicFieldNames(getManager().getFieldNameConverter(), throughType, this.type), 
							Common.getPolymorphicFieldNames(getManager().getFieldNameConverter(), throughType, type));
		} else if (Common.isAccessor(method)) {
			return invokeGetter(getKey(), tableName, getManager().getFieldNameConverter().getName(method), 
					polyFieldName, method.getReturnType(), onUpdateAnnotation == null && transientAnnotation == null);
		} else if (Common.isMutator(method)) {
			invokeSetter((T) proxy, getManager().getFieldNameConverter().getName(method), args[0], 
					onUpdateAnnotation == null, polyFieldName);

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
		CacheLayer cacheLayer = getCacheLayer();
		String[] dirtyFields = cacheLayer.getDirtyFields();
		
		if (dirtyFields.length == 0) {
			return;
		}

		String table = getTableName();
		TypeManager manager = TypeManager.getInstance();
		Connection conn = getConnectionImpl();

		try {
			StringBuilder sql = new StringBuilder("UPDATE " + table + " SET ");

			for (String field : dirtyFields) {
				sql.append(field);

				if (cacheLayer.contains(field)) {
					sql.append(" = ?,");
				} else {
					sql.append(" = NULL,");
				}
			}
			
			if (sql.charAt(sql.length() - 1) == ',') {
				sql.setLength(sql.length() - 1);
			}

			sql.append(" WHERE ").append(pkFieldName).append(" = ?");

			Logger.getLogger("net.java.ao").log(Level.INFO, sql.toString());
			PreparedStatement stmt = conn.prepareStatement(sql.toString());

			int index = 1;
			for (String field : dirtyFields) {
				if (!cacheLayer.contains(field)) {
					continue;
				}
				
				Object value = cacheLayer.get(field);
				
				if (value == null) {
					getManager().getProvider().putNull(stmt, index++);
				} else {
					Class javaType = value.getClass();

					if (value instanceof RawEntity) {
						javaType = ((RawEntity) value).getEntityType();
					}

					DatabaseType dbType = manager.getType(javaType);
					dbType.putToDatabase(index++, stmt, value);
					
					// this check is not comprehensive, will miss @Transient fields
					if (!dbType.shouldCache(javaType)) {
						cacheLayer.remove(field);
					}
				}
			}
			((DatabaseType) Common.getPrimaryKeyType(type)).putToDatabase(index++, stmt, key);

			getManager().getRelationsCache().remove(cacheLayer.getToFlush());
			cacheLayer.clearFlush();

			getManager().getRelationsCache().remove(entity, dirtyFields);

			stmt.executeUpdate();

			cacheLayer.clearDirty();

			stmt.close();
		} finally {
			closeConnectionImpl(conn);
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

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		if (obj instanceof EntityProxy) {
			EntityProxy<?, ?> proxy = (EntityProxy<?, ?>) obj;

			if (proxy.type.equals(type) && proxy.key.equals(key)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public int hashCode() {
		return hashCodeImpl();
	}

	CacheLayer getCacheLayer() {
		return getManager().getCache().getCacheLayer(getManager().get(type, key));
	}

	Class<T> getType() {
		return type;
	}

	// any dirty fields are kept in the cache, since they have yet to be saved
	void flushCache() {
		getCacheLayer().clear();
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
	
	private ReadWriteLock getLock(String field) {
		locksLock.writeLock().lock();
		try {
			if (locks.containsKey(field)) {
				return locks.get(field);
			}
			
			ReentrantReadWriteLock back = new ReentrantReadWriteLock();
			locks.put(field, back);
			
			return back;
		} finally {
			locksLock.writeLock().unlock();
		}
	}

	private <V> V invokeGetter(K key, String table, String name, String polyName, Class<V> type, 
			boolean shouldCache) throws Throwable {
		V back = null;
		CacheLayer cacheLayer = getCacheLayer();
		
		shouldCache = shouldCache && TypeManager.getInstance().getType(type).shouldCache(type);
		
		getLock(name).writeLock().lock();
		try {
			if (!shouldCache && cacheLayer.dirtyContains(name)) {
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
			} else if (shouldCache && cacheLayer.contains(name)) {
				Object value = cacheLayer.get(name);
	
				if (instanceOf(value, type)) {
					return (V) value;
				} else if (Common.interfaceInheritsFrom(type, RawEntity.class) 
						&& instanceOf(value, Common.getPrimaryKeyClassType((Class<? extends RawEntity<K>>) type))) {
					value = getManager().get((Class<? extends RawEntity<Object>>) type, value);
	
					cacheLayer.put(name, value);
					return (V) value;
				} else {
					cacheLayer.remove(name); // invalid cached value
				}
			}
			
			Connection conn = getConnectionImpl();
	
			try {
				StringBuilder sql = new StringBuilder("SELECT ");
	
				sql.append(name);
				if (polyName != null) {
					sql.append(',').append(polyName);
				}
	
				sql.append(" FROM ").append(table).append(" WHERE ");
				sql.append(pkFieldName).append(" = ?");
	
				Logger.getLogger("net.java.ao").log(Level.INFO, sql.toString());
				PreparedStatement stmt = conn.prepareStatement(sql.toString());
				Common.getPrimaryKeyType(this.type).putToDatabase(1, stmt, key);
	
				ResultSet res = stmt.executeQuery();
				if (res.next()) {
					back = convertValue(res, name, polyName, type);
				}
				res.close();
				stmt.close();
			} finally {
				closeConnectionImpl(conn);
			}
	
			if (shouldCache) {
				cacheLayer.put(name, back);
			}
	
			return back;
		} finally {
			getLock(name).writeLock().unlock();
		}
	}

	private void invokeSetter(T entity, String name, Object value, boolean shouldCache, String polyName) throws Throwable {
		Object oldValue = null;
		CacheLayer cacheLayer = getCacheLayer();
		
		getLock(name).writeLock().lock();
		try {
			if (cacheLayer.contains(name)) {
				oldValue = cacheLayer.get(name);
			}
			
			if (value instanceof RawEntity) {
				cacheLayer.markToFlush(((RawEntity<?>) value).getEntityType());
			}

			cacheLayer.markDirty(name);
			cacheLayer.put(name, value);
	
			if (polyName != null) {
				String strValue = null;
	
				if (value != null) {
					strValue = getManager().getPolymorphicTypeMapper().convert(((RawEntity<?>) value).getEntityType());
				}

				cacheLayer.markDirty(polyName);
				cacheLayer.put(polyName, strValue);
			}
		} finally {
			getLock(name).writeLock().unlock();
		}

		PropertyChangeEvent evt = new PropertyChangeEvent(entity, name, oldValue, value);
		for (PropertyChangeListener l : listeners) {
			l.propertyChange(evt);
		}
	}

	private <V extends RawEntity<K>> V[] retrieveRelations(RawEntity<K> entity, String[] inMapFields, 
			String[] outMapFields, Class<V> type, String where, String[] thisPolyNames) throws SQLException {
		return retrieveRelations(entity, inMapFields, outMapFields, type, type, where, thisPolyNames, null);
	}

	private <V extends RawEntity<K>> V[] retrieveRelations(RawEntity<K> entity, String[] inMapFields, 
			String[] outMapFields, Class<? extends RawEntity<?>> type, Class<V> finalType, String where, 
					String[] thisPolyNames, String[] thatPolyNames) throws SQLException {
		if (inMapFields == null || inMapFields.length == 0) {
			inMapFields = Common.getMappingFields(getManager().getFieldNameConverter(), type, this.type);
		}
		String[] fields = getFields(Common.getPrimaryKeyField(finalType, getManager().getFieldNameConverter()), 
				inMapFields, outMapFields, where);
		
		V[] cached = getManager().getRelationsCache().get(entity, finalType, type, fields);
		if (cached != null) {
			return cached;
		}
		
		List<V> back = new ArrayList<V>();
		List<RawEntity<?>> throughValues = new ArrayList<RawEntity<?>>();
		List<String> resPolyNames = new ArrayList<String>(thatPolyNames == null ? 0 : thatPolyNames.length);
		
		String table = getManager().getTableNameConverter().getName(type);
		boolean oneToMany = type.equals(finalType);
		Preload preloadAnnotation = finalType.getAnnotation(Preload.class);
		
		Connection conn = getConnectionImpl();

		try {
			StringBuilder sql = new StringBuilder();
			String returnField;
			String throughField = null;
			int numParams = 0;
			
			if (oneToMany && inMapFields.length == 1 && outMapFields.length == 1 
					&& preloadAnnotation != null && !ignorePreload) {
				sql.append("SELECT ");		// one-to-many preload
				
				Set<String> selectFields = new LinkedHashSet<String>();
				selectFields.add(outMapFields[0]);
				selectFields.addAll(Arrays.asList(preloadAnnotation.value()));
				
				if (selectFields.contains("*")) {
					sql.append('*');
				} else {
					for (String field : selectFields) {
						sql.append(field).append(',');
					}
					sql.setLength(sql.length() - 1);
				}
				
				sql.append(" FROM ").append(table);
				
				sql.append(" WHERE ").append(inMapFields[0]).append(" = ?");
				
				if (!where.trim().equals("")) {
					sql.append(" AND (").append(where).append(")");
				}
				
				if (thisPolyNames != null) {
					for (String name : thisPolyNames) {
						sql.append(" AND ").append(name).append(" = ?");
					}
				}
				
				numParams++;
				returnField = outMapFields[0];
			} else if (!oneToMany && inMapFields.length == 1 && outMapFields.length == 1 
					&& preloadAnnotation != null && !ignorePreload) {
				String finalTable = getManager().getTableNameConverter().getName(finalType);		// many-to-many preload
				
				returnField = finalTable + "__aointernal__id";
				throughField = table + "__aointernal__id";
				
				sql.append("SELECT ");
				
				String finalPKField = Common.getPrimaryKeyField(finalType, getManager().getFieldNameConverter());
				
				Set<String> selectFields = new LinkedHashSet<String>();
				selectFields.add(finalPKField);
				selectFields.addAll(Arrays.asList(preloadAnnotation.value()));
				
				if (selectFields.contains("*")) {
					returnField = finalPKField;
				} else {
					sql.append(finalTable).append('.').append(finalPKField);
					sql.append(" AS ").append(returnField).append(',');
					
					selectFields.remove(finalPKField);
				}
				
				sql.append(table).append('.').append(Common.getPrimaryKeyField(type, getManager().getFieldNameConverter()));
				sql.append(" AS ").append(throughField).append(',');
				
				for (String field : selectFields) {
					sql.append(finalTable).append('.').append(field).append(',');
				}
				sql.setLength(sql.length() - 1);
				
				if (thatPolyNames != null) {
					for (String name : thatPolyNames) {
						String toAppend = table + '.' + name;
						
						resPolyNames.add(toAppend);
						sql.append(',').append(toAppend);
					}
				}
				
				sql.append(" FROM ").append(table).append(" INNER JOIN ");
				sql.append(finalTable).append(" ON ");
				sql.append(table).append('.').append(outMapFields[0]);
				sql.append(" = ").append(finalTable).append('.').append(finalPKField);
				
				sql.append(" WHERE ").append(table).append('.').append(inMapFields[0]).append(" = ?");
				
				if (!where.trim().equals("")) {
					sql.append(" AND (").append(where).append(")");
				}
				
				if (thisPolyNames != null) {
					for (String name : thisPolyNames) {
						sql.append(" AND ").append(name).append(" = ?");
					}
				}

				numParams++;
			} else if (inMapFields.length == 1 && outMapFields.length == 1) {	// 99% case (1-* & *-*)
				sql.append("SELECT ").append(outMapFields[0]);
				
				if (!oneToMany) {
					throughField = Common.getPrimaryKeyField(type, getManager().getFieldNameConverter());
					
					sql.append(',').append(throughField);
				}
				
				if (thatPolyNames != null) {
					for (String name : thatPolyNames) {
						resPolyNames.add(name);
						sql.append(',').append(name);
					}
				}
				
				sql.append(" FROM ").append(table);
				sql.append(" WHERE ").append(inMapFields[0]).append(" = ?");
				
				if (!where.trim().equals("")) {
					sql.append(" AND (").append(where).append(")");
				}
				
				if (thisPolyNames != null) {
					for (String name : thisPolyNames) {
						sql.append(" AND ").append(name).append(" = ?");
					}
				}
				
				numParams++;
				returnField = outMapFields[0];
			} else {
				sql.append("SELECT DISTINCT a.outMap AS outMap");
				
				if (thatPolyNames != null) {
					for (String name : thatPolyNames) {
						resPolyNames.add(name);
						sql.append(',').append("a.").append(name).append(" AS ").append(name);
					}
				}
				
				sql.append(" FROM (");
				returnField = "outMap";
				
				for (String outMap : outMapFields) {
					for (String inMap : inMapFields) {
						sql.append("SELECT ");
						sql.append(outMap);
						sql.append(" AS outMap,");
						sql.append(inMap);
						sql.append(" AS inMap");
						
						if (thatPolyNames != null) {
							for (String name : thatPolyNames) {
								sql.append(',').append(name);
							}
						}
						
						if (thisPolyNames != null) {
							for (String name : thisPolyNames) {
								sql.append(',').append(name);
							}
						}
						
						sql.append(" FROM ").append(table);
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
				
				if (thatPolyNames != null) {
					if (thatPolyNames.length > 0) {
						sql.append(" WHERE (");
					}
					
					for (String name : thatPolyNames) {
						sql.append("a.").append(name).append(" = ?").append(" OR ");
					}
					
					if (thatPolyNames.length > 0) {
						sql.setLength(sql.length() - " OR ".length());
						sql.append(')');
					}
				}
				
				if (thisPolyNames != null) {
					if (thisPolyNames.length > 0) {
						if (thatPolyNames == null) {
							sql.append(" WHERE (");
						} else {
							sql.append(" AND (");
						}
					}
					
					for (String name : thisPolyNames) {
						sql.append("a.").append(name).append(" = ?").append(" OR ");
					}
					
					if (thisPolyNames.length > 0) {
						sql.setLength(sql.length() - " OR ".length());
						sql.append(')');
					}
				}
			}

			Logger.getLogger("net.java.ao").log(Level.INFO, sql.toString());
			PreparedStatement stmt = conn.prepareStatement(sql.toString());
			
			DatabaseType<K> dbType = (DatabaseType<K>) TypeManager.getInstance().getType(key.getClass());
			int index = 0;
			for (; index < numParams; index++) {
				dbType.putToDatabase(index + 1, stmt, key);
			}
			
			int newLength = numParams + (thisPolyNames == null ? 0 : thisPolyNames.length);
			String typeValue = getManager().getPolymorphicTypeMapper().convert(this.type);
			for (; index < newLength; index++) {
				stmt.setString(index + 1, typeValue);
			}

			dbType = Common.getPrimaryKeyType(finalType);
			DatabaseType<Object> throughDBType = Common.getPrimaryKeyType((Class<? extends RawEntity<Object>>) type);
			
			ResultSet res = stmt.executeQuery();
			while (res.next()) {
				K returnValue = dbType.pullFromDatabase(getManager(), res, (Class<? extends K>) type, returnField);
				Class<V> backType = finalType;
				
				for (String polyName : resPolyNames) {
					if ((typeValue = res.getString(polyName)) != null) {
						backType = (Class<V>) getManager().getPolymorphicTypeMapper().invert(finalType, typeValue);
						break;
					}
				}
				
				if (backType.equals(this.type) && returnValue.equals(key)) {
					continue;
				}
				
				if (throughField != null) {
					throughValues.add(getManager().get((Class<? extends RawEntity<Object>>) type, 
							throughDBType.pullFromDatabase(getManager(), res, type, throughField)));
				}

				V returnValueEntity = getManager().get(backType, returnValue);
				ResultSetMetaData md = res.getMetaData();
				for (int i = 0; i < md.getColumnCount(); i++) {
					if (!resPolyNames.contains(md.getColumnLabel(i + 1))) {
						getManager().getCache().getCacheLayer(returnValueEntity).put(md.getColumnLabel(i + 1), 
								res.getObject(i + 1));
					}
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
				(throughValues.size() > 0 ? throughValues.toArray(new RawEntity[throughValues.size()]) : cached), 
				type, cached, finalType, fields);
		
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

	private <V> V convertValue(ResultSet res, String field, String polyName, Class<V> type) throws SQLException {
		if (res.getString(field) == null) {
			return null;
		}
		
		if (polyName != null) {
			Class<? extends RawEntity<?>> entityType = (Class<? extends RawEntity<?>>) type;
			entityType = getManager().getPolymorphicTypeMapper().invert(entityType, res.getString(polyName));
			
			type = (Class<V>) entityType;		// avoiding Java cast oddities with generics
		}
		
		TypeManager manager = TypeManager.getInstance();
		DatabaseType<V> databaseType = manager.getType(type);
		
		if (databaseType == null) {
			throw new RuntimeException("UnrecognizedType: " + type.toString());
		}
		
		return databaseType.pullFromDatabase(getManager(), res, type, field);
	}

	private boolean instanceOf(Object value, Class<?> type) {
		if (value == null) {
			return true;
		}
		
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

	private void checkConstraints(Method method, Object[] args) {
		AnnotationDelegate annotations = Common.getAnnotationDelegate(getManager().getFieldNameConverter(), method);
		
		NotNull notNullAnnotation = annotations.getAnnotation(NotNull.class);
		if (notNullAnnotation != null && args != null && args.length > 0) {
			if (args[0] == null) {
				String name = getManager().getFieldNameConverter().getName(method);
				throw new IllegalArgumentException("Field '" + name + "' does not accept null values");
			}
		}
	}
}
