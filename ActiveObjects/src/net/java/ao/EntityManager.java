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

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.java.ao.schema.CamelCaseFieldNameConverter;
import net.java.ao.schema.CamelCaseTableNameConverter;
import net.java.ao.schema.FieldNameConverter;
import net.java.ao.schema.SchemaGenerator;
import net.java.ao.schema.TableNameConverter;
import net.java.ao.types.TypeManager;

/**
 * <p>The root control class for the entire ActiveObjects API.  <code>EntityManager</code>
 * is the source of all {@link Entity} objects, as well as the dispatch layer between the entities,
 * the pluggable table name converters, and the database abstraction layers.  This is the
 * entry point for any use of the API.</p>
 * 
 * <p><code>EntityManager</code> is designed to be used in an instance fashion with each
 * instance corresponding to a single database.  Thus, rather than a singleton instance or a
 * static factory method, <code>EntityManager</code> does have a proper constructor.  Any
 * static instance management is left up to the developer using the API.</p>
 * 
 * <p>As a side note, ActiveObjects can optionally log all SQL queries prior to their
 * execution.  This query logging is done with the Java Logging API using the {@link Logger}
 * instance for the <code>net.java.ao</code> package.  This logging is disabled by default
 * by the <code>EntityManager</code> static initializer.  Thus, if it is desirable to log the
 * SQL statements, the <code>Logger</code> level must be set to {@link Level.FINE}
 * <i>after</i> the <code>EntityManager</code> class is first used.  This usually means
 * setting the log level after the constructer has been called.</p>
 * 
 * @author Daniel Spiewak
 */
public class EntityManager {
	
	static {
		Logger.getLogger("net.java.ao").setLevel(Level.OFF);
	}
	
	private DatabaseProvider provider;
	
	private Map<RawEntity, EntityProxy<? extends RawEntity>> proxies;
	private final ReadWriteLock proxyLock = new ReentrantReadWriteLock();
	
	private Map<CacheKey, RawEntity> cache;
	private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();
	
	private TableNameConverter tableNameConverter;
	private final ReadWriteLock tableNameConverterLock = new ReentrantReadWriteLock();
	
	private FieldNameConverter fieldNameConverter;
	private final ReadWriteLock fieldNameConverterLock = new ReentrantReadWriteLock();
	
	private RSCachingStrategy rsStrategy;
	private final ReadWriteLock rsStrategyLock = new ReentrantReadWriteLock();
	
	private Map<Class<? extends ValueGenerator<?>>, ValueGenerator<?>> valGenCache;
	private final ReadWriteLock valGenCacheLock = new ReentrantReadWriteLock();
	
	private final RelationsCache relationsCache = new RelationsCache();
	
	/**
	 * Creates a new instance of <code>EntityManager</code> using the specified
	 * {@link DatabaseProvider}.  This constructor intializes the entity cache, as well
	 * as creates the default {@link TableNameConverter} (the default is 
	 * {@link CamelCaseTableNameConverter}, which is non-pluralized).  The provider
	 * instance is immutable once set using this constructor.  By default (using this
	 * constructor), all entities are strongly cached, meaning references are held to
	 * the instances, preventing garbage collection.
	 * 
	 * @see #EntityManager(DatabaseProvider, boolean)
	 */
	public EntityManager(DatabaseProvider provider) {
		this(provider, false);
	}
	
	/**
	 * Creates a new instance of <code>EntityManager</code> using the specified
	 * {@link DatabaseProvider}.  This constructor initializes the entity and proxy
	 * caches based on the given boolean value.  If <code>true</code>, the entities
	 * will be weakly cached, not maintaining a reference allowing for garbage 
	 * collection.  If <code>false</code>, then strong caching will be used, preventing
	 * garbage collection and ensuring the cache is logically complete.  If you are
	 * concerned about memory leaks, specify <code>true</code>.  Otherwise, for
	 * maximum performance use <code>false</code>.  
	 */
	public EntityManager(DatabaseProvider provider, boolean weaklyCache) {
		this.provider = provider;
		
		if (weaklyCache) {
			proxies = new WeakHashMap<RawEntity, EntityProxy<? extends RawEntity>>();
			cache = new WeakHashMap<CacheKey, RawEntity>();
		} else {
			proxies = new SoftHashMap<RawEntity, EntityProxy<? extends RawEntity>>();
			cache = new SoftHashMap<CacheKey, RawEntity>();
		}
		
		valGenCache = new HashMap<Class<? extends ValueGenerator<?>>, ValueGenerator<?>>();
		
		tableNameConverter = new CamelCaseTableNameConverter();
		fieldNameConverter = new CamelCaseFieldNameConverter();
		rsStrategy = RSCachingStrategy.AGGRESSIVE;
	}
	
