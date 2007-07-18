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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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
		StringBuilder back = new StringBuilder();

		back.append(Character.toLowerCase(name.charAt(0)));
		back.append(name.substring(1));

		return back.toString();
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
	
	public static boolean typeInstanceOf(Class<?> type, Class<?> otherType) {
		try {
			type.asSubclass(otherType);
		} catch (ClassCastException e) {
			return false;
		}
		
		return true;
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

	public static List<String> getIndexFields(Class<? extends Entity> type) {
		List<String> back = new ArrayList<String>();
		
		for (Method m : type.getMethods()) {
			Index annot = m.getAnnotation(Index.class);
			
			if (annot != null) {
				Class<?> attributeType = Common.getAttributeTypeFromMethod(m);
				String name = Common.getAttributeNameFromMethod(m);
				
				// don't index Entity fields
				if (name != null && !Common.interfaceInheritsFrom(attributeType, Entity.class) && !back.contains(name)) {
					back.add(name);
				}
			}
		}
		
		return back;
	}
}
