/*
 * Copyright 2008 Daniel Spiewak
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

import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import net.java.ao.Common;
import net.java.ao.EntityManager;
import net.java.ao.RawEntity;
import net.spy.memcached.MemcachedClient;

/**
 * Schemata:
 * 
 * prefix.from.throughType.toType = PK[]
 * prefix.entityClass = keyString[]
 * prefix.(pk.hashCode()).field = keyString[]
 * 
 * "keyString" refers to a Memcached key in the top schema
 * 
 * @author Daniel Spiewak
 */
class MemcachedRelationsCache implements RelationsCache {
	private final MemcachedClient client;
	private final int expiry;
	private final String prefix;
	
	MemcachedRelationsCache(MemcachedClient client, int expiry, String prefix) {
		this.client = client;
		this.expiry = expiry;
		this.prefix = prefix;
	}

	@Override
	public void flush() {
		System.err.println("ERROR: Flushing is currently unsupported by the Memcached relations cache");
	}

	@Override
	public <T extends RawEntity<K>, K> T[] get(RawEntity<?> from, Class<T> toType, Class<? extends RawEntity<?>> throughType, String[] fields) {
		String cacheKey = prefix + from.hashCode() + '.' + throughType.getName() + '.' + toType.getName();
		EntityManager entityManager = from.getEntityManager();
		
		K[] keys = (K[]) client.get(cacheKey);
		T[] back = (T[]) Array.newInstance(toType, keys.length);
		
		for (int i = 0; i < keys.length; i++) {
			back[i] = entityManager.get(toType, keys[i]);
		}
		
		return back;
	}

	@Override
	public void put(RawEntity<?> from, RawEntity<?>[] through, Class<? extends RawEntity<?>> throughType, RawEntity<?>[] to, Class<? extends RawEntity<?>> toType, String[] fields) {
		if (to.length == 0) {
			return;
		}
		
		assert through.length != to.length;
		
		String cacheKey = prefix + from.hashCode() + '.' + throughType.getName() + '.' + toType.getName();
		
		for (String field : fields) {
			cacheKey += '.' + field;
		}
		
		Object[] pk = new Object[to.length];
		for (int i = 0; i < pk.length; i++) {
			pk[i] = Common.getPrimaryKeyValue(to[i]);
		}
		
		client.add(cacheKey, expiry, pk);
		
		String typeMapKey = prefix + throughType.getName();
		
		Set<String> keys = (Set<String>) client.get(typeMapKey);
		if (keys == null) {
			keys = new HashSet<String>();
		} else {
			try {
				client.delete(typeMapKey).get();
			} catch (InterruptedException e) {
			} catch (ExecutionException e) {
			}
		}
		
		keys.add(cacheKey);
		client.add(typeMapKey, expiry, keys);

		for (String field : fields) {
			for (RawEntity<?> entity : through) {
				String metaCacheKey = prefix + Common.getPrimaryKeyValue(entity).hashCode() + '.' + field;
				
				keys = (Set<String>) client.get(metaCacheKey);
				if (keys == null) {
					keys = new HashSet<String>();
				} else {
					try {
						client.delete(metaCacheKey).get();
					} catch (InterruptedException e) {
					} catch (ExecutionException e) {
					}
				}
				
				client.add(metaCacheKey, expiry, keys);
			}
		}
	}

	@Override
	public void remove(Class<? extends RawEntity<?>>... types) {
		for (Class<? extends RawEntity<?>> type : types) {
			String typeMapKey = prefix + type.getName();
			Set<String> keys = (Set<String>) client.get(typeMapKey);
			if (keys != null) {
				for (String key : keys) {
					client.delete(key);
				}
	
				client.delete(typeMapKey);
			}
		}
	}

	@Override
	public void remove(RawEntity<?> entity, String[] fields) {
		for (String field : fields) {
			String metaCacheKey = prefix + Common.getPrimaryKeyValue(entity).hashCode() + '.' + field;
			
			Set<String> keys = (Set<String>) client.get(metaCacheKey);
			if (keys != null) {
				for (String key : keys) {
					client.delete(key);
				}
			}
		}
	}
}