	/**
	 * Creates a new instance of <code>EntityManager</code> by auto-magically
	 * finding a {@link DatabaseProvider} instnace for the specified JDBC URI, username
	 * and password.  The auto-magically determined instance is pooled by default
	 * (if a supported connection pooling library is available on the classpath). 
	 * 
	 * @see #EntityManager(DatabaseProvider)
	 * @see net.java.ao.DatabaseProvider#getInstance(String, String, String)
	 */
	public EntityManager(String uri, String username, String password) {
		this(DatabaseProvider.getInstance(uri, username, password));
	}
	
	/**
	 * Convenience method to create the schema for the specified entities
	 * using the current settings (name converter and database provider).
	 * 
	 *  @see net.java.ao.schema.SchemaGenerator#migrate(DatabaseProvider, TableNameConverter, Class...)
	 */
	public void migrate(Class<? extends RawEntity>... entities) throws SQLException {
		tableNameConverterLock.readLock().lock();
		try {
			SchemaGenerator.migrate(provider, tableNameConverter, fieldNameConverter, entities);
		} finally {
			tableNameConverterLock.readLock().unlock();
		}
	}
	
	public void flushAll() {
		proxyLock.readLock().lock();
		try {
			for (EntityProxy<? extends RawEntity> proxy : proxies.values()) {
				proxy.flushCache();
			}
		} finally {
			proxyLock.readLock().unlock();
		}
	}
	
	public void flush(RawEntity... entities) {
		proxyLock.readLock().lock();
		try {
			for (RawEntity entity : entities) {
				proxies.get(entity).flushCache();
			}
		} finally {
			proxyLock.readLock().unlock();
		}
	
	}
	
	/**
	 * <p>Returns an array of entities of the specified type corresponding to the
	 * varargs ids.  If an in-memory reference already exists to a corresponding
	 * entity (of the specified type and id), it is returned rather than creating
	 * a new instance.</p>
	 * 
	 * <p>No checks are performed to ensure that the id actually exists in the
	 * database for the specified object.  Thus, this method is solely a Java
	 * memory state modifying method.  There is not database access involved.
	 * The upshot of this is that the method is very very fast.  The flip side of
	 * course is that one could conceivably maintain entities which reference
	 * non-existant database rows.</p>
	 */
	public <T extends RawEntity, K> T[] get(Class<T> type, K... keys) {
		T[] back = (T[]) Array.newInstance(type, keys.length);
		int index = 0;
		
		for (Object key : keys) {
			cacheLock.writeLock().lock();
			try {
				T entity = (T) cache.get(new CacheKey(key, type));
				if (entity != null) {
					back[index++] = entity;
					continue;
				}
				
				back[index++] = getAndInstantiate(type, key);
			} finally {
				cacheLock.writeLock().unlock();
			}
		}
		
		return back;
	}
	
	// assumes cache doesn't contain object
	protected <T extends RawEntity> T getAndInstantiate(Class<T> type, Object key) {
		EntityProxy<T> proxy = new EntityProxy<T>(this, type, key);
		
		T entity = (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[] {type}, proxy);

		proxyLock.writeLock().lock();
		try {
			proxies.put(entity, proxy);
		} finally {
			proxyLock.writeLock().unlock();
		}
		
		cache.put(new CacheKey(key, type), entity);
		return entity;
	}
	
