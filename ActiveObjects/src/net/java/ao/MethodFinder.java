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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Daniel Spiewak
 */
final class MethodFinder {
	private static MethodFinder instance;
	
	private Map<CacheKey, Method[]> cache;
	private final ReadWriteLock cacheLock;
	
	private MethodFinder() {
		cache = new HashMap<CacheKey, Method[]>();
		cacheLock = new ReentrantReadWriteLock();
	}
	
	public Method[] findAnnotation(Class<? extends Annotation> annotation, Class<? extends RawEntity> clazz) {
		CacheKey key = new CacheKey(annotation, clazz);
		
		cacheLock.writeLock().lock();
		try {
			if (cache.containsKey(key)) {
				return cache.get(key);
			}
			
			List<Method> back = new ArrayList<Method>();
			for (Method method : clazz.getMethods()) {
				if (method.getAnnotation(annotation) != null) {
					back.add(method);
				}
			}
			
			Method[] array = back.toArray(new Method[back.size()]);
			cache.put(key, array);
			
			return array;
		} finally {
			cacheLock.writeLock().unlock();
		}
	}
	
	public static synchronized MethodFinder getInstance() {
		if (instance == null) {
			instance = new MethodFinder();
		}
		
		return instance;
	}
	
	private static final class CacheKey {
		private Class<? extends Annotation> annotation;
		private Class<? extends RawEntity> type;
		
		public CacheKey(Class<? extends Annotation> annotation, Class<? extends RawEntity> type) {
			this.annotation = annotation;
			this.type = type;
		}

		public Class<? extends Annotation> getAnnotation() {
			return annotation;
		}

		public void setAnnotation(Class<? extends Annotation> annotation) {
			this.annotation = annotation;
		}

		public Class<? extends RawEntity> getType() {
			return type;
		}

		public void setType(Class<? extends RawEntity> type) {
			this.type = type;
		}
		
		@Override
		public int hashCode() {
			int back = 0;
			
			if (annotation != null) {
				back += annotation.hashCode();
			}
			if (type != null) {
				back += type.hashCode();
			}
			back %= 2 << 15;
			
			return back;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof CacheKey) {
				CacheKey key = (CacheKey) obj;
				
				if (key.getAnnotation().equals(annotation) && key.getType().equals(type)) {
					return true;
				}
				
				return false;
			}
			
			return super.equals(obj);
		}
	}
}
