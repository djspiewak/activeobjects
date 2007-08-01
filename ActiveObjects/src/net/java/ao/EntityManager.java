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
import java.lang.reflect.Proxy;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.java.ao.schema.CamelCaseNameConverter;
import net.java.ao.schema.Generator;
import net.java.ao.schema.PluggableNameConverter;

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
	
	private Map<Entity, EntityProxy<? extends Entity>> proxies;
	private final ReadWriteLock proxyLock = new ReentrantReadWriteLock();
	
	private Map<CacheKey, Entity> cache;
	private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();
	
	private PluggableNameConverter nameConverter;
	private final ReadWriteLock nameConverterLock = new ReentrantReadWriteLock();
	
	private RSCachingStrategy rsStrategy;
	private final ReadWriteLock rsStrategyLock = new ReentrantReadWriteLock();
	
	/**
	 * Creates a new instance of <code>EntityManager</code> using the specified
	 * {@link DatabaseProvider}.  This constructor intializes the entity cache, as well
	 * as creates the default {@link PluggableNameConverter} (the default is 
	 * {@link CamelCaseNameConverter}, which is non-pluralized).  The provider
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
			proxies = new WeakHashMap<Entity, EntityProxy<? extends Entity>>();
			cache = new WeakHashMap<CacheKey, Entity>();
		} else {
			proxies = new SoftHashMap<Entity, EntityProxy<? extends Entity>>();
			cache = new SoftHashMap<CacheKey, Entity>();
		}
		
		nameConverter = new CamelCaseNameConverter();
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
	 *  @see net.java.ao.schema.Generator#migrate(DatabaseProvider, PluggableNameConverter, Class...)
	 */
	public void migrate(Class<? extends Entity>... entities) throws SQLException {
		nameConverterLock.readLock().lock();
		try {
			Generator.migrate(provider, nameConverter, entities);
		} finally {
			nameConverterLock.readLock().unlock();
		}
	}
	
	/**
	 * First checks to see if the schema for the specified entities pre-exists in
	 * the database abstracted by the current settings.  If the schema does not
	 * exist, then the {@link #migrate(Class...)} method is invoked.
	 * 
	 * @see net.java.ao.schema.Generator#hasSchema(DatabaseProvider, PluggableNameConverter, Class...)
	 */
	@Deprecated
	public boolean conditionallyMigrate(Class<? extends Entity>... entities) throws SQLException {
		migrate(entities);		// TODO	remove
		
		return false;
	}
	
	public void flushAll() {
		proxyLock.readLock().lock();
		try {
			for (EntityProxy<? extends Entity> proxy : proxies.values()) {
				proxy.flushCache();
			}
		} finally {
			proxyLock.readLock().unlock();
		}
	}
	
	public void flush(Entity... entities) {
		proxyLock.readLock().lock();
		try {
			for (Entity entity : entities) {
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
				
				back[index++] = getAndInstantiate(type, id);
			} finally {
				cacheLock.writeLock().unlock();
			}
		}
		
		return back;
	}
	
	// assumes cache doesn't contain object
	protected <T extends Entity> T getAndInstantiate(Class<T> type, int id) {
		EntityProxy<T> proxy = new EntityProxy<T>(this, type);
		
		T entity = (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[] {type}, proxy);
		entity.setID(id);

		proxyLock.writeLock().lock();
		try {
			proxies.put(entity, proxy);
		} finally {
			proxyLock.writeLock().unlock();
		}
		
		cache.put(new CacheKey(id, type), entity);
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
	public <T extends Entity> T get(Class<T> type, int id) {
		return get(type, new int[] {id})[0];
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
	public <T extends Entity> T create(Class<T> type, DBParam... params) throws SQLException {
		T back = null;
		String table = null;
		
		nameConverterLock.readLock().lock();
		try {
			table = nameConverter.getName(type);
		} finally {
			nameConverterLock.readLock().unlock();
		}
		
		Connection conn = DBEncapsulator.getInstance(provider).getConnection();
		try {
			back = get(type, provider.insertReturningKeys(conn, table, params));
		} finally {
			DBEncapsulator.getInstance(provider).closeConnection(conn);
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
	public void delete(Entity... entities) throws SQLException {
		if (entities.length == 0) {
			return;
		}
		
		Map<Class<? extends Entity>, List<Entity>> organizedEntities = new HashMap<Class<? extends Entity>, List<Entity>>();
		
		for (Entity entity : entities) {
			Class<? extends Entity> type = getProxyForEntity(entity).getType(); 
			
			if (!organizedEntities.containsKey(type)) {
				organizedEntities.put(type, new LinkedList<Entity>());
			}
			organizedEntities.get(type).add(entity);
		}
		
		cacheLock.writeLock().lock();
		try {
			Connection conn = DBEncapsulator.getInstance(provider).getConnection();
			try {
				for (Class<? extends Entity> type : organizedEntities.keySet()) {
					List<Entity> entityList = organizedEntities.get(type);
					
					StringBuilder sql = new StringBuilder("DELETE FROM ");
					
					nameConverterLock.readLock().lock();
					try {
						sql.append(nameConverter.getName(type));
					} finally {
						nameConverterLock.readLock().unlock();
					}
					
					sql.append(" WHERE id IN (?");
					
					for (int i = 1; i < entityList.size(); i++) {
						sql.append(",?");
					}
					sql.append(')');
					
					Logger.getLogger("net.java.ao").log(Level.INFO, sql.toString());
					PreparedStatement stmt = conn.prepareStatement(sql.toString());
					
					int index = 1;
					for (Entity entity : entityList) {
						stmt.setInt(index++, entity.getID());
					}
					
					stmt.executeUpdate();
					stmt.close();
				}
			} finally {
				DBEncapsulator.getInstance(provider).closeConnection(conn);
			}
			
			for (Entity entity : entities) {
				cache.remove(new CacheKey(entity.getID(), entity.getEntityType()));
			}
			
			proxyLock.writeLock().lock();
			try {
				for (Entity entity : entities) {
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
	public <T extends Entity> T[] find(Class<T> type) throws SQLException {
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
	public <T extends Entity> T[] find(Class<T> type, String criteria, Object... parameters) throws SQLException {
		return find(type, Query.select().where(criteria, parameters));
	}
	
	public <T extends Entity> T[] find(Class<T> type, Query query) throws SQLException {
		String selectField = "id";
		
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
	public <T extends Entity> T[] find(Class<T> type, String field, Query query) throws SQLException {
		List<T> back = new ArrayList<T>();
		
		Connection conn = DBEncapsulator.getInstance(provider).getConnection();
		try {
			String sql = null;
			nameConverterLock.readLock().lock();
			try {
				sql = query.toSQL(type, this, false);
			} finally {
				nameConverterLock.readLock().unlock();
			}
			
			Logger.getLogger("net.java.ao").log(Level.INFO, sql);
			PreparedStatement stmt = conn.prepareStatement(sql);
			provider.setQueryStatementProperties(stmt, query);
			
			query.setParameters(stmt);

			ResultSet res = stmt.executeQuery();
			while (res.next()) {
				T entity = get(type, res.getInt(field));
				
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
			DBEncapsulator.getInstance(provider).closeConnection(conn);
		}
		
		return back.toArray((T[]) Array.newInstance(type, back.size()));
	}
	
	/**
	 * Executes the specified SQL and extracts the given idfield, wrapping each
	 * row into a instance of the specified type.  The SQL itself is executed as 
	 * a PreparedStatement with the given parameters. 
	 */
	public <T extends Entity> T[] findWithSQL(Class<T> type, String idField, String sql, Object... parameters) throws SQLException {
		List<T> back = new ArrayList<T>();
		
		Connection conn = DBEncapsulator.getInstance(provider).getConnection();
		try {
			Logger.getLogger("net.java.ao").log(Level.INFO, sql);
			PreparedStatement stmt = conn.prepareStatement(sql);
			
			for (int i = 0; i < parameters.length; i++) {
				if (parameters[i] instanceof Entity) {
					parameters[i] = ((Entity) parameters[i]).getID();
				} else if (parameters[i] instanceof URL) {
					parameters[i] = parameters[i].toString();
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
	
	/**
	 * Counts all entities of the specified type.  This method is actually
	 * a delegate for the <code>count(Class&lt;? extends Entity&gt;, Query)</code>
	 * method.
	 */
	public int count(Class<? extends Entity> type) throws SQLException {
		return count(type, Query.select());
	}
	
	/**
	 * Counts all entities of the specified type matching the given criteria
	 * and parameters.  This is a convenience method for:
	 * 
	 * <code>count(type, Query.select().where(criteria, parameters))</code>
	 */
	public int count(Class<? extends Entity> type, String criteria, Object... parameters) throws SQLException {
		return count(type, Query.select().where(criteria, parameters));
	}
	
	/**
	 * Counts all entities of the specified type matching the given {@link Query}
	 * instance.  The SQL runs as a <code>SELECT COUNT(*)</code> to
	 * ensure maximum performance.
	 */
	public int count(Class<? extends Entity> type, Query query) throws SQLException {
		int back = -1;
		
		Connection conn = DBEncapsulator.getInstance(provider).getConnection();
		try {
			String sql = null;
			nameConverterLock.readLock().lock();
			try {
				sql = query.toSQL(type, this, true);
			} finally {
				nameConverterLock.readLock().unlock();
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
			DBEncapsulator.getInstance(provider).closeConnection(conn);
		}
		
		return back;
	}
	
	/**
	 * <p>Specifies the {@link PluggableNameConverter} instance to use for
	 * name conversion of all entity types.  Name conversion is the process
	 * of determining the appropriate table name from an arbitrary {@link Entity}
	 * class.</p>
	 * 
	 * <p>The default nameConverter is {@link CamelCaseNameConverter}.</p>
	 */
	public void setNameConverter(PluggableNameConverter nameConverter) {
		nameConverterLock.writeLock().lock();
		try {
			this.nameConverter = nameConverter;
		} finally {
			nameConverterLock.writeLock().unlock();
		}
	}
	
	/**
	 * Retrieves the {@link PluggableNameConverter} instance used for name
	 * conversion of all entity types.
	 * 
	 * @see #setNameConverter(PluggableNameConverter)
	 */
	public PluggableNameConverter getNameConverter() {
		nameConverterLock.readLock().lock();
		try {
			return nameConverter;
		} finally {
			nameConverterLock.readLock().unlock();
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

	protected <T extends Entity> EntityProxy<T> getProxyForEntity(T entity) {
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