	/**
	 * Cleverly overloaded method to return a single entity of the specified type
	 * rather than an array in the case where only one ID is passed.  This method
	 * meerly delegates the call to the overloaded <code>get</code> method 
	 * and functions as syntactical sugar.
	 * 
	 * @see #get(Class, int...)
	 */
	public <T extends RawEntity> T get(Class<T> type, Object key) {
		return get(type, new Object[] {key})[0];
	}
	
	/**
	 * <p>Creates a new entity of the specified type with the optionally specified
	 * initial parameters.  This method actually inserts a row into the table represented
	 * by the entity type and returns the entity instance which corresponds to that
	 * row.</p>
	 * 
	 * <p>The {@link DBParam} object parameters are designed to allow the creation
	 * of entities which have non-null fields which have no defalut or auto-generated
	 * value.  Insertion of a row without such field values would of course fail,
	 * thus the need for db params.  The db params can also be used to set
	 * the values for any field in the row, leading to more compact code under
	 * certain circumstances.</p>
	 * 
	 * <p>Unless within a transaction, this method will commit to the database
	 * immediately and exactly once per call.  Thus, care should be taken in
	 * the creation of large numbers of entities.  There doesn't seem to be a more
	 * efficient way to create large numbers of entities, however one should still
	 * be aware of the performance implications.</p>
	 */
	public <T extends RawEntity> T create(Class<T> type, DBParam... params) throws SQLException {
		T back = null;
		String table = null;
		
		tableNameConverterLock.readLock().lock();
		try {
			table = tableNameConverter.getName(type);
		} finally {
			tableNameConverterLock.readLock().unlock();
		}
		
		Set<DBParam> listParams = new HashSet<DBParam>();
		listParams.addAll(Arrays.asList(params));
		
		fieldNameConverterLock.readLock().lock();
		try {
			for (Method method : MethodFinder.getInstance().findAnnotation(Generator.class, type)) {
				Generator genAnno = method.getAnnotation(Generator.class);
				String field = fieldNameConverter.getName(type, method);
				ValueGenerator<?> generator;

				valGenCacheLock.writeLock().lock();
				try {
					if (valGenCache.containsKey(genAnno.value())) {
						generator = valGenCache.get(genAnno.value());
					} else {
						generator = genAnno.value().newInstance();
						valGenCache.put(genAnno.value(), generator);
					}
				} catch (InstantiationException e) {
					continue;
				} catch (IllegalAccessException e) {
					continue;
				} finally {
					valGenCacheLock.writeLock().unlock();
				}
				
				listParams.add(new DBParam(field, generator.generateValue()));
			}
		} finally {
			fieldNameConverterLock.readLock().unlock();
		}
		
		Connection conn = getProvider().getConnection();
		
		try {
			relationsCache.remove(type);
			back = get(type, provider.insertReturningKeys(conn, Common.getPrimaryKeyClassType(type), 
					Common.getPrimaryKeyField(type, getFieldNameConverter()), table, params));
		} finally {
			conn.close();
		}
		
		back.init();
		
		return back;
	}

