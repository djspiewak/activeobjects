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

import net.java.ao.schema.FieldNameConverter;

/**
 * @author Daniel Spiewak
 */
final class MethodFinder {
	private static MethodFinder instance;
	
	private Map<AnnotationCacheKey, Method[]> annotationCache;
	private final ReadWriteLock annotationCacheLock;
	
	private Map<CounterpartCacheKey, Method> counterpartCache;
	private final ReadWriteLock counterpartCacheLock;
	
	private MethodFinder() {
		annotationCache = new HashMap<AnnotationCacheKey, Method[]>();
		annotationCacheLock = new ReentrantReadWriteLock();
		
		counterpartCache = new HashMap<CounterpartCacheKey, Method>();
		counterpartCacheLock = new ReentrantReadWriteLock();
	}
	
	public Method[] findAnnotation(Class<? extends Annotation> annotation, Class<? extends RawEntity<?>> clazz) {
		AnnotationCacheKey key = new AnnotationCacheKey(annotation, clazz);
		
		annotationCacheLock.writeLock().lock();
		try {
			if (annotationCache.containsKey(key)) {
				return annotationCache.get(key);
			}
			
			List<Method> back = new ArrayList<Method>();
			for (Method method : clazz.getMethods()) {
				if (method.getAnnotation(annotation) != null) {
					back.add(method);
				}
			}
			
			Method[] array = back.toArray(new Method[back.size()]);
			annotationCache.put(key, array);
			
			return array;
		} finally {
			annotationCacheLock.writeLock().unlock();
		}
	}
	
	public Method findCounterpart(FieldNameConverter converter, Method method) {
		CounterpartCacheKey key = new CounterpartCacheKey(converter, method);
		String name = converter.getName(method);
		
		counterpartCacheLock.writeLock().lock();
		try {
			if (counterpartCache.containsKey(key)) {
				return counterpartCache.get(key);
			}
			
			Class<?> clazz = method.getDeclaringClass();
			for (Method other : clazz.getMethods()) {
				String otherName = converter.getName(other);
				if (!other.equals(method) && otherName != null && otherName.equals(name)) {
					counterpartCache.put(key, other);
					return other;
				}
			}
		} finally {
			counterpartCacheLock.writeLock().unlock();
		}
		
		return null;
	}
	
	public static synchronized MethodFinder getInstance() {
		if (instance == null) {
			instance = new MethodFinder();
		}
		
		return instance;
	}
	
	private static final class AnnotationCacheKey {
		private Class<? extends Annotation> annotation;
		private Class<? extends RawEntity<?>> type;
		
		public AnnotationCacheKey(Class<? extends Annotation> annotation, Class<? extends RawEntity<?>> type) {
			this.annotation = annotation;
			this.type = type;
		}

		public Class<? extends Annotation> getAnnotation() {
			return annotation;
		}

		public void setAnnotation(Class<? extends Annotation> annotation) {
			this.annotation = annotation;
		}

		public Class<? extends RawEntity<?>> getType() {
			return type;
		}

		public void setType(Class<? extends RawEntity<?>> type) {
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
			if (obj instanceof AnnotationCacheKey) {
				AnnotationCacheKey key = (AnnotationCacheKey) obj;
				
				if (key.getAnnotation().equals(annotation) && key.getType().equals(type)) {
					return true;
				}
				
				return false;
			}
			
			return super.equals(obj);
		}
	}
	
	private static final class CounterpartCacheKey {
		private final FieldNameConverter converter;
		private final Method method;
		
		public CounterpartCacheKey(FieldNameConverter converter, Method method) {
			this.converter = converter;
			this.method = method;
		}

		public FieldNameConverter getConverter() {
			return converter;
		}

		public Method getMethod() {
			return method;
		}
		
		@Override
		public int hashCode() {
			return converter.hashCode() + method.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			if (super.equals(obj)) {
				return true;
			}
			
			if (obj instanceof CounterpartCacheKey) {
				CounterpartCacheKey key = (CounterpartCacheKey) obj;
				
				return key.converter == converter && key.method == method;
			}
			
			return false;
		}
	}
}
