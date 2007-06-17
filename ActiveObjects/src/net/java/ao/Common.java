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

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Daniel Spiewak
 */
public final class Common {
	public static String convertSimpleClassName(String name) {
		String[] array = name.split("\\.");
		return array[array.length - 1];
	}

	public static String convertDowncaseName(String name) {
		String back = "";

		for (char c : name.toCharArray()) {
			if (c == name.charAt(0)) {
				back += Character.toLowerCase(c);
			} else {
				back += c;
			}
		}

		return back;
	}

	public static boolean interfaceInheritsFrom(Class<?> type, Class<?> superType) {
		if (type.equals(superType)) {
			return true;
		}

		Class<?>[] interfaces = type.getInterfaces();
		for (Class<?> t : interfaces) {
			if (interfaceInheritsFrom(t, superType)) {
				return true;
			}
		}

		return false;
	}
	
//	public static String getTableName(Class<? extends Entity> type) {
//		return EntityNameManager.getInstance().getName(type);
//	}
	
	public static String[] getMappingFields(Class<? extends Entity> from, Class<? extends Entity> to) { 
		Set<String> back = new LinkedHashSet<String>();
		
		for (Method method : from.getMethods()) {
			Accessor accessorAnnotation = method.getAnnotation(Accessor.class);
			Mutator mutatorAnnotation = method.getAnnotation(Mutator.class);
			
			if (accessorAnnotation != null) {
				if (method.getReturnType().equals(to)) {
					back.add(accessorAnnotation.value());
				}
			} else if (mutatorAnnotation != null) {
				if (method.getParameterTypes()[0].equals(to)) {
					back.add(mutatorAnnotation.value());
				}
			} else if (method.getName().toLowerCase().startsWith("get")) {
				if (method.getReturnType().equals(to)) {
					back.add(convertDowncaseName(method.getName().substring(3)) + "ID");
				}
			} else if (method.getName().toLowerCase().startsWith("is")) {
				if (method.getReturnType().equals(to)) {
					back.add(convertDowncaseName(method.getName().substring(2)) + "ID");
				}
			} else if (method.getName().toLowerCase().startsWith("set")) {
				if (method.getParameterTypes()[0].equals(to)) {
					back.add(convertDowncaseName(method.getName().substring(3)) + "ID");
				}
			}
		}
		
		return back.toArray(new String[back.size()]);
	}
	
	public static String getAttributeNameFromMethod(Method method) {
		Mutator mutatorAnnotation = method.getAnnotation(Mutator.class);
		Accessor accessorAnnotation = method.getAnnotation(Accessor.class);
		OneToMany oneToManyAnnotation = method.getAnnotation(OneToMany.class);
		ManyToMany manyToManyAnnotation = method.getAnnotation(ManyToMany.class);
		
		String attributeName = null;
		Class<?> type = getAttributeTypeFromMethod(method);
		
		if (mutatorAnnotation != null) {
			attributeName = mutatorAnnotation.value();
		} else if (accessorAnnotation != null) {
			attributeName = accessorAnnotation.value();
		} else if (oneToManyAnnotation != null) {
			return null;
		} else if (manyToManyAnnotation != null) {
			return null;
		} else if (method.getName().startsWith("get")) {
			attributeName = convertDowncaseName(method.getName().substring(3));
			
			if (interfaceInheritsFrom(type, Entity.class)) {
				attributeName += "ID";
			}
		} else if (method.getName().startsWith("is")) {
			attributeName = convertDowncaseName(method.getName().substring(2));
			
			if (interfaceInheritsFrom(type, Entity.class)) {
				attributeName += "ID";
			}
		} else if (method.getName().startsWith("set")) {
			attributeName = convertDowncaseName(method.getName().substring(3));
			
			if (interfaceInheritsFrom(type, Entity.class)) {
				attributeName += "ID";
			}
		}
		
		return attributeName;
	}
	
	public static Class<?> getAttributeTypeFromMethod(Method method) {
		Mutator mutatorAnnotation = method.getAnnotation(Mutator.class);
		Accessor accessorAnnotation = method.getAnnotation(Accessor.class);
		OneToMany oneToManyAnnotation = method.getAnnotation(OneToMany.class);
		ManyToMany manyToManyAnnotation = method.getAnnotation(ManyToMany.class);
		
		Class<?> type = null;
		
		if (mutatorAnnotation != null) {
			type = method.getParameterTypes()[0];
		} else if (accessorAnnotation != null) {
			type = method.getReturnType();
		} else if (oneToManyAnnotation != null) {
			return null;
		} else if (manyToManyAnnotation != null) {
			return null;
		} else if (method.getName().startsWith("get")) {
			type = method.getReturnType();
		} else if (method.getName().startsWith("is")) {
			type = method.getReturnType();
		} else if (method.getName().startsWith("set")) {
			type = method.getParameterTypes()[0];
		}
		
		return type;
	}
    
   public static Class<?> getCallingClass(int depth) {
        StackTraceElement[] stack = new Exception().getStackTrace();
        try {
            return Class.forName(stack[depth + 2].getClassName());
        } catch (ClassNotFoundException e) {}
        
        return null;
    }
}