	/**
	 * <p>Deletes the specified entities from the database.  DELETE statements are
	 * called on the rows in the corresponding tables and the entities are removed
	 * from the instance cache.  The entity instances themselves are not invalidated,
	 * but it doesn't even make sense to continue using the instance without a row
	 * with which it is paired.</p>
	 * 
	 * <p>This method does attempt to group the DELETE statements on a per-type
	 * basis.  Thus, if you pass 5 instances of <code>EntityA</code> and two 
	 * instances of <code>EntityB</code>, the following prepared statement SQL
	 * will be invoked:</p>
	 * 
	 * <pre>DELETE FROM entityA WHERE id IN (?,?,?,?,?);
	 * DELETE FROM entityB WHERE id IN (?,?);</pre>
	 * 
	 * <p>Thus, this method scales very well for large numbers of entities grouped
	 * into types.  However, the execution time scales linearly for each entity of
	 * unique type.</p>
	 */
	public void delete(RawEntity... entities) throws SQLException {
		if (entities.length == 0) {
			return;
		}
		
		Map<Class<? extends RawEntity>, List<RawEntity>> organizedEntities = new HashMap<Class<? extends RawEntity>, List<RawEntity>>();
		
		for (RawEntity entity : entities) {
			Class<? extends RawEntity> type = getProxyForEntity(entity).getType(); 
			
			if (!organizedEntities.containsKey(type)) {
				organizedEntities.put(type, new LinkedList<RawEntity>());
			}
			organizedEntities.get(type).add(entity);
		}
		
		cacheLock.writeLock().lock();
		try {
			Connection conn = getProvider().getConnection();
			try {
				for (Class<? extends RawEntity> type : organizedEntities.keySet()) {
					List<RawEntity> entityList = organizedEntities.get(type);
					
					StringBuilder sql = new StringBuilder("DELETE FROM ");
					
					tableNameConverterLock.readLock().lock();
					try {
						sql.append(tableNameConverter.getName(type));
					} finally {
						tableNameConverterLock.readLock().unlock();
					}
					
					sql.append(" WHERE ").append(Common.getPrimaryKeyField(type, getFieldNameConverter())).append(" IN (?");
					
					for (int i = 1; i < entityList.size(); i++) {
						sql.append(",?");
					}
					sql.append(')');
					
					Logger.getLogger("net.java.ao").log(Level.INFO, sql.toString());
					PreparedStatement stmt = conn.prepareStatement(sql.toString());
					
					int index = 1;
					for (RawEntity entity : entityList) {
						TypeManager.getInstance().getType((Class<RawEntity>) entity.getEntityType()).putToDatabase(index++, stmt, entity);
					}
					
					relationsCache.remove(type);
					stmt.executeUpdate();
					stmt.close();
				}
			} finally {
				conn.close();
			}
			
			for (RawEntity entity : entities) {
				cache.remove(new CacheKey(Common.getPrimaryKeyValue(entity), entity.getEntityType()));
			}
			
			proxyLock.writeLock().lock();
			try {
				for (RawEntity entity : entities) {
					proxies.remove(entity);
				}
			} finally {
				proxyLock.writeLock().unlock();
			}
		} finally {
			cacheLock.writeLock().unlock();
		}
	}
	
	/**
	 * Returns all entities of the given type.  This actually peers the call to
	 * the {@link #find(Class, Query)} method.
	 */
	public <T extends RawEntity> T[] find(Class<T> type) throws SQLException {
		return find(type, Query.select());
	}
	
	/**
	 * <p>Convenience method to select all entities of the given type with the
	 * specified, parameterized criteria.  The <code>criteria</code> String
	 * specified is appended to the SQL prepared statement immediately
	 * following the <code>WHERE</code>.</p>
	 * 
	 * <p>Example:</p>
	 * 
	 * <pre>manager.find(Person.class, "name LIKE ? OR age > ?", "Joe", 9);</pre>
	 * 
	 * <p>This actually peers the call to the {@link #find(Class, Query)}
	 * method, properly parameterizing the {@link Query} object.</p>
	 */
	public <T extends RawEntity> T[] find(Class<T> type, String criteria, Object... parameters) throws SQLException {
		return find(type, Query.select().where(criteria, parameters));
	}
	
	public <T extends RawEntity> T[] find(Class<T> type, Query query) throws SQLException {
		String selectField = Common.getPrimaryKeyField(type, getFieldNameConverter());
		query.resolveFields(type, getFieldNameConverter());
		
		String[] fields = query.getFields();
		if (fields.length == 1) {
			selectField = fields[0];
		}
		
		return find(type, selectField, query);
	}
	
