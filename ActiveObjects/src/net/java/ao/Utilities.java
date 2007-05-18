/*
 * Created on May 4, 2007
 */
package net.java.ao;

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
}
