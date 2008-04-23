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

import java.util.HashMap;
import java.util.Map;

import net.java.ao.RawEntity;

/**
 * <p>Superinterface for classes which manage the cache semantics for
 * specific entity values.  Implementations of this interface are what
 * actually handle the meat of the cache operation, using resources
 * allocated within the corresponding {@link Cache} implementation.</p>
 * 
 * <p>The basic cache model is that of the {@link Map} interface (which
 * is logical as ActiveObjects was originally cached with a conventional
 * {@link HashMap}).  Values are stored in the cache based on 
 * <code>String</code> keys.  These values <i>must be typed</i> within
 * the cache and returned in their proper type, up-cast to
 * <code>Object</code>.  This is extremely important since AO will
 * assume that values can be cast to their appropriate types within
 * code which uses the cache.  Thus, any cache implementation must
 * also store value types in its implementation.  It is also critical
 * that any cache implementation be able to store <code>null</code>
 * values.  Technically, <code>null</code> values <i>may</i> be
 * dropped for non-dirty fields, but this will lead to decreased
 * performance in the entity accessor algorithm.  Null keys need not
 * be accepted.</p>
 * 
 * <p>Cache bindings themselves need not be persistent.  For example,
 * it is strongly encouraged to implement value expiry on implementations
 * with a distributed backend.  Likewise, local caches may take into
 * account finite resources (such as memory).  There is nothing in the
 * ActiveObjects core implementation which assumes that bindings will
 * be persistent for any length of time.  In fact, it is technically
 * permissible to implement a cache layer which does not cache at all,
 * but simply responds appropriately when invoked.  Note that while
 * this is certainly possible, it would be quite detrimental to
 * performance.  Implementations should make the utmost effort to
 * ensure that key,value bindings persist for as long as possible. The
 * values themselves will never become stale (out of sync with the DB)
 * unless the cache is being accessed by multiple instnaces of AO.</p>
 * 
 * <p>It is important to note that <i>all</i> of the methods in the
 * interface must be implemented for ActiveObjects to function
 * properly.  A few of the methods may be omitted without affecting
 * critical functionality, but this should not be assumed.  This fact
 * is especially important to keep in mind when working with limitted
 * cache backends (such as memcached).  By whatever means necessary,
 * the contract must be fullfilled.</p>
 * 
 * <p>Concurrency must be considered for all implementations, but
 * especially those with a distributed backend.  ActiveObjects does
 * not synchronize calls to the cache, that task is left up to the
 * cache implementation itself.  Due to the fact that entire code
 * blocks are not locked against the cache, ActiveObjects is
 * inherantly prone to race conditions and stale data in distributed
 * caches.  This is a design decision which may change in future,
 * but for now it remains due to performance and stability concerns.
 * For this reason, caches with distributed backends (such as
 * memcached) are encouraged to set a sufficiently short default
 * timeout to allow invalid data to simply expire naturally.</p>
 * 
 * <p>Except in odd cases, <code>CacheLayer</code> implementations
 * should not manage volatile resources themselves.  All of this
 * work should be handled within the corresponding <code>ValueCache</code>
 * implementation.  The general rule of thumb is that the value
 * cache handles the "what" while the cache layer manages the "how".</p>
 * 
 * @author Daniel Spiewak
 * @see Cache
 */
public interface CacheLayer {
	
	/**
	 * Stores a typed value in the cache, indexed by the given
	 * <code>String</code> key.  If the key is already assigned, its
	 * value should be overwritten.
	 * 
	 * @param field	The key which should be assigned to the given value.
	 * @param value	The value to be stored (may be <code>null</code>).
	 */
	public void put(String field, Object value);
	
	/**
	 * Retrieves a typed value from the cache based on the given
	 * <code>String</code> key.  Even with a non-native cache backend,
	 * all values returned from the <code>get(String)</code> method
	 * must be of the same type with which they were stored.  If this
	 * contract is not observed, a {@link ClassCastException} will be
	 * thrown on some entity operations.
	 * 
	 * @param field	The key for which the corresponding value should be retrieved.
	 * @return	The value which corresponds to the given key, or
	 * 		<code>null</code> if no binding is found (or if the value
	 * 		itself is <code>null</code>).
	 */
	public Object get(String field);
	
	/**
	 * Removes the binding for a specified key, deleting the association
	 * from the cache.  From an implementation standpoint, the cache itself
	 * need not discard the value when its binding is removed, it must
	 * simply return <code>null</code> for any future calls to {@link #get(String)}
	 * on that key, as well as <code>false</code> for any calls to
	 * {@link #contains(String)}.
	 * 
	 * @param field	The key which indexes the binding to be removed.
	 */
	public void remove(String field);
	
	/**
	 * Determines if a binding is present which corresponds to the given
	 * key.  Note that this method cannot simply check for a <code>null</code>
	 * value as ActiveObjects may store <code>null</code>s within the
	 * cache.  If this method is inconsistent in its implementation, very
	 * strange things will happen on <i>all</i> entity calls.
	 * 
	 * @param field	The key for which a binding may be found.
	 * @return	<code>true</code> if the cache contains a binding for the
	 * 		given key, <code>false</code> otherwise.
	 */
	public boolean contains(String field);
	
	/**
	 * Removes all bindings, effectively flushing the cache.  As with
	 * {@link #remove(String)}, values need not actually be deleted, but
	 * the bindings must become void.
	 */
	public void clear();
	
	/**
	 * Adds a given field to the set of dirty fields (fields which have
	 * been modified without being persisted back to the DB).  At the
	 * very least, this method must append the field to the set of dirty
	 * fields (as returned from {@link #getDirtyFields()}).  However,
	 * secondary actions are also permitted (such as flagging the field
	 * for a preemptive, local cache).
	 * 
	 * @param field	The field which has been locally modified.
	 */
	public void markDirty(String field);
	
	/**
	 * Retrieves the set of all dirty fields in no particular order.
	 * 
	 * @return	The set of all fields which have been locally modified.
	 * @see #markDirty(String)
	 */
	public String[] getDirtyFields();
	
	/**
	 * Determines if a given field is in the set of dirty fields (has
	 * been locally modified).  Correct implementation of this method
	 * is critical to the functionality of the {@link RawEntity#save()}
	 * method.
	 * 
	 * @param field	The field which may be dirty.
	 * @return	<code>true</code> if the field is dirty, <code>false</code>
	 * 		otherwise.
	 */
	public boolean dirtyContains(String field);
	
	/**
	 * Removes all fields from the dirty set, implicitly marking all fields
	 * as "in sync" with the database.  As with the {@link #markDirty(String)}
	 * method, the implementation must at the minimum clear the set of all
	 * dirty fields.  However, other side effects (like resyncing with a
	 * distributed cache) may also be implemented.
	 */
	public void clearDirty();
	
	public void markToFlush(Class<? extends RawEntity<?>> type);
	public Class<? extends RawEntity<?>>[] getToFlush();
	
	public void clearFlush();
}
