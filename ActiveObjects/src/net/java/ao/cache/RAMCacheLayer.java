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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.java.ao.RawEntity;

/**
 * @author Daniel Spiewak
 */
class RAMCacheLayer implements CacheLayer {
	private Map<String, Object> values;
	private final ReadWriteLock valueLock = new ReentrantReadWriteLock();
	
	private Set<String> dirty;
	private final ReadWriteLock dirtyLock = new ReentrantReadWriteLock();
	
	private Set<Class<? extends RawEntity<?>>> flush;
	private final ReadWriteLock flushLock = new ReentrantReadWriteLock();
	
	RAMCacheLayer() {
		values = new HashMap<String, Object>();
		dirty = new HashSet<String>();
		flush = new HashSet<Class<? extends RawEntity<?>>>();
	}

	public void clear() {
		dirtyLock.readLock().lock();
		valueLock.writeLock().lock();
		try {
			Set<String> toRemove = new HashSet<String>();
			for (String field : values.keySet()) {
				if (!dirty.contains(field)) {
					toRemove.add(field);
				}
			}
			
			for (String field : toRemove) {
				values.remove(field);
			}
		} finally {
			valueLock.writeLock().unlock();
			dirtyLock.readLock().unlock();
		}
	}

	public void clearDirty() {
		dirtyLock.writeLock().lock();
		try {
			dirty.clear();
		} finally {
			dirtyLock.writeLock().unlock();
		}
	
	}

	public void clearFlush() {
		flushLock.writeLock().lock();
		try {
			flush.clear();
		} finally {
			flushLock.writeLock().unlock();
		}
	}

	public boolean contains(String field) {
		valueLock.readLock().lock();
		try {
			return values.containsKey(field.toLowerCase());
		} finally {
			valueLock.readLock().unlock();
		}
	}
	
	public boolean dirtyContains(String field) {
		dirtyLock.readLock().lock();
		try {
			return dirty.contains(field);
		} finally {
			dirtyLock.readLock().unlock();
		}
	}

	public Object get(String field) {
		valueLock.readLock().lock();
		try {
			return values.get(field.toLowerCase());
		} finally {
			valueLock.readLock().unlock();
		}
	}

	public String[] getDirtyFields() {
		dirtyLock.readLock().lock();
		try {
			return dirty.toArray(new String[dirty.size()]);
		} finally {
			dirtyLock.readLock().unlock();
		}
	}

	public Class<? extends RawEntity<?>>[] getToFlush() {
		flushLock.readLock().lock();
		try {
			return flush.toArray(new Class[flush.size()]);
		} finally {
			flushLock.readLock().unlock();
		}
	}

	public void markDirty(String field) {
		dirtyLock.writeLock().lock();
		try {
			dirty.add(field.toLowerCase());
		} finally {
			dirtyLock.writeLock().unlock();
		}
	}

	public void markToFlush(Class<? extends RawEntity<?>> type) {
		flushLock.writeLock().lock();
		try {
			flush.add(type);
		} finally {
			flushLock.writeLock().unlock();
		}
	}

	public void put(String field, Object value) {
		valueLock.writeLock().lock();
		try {
			values.put(field.toLowerCase(), value);
		} finally {
			valueLock.writeLock().unlock();
		}
	}
	
	public void remove(String field) {
		valueLock.writeLock().lock();
		try {
			values.remove(field.toLowerCase());
		} finally {
			valueLock.writeLock().unlock();
		}
	}
}
