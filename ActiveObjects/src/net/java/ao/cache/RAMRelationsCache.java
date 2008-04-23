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

import java.util.Arrays;
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
public class RAMRelationsCache implements RelationsCache {
	private final Map<CacheKey, RawEntity<?>[]> cache;
	private final Map<Class<? extends RawEntity<?>>, Set<CacheKey>> typeMap;
	private final Map<MetaCacheKey, Set<CacheKey>> fieldMap;
	private final ReadWriteLock lock;

	public RAMRelationsCache() {
		cache = new HashMap<CacheKey, RawEntity<?>[]>();
		typeMap = new HashMap<Class<? extends RawEntity<?>>, Set<CacheKey>>();
		fieldMap = new HashMap<MetaCacheKey, Set<CacheKey>>();
		
		lock = new ReentrantReadWriteLock();
	}
	
	public void flush() {
		lock.writeLock().lock();
		try {
			cache.clear();
			typeMap.clear();
			fieldMap.clear();
		} finally {
			lock.writeLock().unlock();
		}
	}

	public void put(RawEntity<?> from, RawEntity<?>[] through, Class<? extends RawEntity<?>> throughType, RawEntity<?>[] to, Class<? extends RawEntity<?>> toType, String[] fields) {
		if (to.length == 0) {
			return;
		}
		
		assert through.length != to.length;
		
		CacheKey key = new CacheKey(from, toType, throughType, fields);
		lock.writeLock().lock();
		try {
			cache.put(key, to);
			
			Set<CacheKey> keys = typeMap.get(key.getThroughType());
			if (keys == null) {
				keys = new HashSet<CacheKey>();
				typeMap.put(key.getThroughType(), keys);
			}
			keys.add(key);
			
			for (String field : fields) {
				for (RawEntity<?> entity : through) {
					MetaCacheKey metaKey = new MetaCacheKey(entity, field);
					
					keys = fieldMap.get(metaKey);
					if (keys == null) {
						keys = new HashSet<CacheKey>();
						fieldMap.put(metaKey, keys);
					}
					keys.add(key);
				}
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	public <T extends RawEntity<?>> T[] get(RawEntity<?> from, Class<T> toType, 
			Class<? extends RawEntity<?>> throughType, String[] fields) {
		lock.readLock().lock();
		try {
			return (T[]) cache.get(new CacheKey(from, toType, throughType, fields));
		} finally {
			lock.readLock().unlock();
		}
	}
	
	public void remove(Class<? extends RawEntity<?>>... types) {
		lock.writeLock().lock();
		try {
			for (Class<? extends RawEntity<?>> type : types) {
				Set<CacheKey> keys = typeMap.get(type);
				if (keys != null) {
					for (CacheKey key : keys) {
						cache.remove(key);
					}
		
					typeMap.remove(type);
				}
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	public void remove(RawEntity<?> entity, String[] fields) {
		lock.writeLock().tryLock();
		try {
			for (String field : fields) {
				Set<CacheKey> keys = fieldMap.get(new MetaCacheKey(entity, field));
				if (keys != null) {
					for (CacheKey key : keys) {
						cache.remove(key);
					}
				}
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	private static class CacheKey {
		private RawEntity<?> from;
		private Class<? extends RawEntity<?>> toType;
		private Class<? extends RawEntity<?>> throughType;
		
		private String[] fields;
		
		public CacheKey(RawEntity<?> from, Class<? extends RawEntity<?>> toType, 
				Class<? extends RawEntity<?>> throughType, String[] fields) {
			this.from = from;
			this.toType = toType;
			this.throughType = throughType;
			
			setFields(fields);
		}

		public RawEntity<?> getFrom() {
			return from;
		}

		public void setFrom(RawEntity<?> from) {
			this.from = from;
		}

		public Class<? extends RawEntity<?>> getToType() {
			return toType;
		}

		public void setToType(Class<? extends RawEntity<?>> toType) {
			this.toType = toType;
		}

		public String[] getFields() {
			return fields;
		}

		public void setFields(String[] fields) {
			for (int i = 0; i < fields.length; i++) {
				fields[i] = fields[i].toLowerCase();
			}
			
			Arrays.sort(fields);
			
			this.fields = fields;
		}
		
		public Class<? extends RawEntity<?>> getThroughType() {
			return throughType;
		}
		
		public void setThroughType(Class<? extends RawEntity<?>> throughType) {
			this.throughType = throughType;
		}
		
		@Override
		public String toString() {
			return '(' + from.toString() + "; to=" + toType.getName() + "; through=" + throughType.getName() + "; " + Arrays.toString(fields) + ')';
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
					if (!key.getToType().getName().equals(toType.getName())) {
						return false;
					}
				}
				if (key.getThroughType() != null) {
					if (!key.getThroughType().getName().equals(throughType.getName())) {
						return false;
					}
				}
				if (key.getFields() != null) {
					if (!Arrays.equals(key.getFields(), fields)) {
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
				hashCode += toType.getName().hashCode();
			}
			if (throughType != null) {
				hashCode += throughType.getName().hashCode();
			}
			if (fields != null) {
				for (String field : fields) {
					hashCode += field.hashCode();
				}
			}
			hashCode %= 2 << 10;
			
			return hashCode;
		}
	}
	
	private static class MetaCacheKey {
		private RawEntity<?> entity;
		private String field;
		
		public MetaCacheKey(RawEntity<?> entity, String field) {
			this.entity = entity;
			
			setField(field);
		}

		public RawEntity<?> getEntity() {
			return entity;
		}

		public void setEntity(RawEntity<?> entity) {
			this.entity = entity;
		}

		public String getField() {
			return field;
		}

		public void setField(String field) {
			this.field = field.toLowerCase();
		}
		
		@Override
		public String toString() {
			return entity.toString() + "; " + field;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof MetaCacheKey) {
				MetaCacheKey key = (MetaCacheKey) obj;
				
				if (key.getEntity() != null) {
					if (!key.getEntity().equals(entity)) {
						return false;
					}
				}
				if (key.getField() != null) {
					if (!key.getField().equals(field)) {
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
			
			if (entity != null) {
				hashCode += entity.hashCode();
			}
			if (field != null) {
				hashCode += field.hashCode();
			}
			hashCode %= 2 << 15;
			
			return hashCode;
		}
	}
}
