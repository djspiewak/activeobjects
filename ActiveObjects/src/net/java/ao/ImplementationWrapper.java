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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Spiewak
 */
class ImplementationWrapper<T extends RawEntity<?>> {
	private List<Object> implementations;
	
	public ImplementationWrapper(T instance) {
		implementations = new ArrayList<Object>();
		
		instantiateImplementation(instance, instance.getEntityType());
	}
	
	private void instantiateImplementation(T instance, Class<? extends RawEntity<?>> clazz) {
		Implementation implAnnotation = clazz.getAnnotation(Implementation.class);
		
		if (implAnnotation != null) {
			try {
				Constructor<Object> con = (Constructor<Object>) implAnnotation.value().getConstructor(clazz);
				implementations.add(con.newInstance(instance));
			} catch (SecurityException e) {
			} catch (NoSuchMethodException e) {
			} catch (IllegalArgumentException e) {
			} catch (InstantiationException e) {
			} catch (IllegalAccessException e) {
			} catch (InvocationTargetException e) {
			}
		}
		
		for (Class<?> sup : clazz.getInterfaces()) {
			instantiateImplementation(instance, (Class<? extends RawEntity<?>>) sup);
		}
	}
	
	public MethodImplWrapper getMethod(String name, Class<?>... parameterTypes) {
		for (Object obj : implementations) {
			try {
				return new MethodImplWrapper(obj, obj.getClass().getMethod(name, parameterTypes));
			} catch (SecurityException e) {
			} catch (NoSuchMethodException e) {
			}
		}
		
		return null;
	}
}
