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
package net.java.ao.cache;

import net.java.ao.EntityManager;
import net.java.ao.RawEntity;

/**
 * <p>The super-interface for all value cache implementations.  Implementations of
 * this interface create an instance of {@link CacheLayer} for a given
 * {@link RawEntity}.  The design is such that any implementation of this interface
 * acts as the primary controller for the cache, delegating entity-specific tasks
 * to a custom implementation of <code>CacheLayer</code>.  Any resources which are
 * associated with a cache should be managed within the implementation of this
 * interface.</p>
 * 
 * <p>An example design would be for caching in a database (a patently useless
 * operation, given the function of the framework).  In this cache, the database
 * itself would be managed in the implementation of <code>ValueCache</code>.  This
 * class would then instantiate instances of a custom <code>CacheLayer</code>
 * implementation which would handle the actual act of caching data for specific
 * <code>RawEntity</code> instances (most likely each corresponding with a single
 * row).  By separating these implementations, it is possible keep resources
 * handled in a central controller while still devoting distinct memory space to
 * specific entities.  The distinct memory is most important in the default
 * implementation, {@link RAMCache}.</p>
 * 
 * <p>Generically, implementations of <code>ValueCache</code> function as a
 * factory and controller for the relevant implementation of <code>CacheLayer</code>.
 * The primary purpose is to construct new instances of the implementation-specific
 * <code>CacheLayer</code> and to manage resources common to all constructed
 * instances.</p>
 * 
 * @author Daniel Spiewak
 * @see #getCacheLayer(RawEntity)
 * @see EntityManager#setCache(Cache)
 * @see EntityManager#getCache()
 */
public interface Cache {
	
	/**
	 * <p>Retrieves a <code>CacheLayer</code> instance which corresponds to the given
	 * entity.  <code>CacheLayer</code> instance(s) may themselves be cached within
	 * the value cache, but this is not part of the specification.  Generically,
	 * this method should return an instance of a working <code>CacheLayer</code> 
	 * implementation which can persist values relevant to the given entity in an
	 * implementation-specific way.  For example, the default implementation of this
	 * interface ({@link RAMCache}) returns a <code>CacheLayer</code> which can
	 * cache values in memory for a specific entity.</p>
	 * 
	 * <p>Typically, instances of the <i>same</i> implementation are returned for any
	 * given entity (e.g. <code>RAMCacheLayer</code> always returns an instance of
	 * <code>RAMValueCache</code>, regardless of the entity in question).  It is
	 * theoretically possible however to differentiate layers based on entity type
	 * or even specific primary key value.  I'm not sure why you would want to do
	 * this, but there's nothing in the requirements which would prevent it.  Code
	 * which uses the result of this method should certainly be written to the
	 * interface, rather than any specific implementation.</p>
	 * 
	 * @param entity	The entity to which the cache layer should correspond.
	 * @return	A layer which will handle caching of values as necessary for the
	 * 		given entity.
	 */
	public CacheLayer getCacheLayer(RawEntity<?> entity);
	
	public RelationsCache getRelationsCache();
	
	/**
	 * Frees all resources associated with the cache.  This should be used to
	 * handle things like closing connections, freeing result-sets, etc.  This
	 * method will be called by {@link EntityManager} on the old <code>ValueCache</code>
	 * when a new instance is specified.
	 * 
	 * @see EntityManager#setCache(Cache)
	 */
	public void dispose();
}
