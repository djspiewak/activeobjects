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

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
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

import net.java.ao.cache.Cache;
import net.java.ao.cache.CacheLayer;
import net.java.ao.cache.RAMCache;
import net.java.ao.cache.RAMRelationsCache;
import net.java.ao.cache.RelationsCache;
import net.java.ao.schema.AutoIncrement;
import net.java.ao.schema.CamelCaseFieldNameConverter;
import net.java.ao.schema.CamelCaseTableNameConverter;
import net.java.ao.schema.FieldNameConverter;
import net.java.ao.schema.SchemaGenerator;
import net.java.ao.schema.TableNameConverter;
import net.java.ao.types.TypeManager;

/**
 * <p>The root control class for the entire ActiveObjects API.  <code>EntityManager</code>
 * is the source of all {@link RawEntity} objects, as well as the dispatch layer between the entities,
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
	
	private final DatabaseProvider provider;
	
	private final boolean weaklyCache;
	
	private Map<RawEntity<?>, EntityProxy<? extends RawEntity<?>, ?>> proxies;
	private final ReadWriteLock proxyLock = new ReentrantReadWriteLock();
	
	private Map<CacheKey<?>, Reference<RawEntity<?>>> entityCache;
	private final ReadWriteLock entityCacheLock = new ReentrantReadWriteLock();
	
	private Cache cache;
	private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();
	
	private TableNameConverter tableNameConverter;
	private final ReadWriteLock tableNameConverterLock = new ReentrantReadWriteLock();
	
	private FieldNameConverter fieldNameConverter;
	private final ReadWriteLock fieldNameConverterLock = new ReentrantReadWriteLock();
	
	private PolymorphicTypeMapper typeMapper;
	private final ReadWriteLock typeMapperLock = new ReentrantReadWriteLock();
	
	private Map<Class<? extends ValueGenerator<?>>, ValueGenerator<?>> valGenCache;
	private final ReadWriteLock valGenCacheLock = new ReentrantReadWriteLock();
	
	private final RelationsCache relationsCache = new RAMRelationsCache();
	
	/**
	 * Creates a new instance of <code>EntityManager</code> using the specified
	 * {@link DatabaseProvider}.  This constructor intializes the entity cache, as well
	 * as creates the default {@link TableNameConverter} (the default is 
	 * {@link CamelCaseTableNameConverter}, which is non-pluralized) and the default
	 * {@link FieldNameConverter} ({@link CamelCaseFieldNameConverter}).  The provider
	 * instance is immutable once set using this constructor.  By default (using this
	 * constructor), all entities are strongly cached, meaning references are held to
	 * the instances, preventing garbage collection.
	 * 
	 * @param provider	The {@link DatabaseProvider} to use in all database operations.
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
	 * concerned about memory, specify <code>true</code>.  Otherwise, for
	 * maximum performance use <code>false</code> (highly recomended).
	 * 
	 * @param provider	The {@link DatabaseProvider} to use in all database operations.
	 * @param weaklyCache	Whether or not to use {@link WeakReference} in the entity
	 * 		cache.  If <code>false</code>, then {@link SoftReference} will be used.
	 */
	public EntityManager(DatabaseProvider provider, boolean weaklyCache) {
		this.provider = provider;
		this.weaklyCache = weaklyCache;
		
		if (weaklyCache) {
			proxies = new WeakHashMap<RawEntity<?>, EntityProxy<? extends RawEntity<?>, ?>>();
		} else {
			proxies = new SoftHashMap<RawEntity<?>, EntityProxy<? extends RawEntity<?>, ?>>();
		}
		
		entityCache = new HashMap<CacheKey<?>, Reference<RawEntity<?>>>();
		
		cache = new RAMCache();
		
		valGenCache = new HashMap<Class<? extends ValueGenerator<?>>, ValueGenerator<?>>();
		
		tableNameConverter = new CamelCaseTableNameConverter();
		fieldNameConverter = new CamelCaseFieldNameConverter();
		typeMapper = new DefaultPolymorphicTypeMapper(new HashMap<Class<? extends RawEntity<?>>, String>());
	}
	
	/**
	 * <p>Creates a new instance of <code>EntityManager</code> by auto-magically
	 * finding a {@link DatabaseProvider} instance for the specified JDBC URI, username
	 * and password.  The auto-magically determined instance is pooled by default
	 * (if a supported connection pooling library is available on the classpath).</p>
	 * 
	 * <p>The actual auto-magical parsing code isn't contained within this method,
	 * but in {@link DatabaseProvider#getInstance(String, String, String)}.  This way,
	 * it is possible to use the parsing logic to get a <code>DatabaseProvider</code>
	 * instance separate from <code>EntityManager</code> if necessary.</p>
	 * 
	 * @param uri	The JDBC URI to use for the database connection.
	 * @param username	The username to use in authenticating the database connection. 
	 * @param password		The password to use in authenticating the database connection.
	 * @see #EntityManager(DatabaseProvider)
	 * @see net.java.ao.DatabaseProvider#getInstance(String, String, String)
	 */
	public EntityManager(String uri, String username, String password) {
		this(DatabaseProvider.getInstance(uri, username, password));
	}
	
	/**
	 * Convenience method to create the schema for the specified entities
	 * using the current settings (table/field name converter and database provider).
	 * 
	 *  @see net.java.ao.schema.SchemaGenerator#migrate(DatabaseProvider, TableNameConverter, FieldNameConverter, Class...)
	 */
	public void migrate(Class<? extends RawEntity<?>>... entities) throws SQLException {
		tableNameConverterLock.readLock().lock();
		fieldNameConverterLock.readLock().lock();
		try {
			SchemaGenerator.migrate(provider, tableNameConverter, fieldNameConverter, entities);
		} finally {
			fieldNameConverterLock.readLock().unlock();
			tableNameConverterLock.readLock().unlock();
		}
	}
	
	/**
	 * Flushes all value caches contained within entities controlled by this <code>EntityManager</code>
	 * instance.  This does not actually remove the entities from the instance cache maintained
	 * within this class.  Rather, it simply dumps all of the field values cached within the entities
	 * themselves (with the exception of the primary key value).  This should be used in the case
	 * of a complex process outside AO control which may have changed values in the database.  If
	 * it is at all possible to determine precisely which rows have been changed, the {@link #flush(RawEntity...)}
	 * method should be used instead.
	 */
	public void flushAll() {
		proxyLock.readLock().lock();
		try {
			for (EntityProxy<? extends RawEntity<?>, ?> proxy : proxies.values()) {
				proxy.flushCache();
			}
		} finally {
			proxyLock.readLock().unlock();
		}
		
		relationsCache.flush();
	}
	
	/**
	 * Flushes the value caches of the specified entities along with all of the relevant
	 * relations cache entries.  This should be called after a process outside of AO control
	 * may have modified the values in the specified rows.  This does not actually remove
	 * the entity instances themselves from the instance cache.  Rather, it just flushes all
	 * of their internally cached values (with the exception of the primary key).
	 */
	public void flush(RawEntity<?>... entities) {
		proxyLock.readLock().lock();
		try {
			List<Class<? extends RawEntity<?>>> types = new ArrayList<Class<? extends RawEntity<?>>>(entities.length);
			
			for (RawEntity<?> entity : entities) {
				types.add(entity.getEntityType());
				proxies.get(entity).flushCache();
			}
			
			relationsCache.remove(types.toArray(new Class[types.size()]));
		} finally {
			proxyLock.readLock().unlock();
		}
	}
	
	/**
	 * <p>Returns an array of entities of the specified type corresponding to the
	 * varargs primary keys.  If an in-memory reference already exists to a corresponding
	 * entity (of the specified type and key), it is returned rather than creating
	 * a new instance.</p>
	 * 
	 * <p>No checks are performed to ensure that the key actually exists in the
	 * database for the specified object.  Thus, this method is solely a Java
	 * memory state modifying method.  There is no database access involved.
	 * The upshot of this is that the method is very very fast.  The flip side of
	 * course is that one could conceivably maintain entities which reference
	 * non-existant database rows.</p>
	 * 
	 * @param type		The type of the entities to retrieve.
	 * @param keys	The primary keys corresponding to the entities to retrieve.  All
	 * 	keys must be typed according to the generic type parameter of the entity's
	 * 	{@link RawEntity} inheritence (if inheriting from {@link Entity}, this is <code>Integer</code>
	 * 	or <code>int</code>).  Thus, the <code>keys</code> array is type-checked at compile
	 * 	time.
	 * @return An array of entities of the given type corresponding with the specified primary keys.
	 */
	public <T extends RawEntity<K>, K> T[] get(Class<T> type, K... keys) {
		T[] back = (T[]) Array.newInstance(type, keys.length);
		int index = 0;
		
		for (K key : keys) {
			entityCacheLock.writeLock().lock();
			try {	// upcast to workaround bug in javac
				Reference<?> reference = entityCache.get(new CacheKey<K>(key, type));
				Reference<T> ref = (Reference<T>) reference;
				T entity = (ref == null ? null : ref.get());
				
				if (entity != null) {
					back[index++] = entity;
				} else {
					back[index++] = getAndInstantiate(type, key);
				}
			} finally {
				entityCacheLock.writeLock().unlock();
			}
		}
		
		return back;
	}
	
	/**
	 * Creates a new instance of the entity of the specified type corresponding to the
	 * given primary key.  This is used by {@link #get(Class, Object...)} to create the entity
	 * if the instance is not found already in the cache.  This method should not be
	 * repurposed to perform any caching, since ActiveObjects already assumes that
	 * the caching has been performed.
	 * 
	 *  @param type	The type of the entity to create.
	 *  @param key		The primary key corresponding to the entity instance required.
	 *  @return An entity instance of the specified type and primary key.
	 */
	protected <T extends RawEntity<K>, K> T getAndInstantiate(Class<T> type, K key) {
		EntityProxy<T, K> proxy = new EntityProxy<T, K>(this, type, key);
		
		T entity = (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[] {type}, proxy);

		proxyLock.writeLock().lock();
		try {
			proxies.put(entity, proxy);
		} finally {
			proxyLock.writeLock().unlock();
		}
		
		entityCache.put(new CacheKey<K>(key, type), createRef(entity));
		return entity;
	}
	
	/**
	 * Cleverly overloaded method to return a single entity of the specified type
	 * rather than an array in the case where only one ID is passed.  This method
	 * meerly delegates the call to the overloaded <code>get</code> method 
	 * and functions as syntactical sugar.
	 * 
	 * @param type		The type of the entity instance to retrieve.
	 * @param key		The primary key corresponding to the entity to be retrieved.
	 * @return An entity instance of the given type corresponding to the specified primary key.
	 * @see #get(Class, Object...)
	 */
	public <T extends RawEntity<K>, K> T get(Class<T> type, K key) {
		return get(type, (K[]) new Object[] {key})[0];
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
	 * 
	 * <p>This method delegates the action INSERT action to 
	 * {@link DatabaseProvider#insertReturningKey(Connection, Class, String, boolean, String, DBParam...)}.
	 * This is necessary because not all databases support the JDBC <code>RETURN_GENERATED_KEYS</code>
	 * constant (e.g. PostgreSQL and HSQLDB).  Thus, the database provider itself is
	 * responsible for handling INSERTion and retrieval of the correct primary key
	 * value.</p>
	 * 
	 * @param type		The type of the entity to INSERT.
	 * @param params	An optional varargs array of initial values for the fields in the row.  These
	 * 	values will be passed to the database within the INSERT statement.
	 * @return	The new entity instance corresponding to the INSERTed row.
	 * @see net.java.ao.DBParam
	 * @see net.java.ao.DatabaseProvider#insertReturningKey(Connection, Class, String, boolean, String, DBParam...)
	 */
	public <T extends RawEntity<K>, K> T create(Class<T> type, DBParam... params) throws SQLException {
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
				String field = fieldNameConverter.getName(method);
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
				
				listParams.add(new DBParam(field, generator.generateValue(this)));
			}
		} finally {
			fieldNameConverterLock.readLock().unlock();
		}
		
		Connection conn = getProvider().getConnection();
		
		try {
			relationsCache.remove(type);
			
			Method pkMethod = Common.getPrimaryKeyMethod(type);
			back = get(type, provider.insertReturningKey(conn, Common.getPrimaryKeyClassType(type), 
					Common.getPrimaryKeyField(type, getFieldNameConverter()), 
					pkMethod.getAnnotation(AutoIncrement.class) != null, 
					table, listParams.toArray(new DBParam[listParams.size()])));
		} finally {
			conn.close();
		}
		
		back.init();
		
		return back;
	}
	
	/**
	 * Creates and INSERTs a new entity of the specified type with the given map of 
	 * parameters.  This method merely delegates to the {@link #create(Class, DBParam...)} 
	 * method.  The idea behind having a separate convenience method taking a map is in 
	 * circumstances with large numbers of parameters or for people familiar with the 
	 * anonymous inner class constructor syntax who might be more comfortable with 
	 * creating a map than with passing a number of objects.
	 * 
	 * @param type	The type of the entity to INSERT.
	 * @param params	A map of parameters to pass to the INSERT.
	 * @return	The new entity instance corresponding to the INSERTed row.
	 * @see #create(Class, DBParam...)
	 */
	public <T extends RawEntity<K>, K> T create(Class<T> type, Map<String, Object> params) throws SQLException {
		DBParam[] arrParams = new DBParam[params.size()];
		int i = 0;
		
		for (String key : params.keySet()) {
			arrParams[i++] = new DBParam(key, params.get(key));
		}
		
		return create(type, arrParams);
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
	 * instances of <code>EntityB</code>, the following SQL prepared statements
	 * will be invoked:</p>
	 * 
	 * <pre>DELETE FROM entityA WHERE id IN (?,?,?,?,?);
	 * DELETE FROM entityB WHERE id IN (?,?);</pre>
	 * 
	 * <p>Thus, this method scales very well for large numbers of entities grouped
	 * into types.  However, the execution time increases linearly for each entity of
	 * unique type.</p>
	 * 
	 * @param entities	A varargs array of entities to delete.  Method returns immediately
	 * 	if length == 0.
	 */
	@SuppressWarnings("unchecked")
	public void delete(RawEntity<?>... entities) throws SQLException {
		if (entities.length == 0) {
			return;
		}
		
		Map<Class<? extends RawEntity<?>>, List<RawEntity<?>>> organizedEntities = 
			new HashMap<Class<? extends RawEntity<?>>, List<RawEntity<?>>>();
		
		for (RawEntity<?> entity : entities) {
			Class<? extends RawEntity<?>> type = getProxyForEntity(entity).getType(); 
			
			if (!organizedEntities.containsKey(type)) {
				organizedEntities.put(type, new LinkedList<RawEntity<?>>());
			}
			organizedEntities.get(type).add(entity);
		}
		
		entityCacheLock.writeLock().lock();
		try {
			Connection conn = getProvider().getConnection();
			try {
				for (Class<? extends RawEntity<?>> type : organizedEntities.keySet()) {
					List<RawEntity<?>> entityList = organizedEntities.get(type);
					
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
					for (RawEntity<?> entity : entityList) {
						TypeManager.getInstance().getType((Class) entity.getEntityType()).putToDatabase(index++, stmt, entity);
					}
					
					relationsCache.remove(type);
					stmt.executeUpdate();
					stmt.close();
				}
			} finally {
				conn.close();
			}
			
			for (RawEntity<?> entity : entities) {
				entityCache.remove(new CacheKey<Object>(Common.getPrimaryKeyValue(entity), 
						(Class<? extends RawEntity<Object>>) entity.getEntityType()));
			}
			
			proxyLock.writeLock().lock();
			try {
				for (RawEntity<?> entity : entities) {
					proxies.remove(entity);
				}
			} finally {
				proxyLock.writeLock().unlock();
			}
		} finally {
			entityCacheLock.writeLock().unlock();
		}
	}
	
	/**
	 * Returns all entities of the given type.  This actually peers the call to
	 * the {@link #find(Class, Query)} method.
	 * 
	 * @param type		The type of entity to retrieve.
	 * @return	An array of all entities which correspond to the given type.
	 */
	public <T extends RawEntity<K>, K> T[] find(Class<T> type) throws SQLException {
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
	 * <pre>manager.find(Person.class, "name LIKE ? OR age &gt; ?", "Joe", 9);</pre>
	 * 
	 * <p>This actually delegates the call to the {@link #find(Class, Query)}
	 * method, properly parameterizing the {@link Query} object.</p>
	 * 
	 * @param type		The type of the entities to retrieve.
	 * @param criteria		A parameterized WHERE statement used to determine the results.
	 * @param parameters	A varargs array of parameters to be passed to the executed
	 * 	prepared statement.  The length of this array <i>must</i> match the number of
	 * 	parameters (denoted by the '?' char) in the <code>criteria</code>.
	 * @return	An array of entities of the given type which match the specified criteria.
	 */
	public <T extends RawEntity<K>, K> T[] find(Class<T> type, String criteria, Object... parameters) throws SQLException {
		return find(type, Query.select().where(criteria, parameters));
	}
	
	/**
	 * <p>Selects all entities matching the given type and {@link Query}.  By default, the
	 * entities will be created based on the values within the primary key field for the
	 * specified type (this is usually the desired behavior).</p>
	 * 
	 * <p>Example:</p>
	 * 
	 * <pre>manager.find(Person.class, Query.select().where("name LIKE ? OR age &gt; ?", "Joe", 9).limit(10));</pre>
	 * 
	 * <p>This method delegates the call to {@link #find(Class, String, Query)}, passing the
	 * primary key field for the given type as the <code>String</code> parameter.</p>
	 * 
	 * @param type		The type of the entities to retrieve.
	 * @param query	The {@link Query} instance to be used to determine the results.
	 * @return An array of entities of the given type which match the specified query.
	 */
	public <T extends RawEntity<K>, K> T[] find(Class<T> type, Query query) throws SQLException {
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
	 * the result set and extracts the specified field, mapping an entity
	 * of the given type to each row.  This array of entities is returned.</p>
	 * 
	 * @param type		The type of the entities to retrieve.
	 * @param field		The field value to use in the creation of the entities.  This is usually
	 * 	the primary key field of the corresponding table.
	 * @param query	The {@link Query} instance to use in determining the results.
	 * @return	An array of entities of the given type which match the specified query.
	 */
	public <T extends RawEntity<K>, K> T[] find(Class<T> type, String field, Query query) throws SQLException {
		List<T> back = new ArrayList<T>();
		
		query.resolveFields(type, getFieldNameConverter());
		
		Preload preloadAnnotation = type.getAnnotation(Preload.class);
		if (preloadAnnotation != null) {
			if (!query.getFields()[0].equals("*") && query.getJoins().isEmpty()) {
				String[] oldFields = query.getFields();
				List<String> newFields = new ArrayList<String>();
				
				for (String newField : preloadAnnotation.value()) {
					newField = newField.trim();
					
					int fieldLoc = -1;
					for (int i = 0; i < oldFields.length; i++) {
						if (oldFields[i].equals(newField)) {
							fieldLoc = i;
							break;
						}
					}
					
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
			PreparedStatement stmt = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			provider.setQueryStatementProperties(stmt, query);
			
			query.setParameters(this, stmt);

			ResultSet res = stmt.executeQuery();
			ResultSetMetaData md = res.getMetaData();
			
			provider.setQueryResultSetProperties(res, query);
			
			while (res.next()) {
				T entity = get(type, Common.getPrimaryKeyType(type).pullFromDatabase(this, res, Common.getPrimaryKeyClassType(type), field));
				CacheLayer cacheLayer = getCache().getCacheLayer(entity);

				for (int i = 0; i < md.getColumnCount(); i++) {
					cacheLayer.put(md.getColumnLabel(i + 1), res.getObject(i + 1));
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
	 * <p>Executes the specified SQL and extracts the given key field, wrapping each
	 * row into a instance of the specified type.  The SQL itself is executed as 
	 * a {@link PreparedStatement} with the given parameters.</p>
	 * 
	 *  <p>Example:</p>
	 *  
	 *  <pre>manager.findWithSQL(Person.class, "personID", "SELECT personID FROM chairs WHERE position &lt; ? LIMIT ?", 10, 5);</pre>
	 *  
	 *  <p>The SQL is not parsed or modified in any way by ActiveObjects.  As such, it is
	 *  possible to execute database-specific queries using this method without realizing
	 *  it.  For example, the above query will not run on MS SQL Server or Oracle, due to 
	 *  the lack of a LIMIT clause in their SQL implementation.  As such, be extremely
	 *  careful about what SQL is executed using this method, or else be conscious of the
	 *  fact that you may be locking yourself to a specific DBMS.</p>
	 *  
	 * @param type		The type of the entities to retrieve.
	 * @param keyField	The field value to use in the creation of the entities.  This is usually
	 * 	the primary key field of the corresponding table.
	 * @param sql	The SQL statement to execute.
	 * @param parameters	A varargs array of parameters to be passed to the executed
	 * 	prepared statement.  The length of this array <i>must</i> match the number of
	 * 	parameters (denoted by the '?' char) in the <code>criteria</code>.
	 * @return	An array of entities of the given type which match the specified query.
	 */
	@SuppressWarnings("unchecked")
	public <T extends RawEntity<K>, K> T[] findWithSQL(Class<T> type, String keyField, String sql, Object... parameters) throws SQLException {
		List<T> back = new ArrayList<T>();
		
		Connection conn = getProvider().getConnection();
		try {
			Logger.getLogger("net.java.ao").log(Level.INFO, sql);
			PreparedStatement stmt = conn.prepareStatement(sql);
			
			TypeManager manager = TypeManager.getInstance();
			for (int i = 0; i < parameters.length; i++) {
				Class javaType = parameters[i].getClass();
				
				if (parameters[i] instanceof RawEntity) {
					javaType = ((RawEntity<?>) parameters[i]).getEntityType();
				}
				
				manager.getType(javaType).putToDatabase(i + 1, stmt, parameters[i]);
			}

			ResultSet res = stmt.executeQuery();
			while (res.next()) {
				back.add(get(type, Common.getPrimaryKeyType(type).pullFromDatabase(this, res, (Class<? extends K>) type, keyField)));
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
	 * a delegate for: <code>count(Class&lt;? extends Entity&gt;, Query)</code>
	 * 
	 * @param type		The type of the entities which should be counted.
	 * @return The number of entities of the specified type.
	 */
	public <K> int count(Class<? extends RawEntity<K>> type) throws SQLException {
		return count(type, Query.select());
	}
	
	/**
	 * Counts all entities of the specified type matching the given criteria
	 * and parameters.  This is a convenience method for:
	 * <code>count(type, Query.select().where(criteria, parameters))</code>
	 * 
	 * @param type		The type of the entities which should be counted.
	 * @param criteria		A parameterized WHERE statement used to determine the result
	 * 	set which will be counted.
	 * @param parameters	A varargs array of parameters to be passed to the executed
	 * 	prepared statement.  The length of this array <i>must</i> match the number of
	 * 	parameters (denoted by the '?' char) in the <code>criteria</code>.
	 * @return The number of entities of the given type which match the specified criteria.
	 */
	public <K> int count(Class<? extends RawEntity<K>> type, String criteria, Object... parameters) throws SQLException {
		return count(type, Query.select().where(criteria, parameters));
	}
	
	/**
	 * Counts all entities of the specified type matching the given {@link Query}
	 * instance.  The SQL runs as a <code>SELECT COUNT(*)</code> to
	 * ensure maximum performance.
	 * 
	 * @param type		The type of the entities which should be counted.
	 * @param query	The {@link Query} instance used to determine the result set which
	 * 	will be counted.
	 * @return The number of entities of the given type which match the specified query.
	 */
	public <K> int count(Class<? extends RawEntity<K>> type, Query query) throws SQLException {
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
			
			query.setParameters(this, stmt);

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
	 * of determining the appropriate table name from an arbitrary interface
	 * extending {@link RawEntity}.</p>
	 * 
	 * <p>The default table name converter is {@link CamelCaseTableNameConverter}.</p>
	 * 
	 * @see #getTableNameConverter()
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
	
	/**
	 * <p>Specifies the {@link FieldNameConverter} instance to use for
	 * field name conversion of all entity methods.  Name conversion is the
	 * process of determining the appropriate field name from an arbitrary
	 * method within an interface extending {@link RawEntity}.</p>
	 * 
	 * <p>The default field name converter is {@link CamelCaseFieldNameConverter}.</p>
	 * 
	 * @see #getFieldNameConverter()
	 */
	public void setFieldNameConverter(FieldNameConverter fieldNameConverter) {
		fieldNameConverterLock.writeLock().lock();
		try {
			this.fieldNameConverter = fieldNameConverter;
		} finally {
			fieldNameConverterLock.writeLock().unlock();
		}
	}
	
	/**
	 * Retrieves the {@link FieldNameConverter} instance used for name
	 * conversion of all entity methods.
	 * 
	 * @see #setFieldNameConverter(FieldNameConverter)
	 */
	public FieldNameConverter getFieldNameConverter() {
		fieldNameConverterLock.readLock().lock();
		try {
			return fieldNameConverter;
		} finally {
			fieldNameConverterLock.readLock().unlock();
		}
	}
	
	/**
	 * Specifies the {@link PolymorphicTypeMapper} instance to use for
	 * all flag value conversion of polymorphic types.  The default type
	 * mapper is an empty {@link DefaultPolymorphicTypeMapper} instance
	 * (thus using the fully qualified classname for all values).
	 * 
	 * @see #getPolymorphicTypeMapper()
	 */
	public void setPolymorphicTypeMapper(PolymorphicTypeMapper typeMapper) {
		typeMapperLock.writeLock().lock();
		try {
			this.typeMapper = typeMapper;
			
			if (typeMapper instanceof DefaultPolymorphicTypeMapper) {
				((DefaultPolymorphicTypeMapper) typeMapper).resolveMappings(getTableNameConverter());
			}
		} finally {
			typeMapperLock.writeLock().unlock();
		}
	}
	
	/**
	 * Retrieves the {@link PolymorphicTypeMapper} instance used for flag
	 * value conversion of polymorphic types.
	 * 
	 * @see #setPolymorphicTypeMapper(PolymorphicTypeMapper)
	 */
	public PolymorphicTypeMapper getPolymorphicTypeMapper() {
		typeMapperLock.readLock().lock();
		try {
			if (typeMapper == null) {
				throw new RuntimeException("No polymorphic type mapper was specified");
			}
			
			return typeMapper;
		} finally {
			typeMapperLock.readLock().unlock();
		}
	}
	
	public void setCache(Cache cache) {
		cacheLock.writeLock().lock();
		try {
			if (!this.cache.equals(cache)) {
				this.cache.dispose();
				this.cache = cache;
			}
		} finally {
			cacheLock.writeLock().unlock();
		}
	}
	
	public Cache getCache() {
		cacheLock.readLock().lock();
		try {
			return cache;
		} finally {
			cacheLock.readLock().unlock();
		}
	}

	/**
	 * <p>Retrieves the database provider used by this <code>EntityManager</code>
	 * for all database operations.  This method can be used reliably to obtain
	 * a database provider and hence a {@link Connection} instance which can
	 * be used for JDBC operations outside of ActiveObjects.  Thus:</p>
	 * 
	 * <pre>Connection conn = manager.getProvider().getConnection();
	 * try {
	 *     // ...
	 * } finally {
	 *     conn.close();
	 * }</pre>
	 */
	public DatabaseProvider getProvider() {
		return provider;
	}

	<T extends RawEntity<K>, K> EntityProxy<T, K> getProxyForEntity(T entity) {
		proxyLock.readLock().lock();
		try {
			return (EntityProxy<T, K>) proxies.get(entity);
		} finally {
			proxyLock.readLock().unlock();
		}
	}

	RelationsCache getRelationsCache() {
		return relationsCache;
	}

	private Reference<RawEntity<?>> createRef(RawEntity<?> entity) {
		if (weaklyCache) {
			return new WeakReference<RawEntity<?>>(entity);
		}
		
		return new SoftReference<RawEntity<?>>(entity);
	}
	
	private static class CacheKey<T> {
		private T key;
		private Class<? extends RawEntity<?>> type;
		
		public CacheKey(T key, Class<? extends RawEntity<T>> type) {
			this.key = key;
			this.type = type;
		}
		
		@Override
		public int hashCode() {
			return (type.hashCode() + key.hashCode()) % (2 << 15);
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			
			if (obj instanceof CacheKey) {
				CacheKey<T> keyObj = (CacheKey<T>) obj;
				
				if (key.equals(keyObj.key) && type.equals(keyObj.type)) {
					return true;
				}
			}
			
			return false;
		}
	}
}
