/*
 * Created on May 4, 2007
 */
package net.java.ao;

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
}
