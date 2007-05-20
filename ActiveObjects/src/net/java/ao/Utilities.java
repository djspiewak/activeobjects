/*
 * Created on May 4, 2007
 */
package net.java.ao;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;

import net.java.ao.schema.Table;

/**
 * @author Daniel Spiewak
 */
public final class Utilities {
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

	public static boolean interfaceIneritsFrom(Class<?> type, Class<?> superType) {
		if (type.equals(superType)) {
			return true;
		}

		Class<?>[] interfaces = type.getInterfaces();
		for (Class<?> t : interfaces) {
			if (interfaceIneritsFrom(t, superType)) {
				return true;
			}
		}

		return false;
	}
	
	public static String getTableName(Class<? extends Entity> type) {
		String tableName = convertDowncaseName(convertSimpleClassName(type.getCanonicalName()));
		
		if (type.getAnnotation(Table.class) != null) {
			tableName = type.getAnnotation(Table.class).value();
		}
		
		return tableName;
	}
	
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
}