	/**
	 * <p>Selects all entities of the specified type which match the given
	 * <code>Query</code>.  This method creates a <code>PreparedStatement</code>
	 * using the <code>Query</code> instance specified against the table
	 * represented by the given type.  This query is then executed (with the
	 * parameters specified in the query).  The method then iterates through
	 * the result set and extracts the specified field, mapping an <code>Entity</code>
	 * of the given type to each row.  This array of entities is returned.</p>
	 */
	public <T extends RawEntity> T[] find(Class<T> type, String field, Query query) throws SQLException {
		List<T> back = new ArrayList<T>();
		
		query.resolveFields(type, getFieldNameConverter());
		
		Preload preloadAnnotation = type.getAnnotation(Preload.class);
		if (preloadAnnotation != null) {
			if (!query.getFields()[0].equals("*") && query.getJoins().isEmpty()) {
				String[] oldFields = query.getFields();
				List<String> newFields = new ArrayList<String>();
				
				for (String newField : preloadAnnotation.value()) {
					newField = newField.trim();
					
					int fieldLoc = Arrays.binarySearch(oldFields, newField);
					
					if (fieldLoc < 0) {
						newFields.add(newField);
					} else {
						newFields.add(oldFields[fieldLoc]);
					}
				}
				
				if (!newFields.contains("*")) {
					for (String oldField : oldFields) {
						if (!newFields.contains(oldField)) {
							newFields.add(oldField);
						}
					}
				}
				
				query.setFields(newFields.toArray(new String[newFields.size()]));
			}
		}
		
		Connection conn = getProvider().getConnection();
		try {
			String sql = null;
			tableNameConverterLock.readLock().lock();
			try {
				sql = query.toSQL(type, provider, tableNameConverter, getFieldNameConverter(), false);
			} finally {
				tableNameConverterLock.readLock().unlock();
			}
			
			Logger.getLogger("net.java.ao").log(Level.INFO, sql);
			PreparedStatement stmt = conn.prepareStatement(sql);
			provider.setQueryStatementProperties(stmt, query);
			
			query.setParameters(stmt);

			ResultSet res = stmt.executeQuery();
			provider.setQueryResultSetProperties(res, query);
			
			while (res.next()) {
				T entity = get(type, Common.getPrimaryKeyType(type).convert(this, res, Common.getPrimaryKeyClassType(type), field));
				
				rsStrategyLock.readLock().lock();
				try {
					rsStrategy.cache(res, getProxyForEntity(entity));
				} finally {
					rsStrategyLock.readLock().unlock();
				}
				
				back.add(entity);
			}
			res.close();
			stmt.close();
		} finally {
			conn.close();
		}
		
		return back.toArray((T[]) Array.newInstance(type, back.size()));
	}
	
	/**
	 * Executes the specified SQL and extracts the given idfield, wrapping each
	 * row into a instance of the specified type.  The SQL itself is executed as 
	 * a PreparedStatement with the given parameters. 
	 */
	@SuppressWarnings("unchecked")
	public <T extends RawEntity> T[] findWithSQL(Class<T> type, String keyField, String sql, Object... parameters) throws SQLException {
		List<T> back = new ArrayList<T>();
		
		Connection conn = getProvider().getConnection();
		try {
			Logger.getLogger("net.java.ao").log(Level.INFO, sql);
			PreparedStatement stmt = conn.prepareStatement(sql);
			
			TypeManager manager = TypeManager.getInstance();
			for (int i = 0; i < parameters.length; i++) {
				Class javaType = parameters[i].getClass();
				
				if (parameters[i] instanceof RawEntity) {
					javaType = ((RawEntity) parameters[i]).getEntityType();
				}
				
				manager.getType(javaType).putToDatabase(i + 1, stmt, parameters[i]);
			}

			ResultSet res = stmt.executeQuery();
			while (res.next()) {
				back.add(get(type, Common.getPrimaryKeyType(type).convert(this, res, type, keyField)));
			}
			res.close();
			stmt.close();
		} finally {
			conn.close();
		}
		
		return back.toArray((T[]) Array.newInstance(type, back.size()));
	}
	
	/**
	 * Counts all entities of the specified type.  This method is actually
	 * a delegate for the <code>count(Class&lt;? extends Entity&gt;, Query)</code>
	 * method.
	 */
	public int count(Class<? extends RawEntity> type) throws SQLException {
		return count(type, Query.select());
	}
	
	/**
	 * Counts all entities of the specified type matching the given criteria
	 * and parameters.  This is a convenience method for:
	 * 
	 * <code>count(type, Query.select().where(criteria, parameters))</code>
	 */
	public int count(Class<? extends RawEntity> type, String criteria, Object... parameters) throws SQLException {
		return count(type, Query.select().where(criteria, parameters));
	}
	
