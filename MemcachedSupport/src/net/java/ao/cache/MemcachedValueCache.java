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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.java.ao.Common;
import net.java.ao.RawEntity;
import net.spy.memcached.MemcachedClient;

/**
 * @author Daniel Spiewak
 */
public class MemcachedValueCache implements ValueCache {
	private Map<RawEntity<?>, CacheLayer> cache;
	private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();
	
	private final MemcachedClient client;
	
	public MemcachedValueCache(InetSocketAddress... servers) throws IOException {
		this(new MemcachedClient(servers));
	}
	
	public MemcachedValueCache(MemcachedClient client) {
		cache = new WeakHashMap<RawEntity<?>, CacheLayer>();
		
		this.client = client;
	}
	
	public MemcachedClient getClient() {
		return client;
	}

	public CacheLayer getCacheLayer(RawEntity<?> entity) {
		cacheLock.writeLock().lock();
		try {
			if (cache.containsKey(entity)) {
				cacheLock.readLock().lock();
				cacheLock.writeLock().unlock();
				
				try {
					return cache.get(entity);
				} finally {
					cacheLock.readLock().unlock();
				}
			} else {
				String prefix = "activeobjects.";
				prefix += entity.getEntityManager().getTableNameConverter().getName(entity.getEntityType()) + '.';
				prefix += Common.getPrimaryKeyType(entity.getEntityType()).valueToString(
						Common.getPrimaryKeyValue(entity)) + '.';
				
				CacheLayer layer = new MemcachedCacheLayer(client, prefix);
				cache.put(entity, layer);
				
				return layer;
			}
		} finally {
			try {
				cacheLock.writeLock().unlock();
			} catch (Throwable t) {}	// may not actually be locked
		}
	}
	
	public void dispose() {
		client.shutdown(10, TimeUnit.SECONDS);
	}
}
