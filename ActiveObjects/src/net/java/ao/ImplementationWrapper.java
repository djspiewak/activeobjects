/*
 * Copyright 2007, Daniel Spiewak
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided
 *     with the distribution.
 *   * Neither the name of the ActiveObjects project nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.java.ao;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Spiewak
 */
class ImplementationWrapper<T extends Entity> {
	private List<Object> implementations;
	
	public ImplementationWrapper(T instance) {
		implementations = new ArrayList<Object>();
		
		instantiateImplementation(instance, instance.getEntityType());
	}
	
	private void instantiateImplementation(T instance, Class<? extends Entity> clazz) {
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
			if (Common.interfaceInheritsFrom(sup, Entity.class)) {
				instantiateImplementation(instance, (Class<? extends Entity>) sup);
			}
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