	/**
	 * Counts all entities of the specified type matching the given {@link Query}
	 * instance.  The SQL runs as a <code>SELECT COUNT(*)</code> to
	 * ensure maximum performance.
	 */
	public int count(Class<? extends RawEntity> type, Query query) throws SQLException {
		int back = -1;
		
		Connection conn = getProvider().getConnection();
		try {
			String sql = null;
			tableNameConverterLock.readLock().lock();
			try {
				sql = query.toSQL(type, provider, tableNameConverter, getFieldNameConverter(), true);
			} finally {
				tableNameConverterLock.readLock().unlock();
			}
			
			Logger.getLogger("net.java.ao").log(Level.INFO, sql);
			PreparedStatement stmt = conn.prepareStatement(sql);
			provider.setQueryStatementProperties(stmt, query);
			
			query.setParameters(stmt);

			ResultSet res = stmt.executeQuery();
			if (res.next()) {
				back = res.getInt(1);
			}
			res.close();
			stmt.close();
		} finally {
			conn.close();
		}
		
		return back;
	}
	
	/**
	 * <p>Specifies the {@link TableNameConverter} instance to use for
	 * name conversion of all entity types.  Name conversion is the process
	 * of determining the appropriate table name from an arbitrary {@link Entity}
	 * class.</p>
	 * 
	 * <p>The default nameConverter is {@link CamelCaseTableNameConverter}.</p>
	 */
	public void setTableNameConverter(TableNameConverter tableNameConverter) {
		tableNameConverterLock.writeLock().lock();
		try {
			this.tableNameConverter = tableNameConverter;
		} finally {
			tableNameConverterLock.writeLock().unlock();
		}
	}
	
	/**
	 * Retrieves the {@link TableNameConverter} instance used for name
	 * conversion of all entity types.
	 * 
	 * @see #setTableNameConverter(TableNameConverter)
	 */
	public TableNameConverter getTableNameConverter() {
		tableNameConverterLock.readLock().lock();
		try {
			return tableNameConverter;
		} finally {
			tableNameConverterLock.readLock().unlock();
		}
	}
	
	public void setFieldNameConverter(FieldNameConverter fieldNameConverter) {
		fieldNameConverterLock.writeLock().lock();
		try {
			this.fieldNameConverter = fieldNameConverter;
		} finally {
			fieldNameConverterLock.writeLock().unlock();
		}
	}
	
	public FieldNameConverter getFieldNameConverter() {
		fieldNameConverterLock.readLock().lock();
		try {
			return fieldNameConverter;
		} finally {
			fieldNameConverterLock.readLock().unlock();
		}
	}

	/**
	 * 
	 */
	public void setRSCachingStrategy(RSCachingStrategy rsStrategy) {
		rsStrategyLock.writeLock().lock();
		try {
			this.rsStrategy = rsStrategy;
		} finally {
			rsStrategyLock.writeLock().unlock();
		}
	}
	
	public RSCachingStrategy getRSCachingStrategy() {
		rsStrategyLock.readLock().lock();
		try {
			return rsStrategy;
		} finally {
			rsStrategyLock.readLock().unlock();
		}
	}

	public DatabaseProvider getProvider() {
		return provider;
	}

	protected <T extends RawEntity> EntityProxy<T> getProxyForEntity(T entity) {
		proxyLock.readLock().lock();
		try {
			return (EntityProxy<T>) proxies.get(entity);
		} finally {
			proxyLock.readLock().unlock();
		}
	}

	RelationsCache getRelationsCache() {
		return relationsCache;
	}

	private static class CacheKey {
		private Object key;
		private Class<? extends RawEntity> type;
		
		public CacheKey(Object key, Class<? extends RawEntity> type) {
			this.key = key;
			this.type = type;
		}
		
		public int hashCode() {
			return (type.hashCode() + (key != null ? key.hashCode() : 0)) % (2 << 15);
		}
		
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			
			if (obj instanceof CacheKey) {
				CacheKey key = (CacheKey) obj;
				
				if (key == key.key && type.equals(key.type)) {
					return true;
				}
			}
			
			return false;
		}
	}
}
