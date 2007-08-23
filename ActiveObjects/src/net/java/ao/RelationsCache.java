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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Daniel Spiewak
 */
class RelationsCache {
	private final Map<CacheKey, Entity[]> cache;
	private final ReadWriteLock lock;

	public RelationsCache() {
		cache = new HashMap<CacheKey, Entity[]>();
		lock = new ReentrantReadWriteLock();
	}

	public void put(Entity from, Entity[] to) {
		if (to.length == 0) {
			return;
		}
		
		CacheKey key = new CacheKey(from, to[0].getEntityType());
		lock.writeLock().lock();
		try {
			cache.put(key, to);
		} finally {
			lock.writeLock().unlock();
		}
	}

	public Entity[] get(Entity from, Class<? extends Entity> toType) {
		lock.readLock().lock();
		try {
			return cache.get(new CacheKey(from, toType));
		} finally {
			lock.readLock().unlock();
		}
	}

	public boolean contains(Entity from, Class<? extends Entity> toType) {
		lock.readLock().lock();
		try {
			return cache.containsKey(new CacheKey(from, toType));
		} finally {
			lock.readLock().unlock();
		}
	}

	private static class CacheKey {
		private Entity from;
		private Class<? extends Entity> toType;
		
		public CacheKey(Entity from, Class<? extends Entity> toType) {
			this.from = from;
			this.toType = toType;
		}

		public Entity getFrom() {
			return from;
		}

		public void setFrom(Entity from) {
			this.from = from;
		}

		public Class<? extends Entity> getToType() {
			return toType;
		}

		public void setToType(Class<? extends Entity> toType) {
			this.toType = toType;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof CacheKey) {
				CacheKey key = (CacheKey) obj;
				
				if (key.getFrom() != null) {
					if (!key.getFrom().equals(from)) {
						return false;
					}
				}
				if (key.getToType() != null) {
					if (!key.getToType().equals(toType)) {
						return false;
					}
				}
				
				return true;
			}
			
			return super.equals(obj);
		}
		
		@Override
		public int hashCode() {
			int hashCode = 0;
			
			if (from != null) {
				hashCode += from.hashCode();
			}
			if (toType != null) {
				hashCode += toType.hashCode();
			}
			
			return hashCode;
		}
	}
}
