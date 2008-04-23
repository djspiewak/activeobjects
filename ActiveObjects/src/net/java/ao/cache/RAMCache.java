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

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.java.ao.RawEntity;

/**
 * @author Daniel Spiewak
 */
public class RAMCache implements Cache {
	private Map<RawEntity<?>, CacheLayer> cacheLayer;
	private final ReadWriteLock cacheLayerLock = new ReentrantReadWriteLock();
	
	private final RAMRelationsCache relationsCache;
	
	public RAMCache() {
		cacheLayer = new WeakHashMap<RawEntity<?>, CacheLayer>();
		relationsCache = new RAMRelationsCache();
	}

	public CacheLayer getCacheLayer(RawEntity<?> entity) {
		cacheLayerLock.writeLock().lock();
		try {
			if (cacheLayer.containsKey(entity)) {
				cacheLayerLock.readLock().lock();
				cacheLayerLock.writeLock().unlock();
				
				try {
					return cacheLayer.get(entity);
				} finally {
					cacheLayerLock.readLock().unlock();
				}
			}
			
			CacheLayer layer = new RAMCacheLayer();
			cacheLayer.put(entity, layer);
			
			return layer;
		} finally {
			try {
				cacheLayerLock.writeLock().unlock();
			} catch (Throwable t) {}	// may not actually be locked
		}
	}
	
	public void dispose() {
	}

	public RelationsCache getRelationsCache() {
		return relationsCache;
	}
}
