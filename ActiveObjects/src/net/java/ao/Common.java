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

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.java.ao.schema.FieldNameConverter;
import net.java.ao.schema.PrimaryKey;
import net.java.ao.types.DatabaseType;
import net.java.ao.types.TypeManager;

/**
 * WARNING: <i>Not</i> part of the public API.  This class is public only
 * to allow its use within other packages in the ActiveObjects library.
 * 
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
		return typeInstanceOf(type, superType);
	}
	
	public static boolean typeInstanceOf(Class<?> type, Class<?> otherType) {
		try {
			type.asSubclass(otherType);
		} catch (ClassCastException e) {
			return false;
		}
		
		return true;
	}
	
	public static String[] getMappingFields(FieldNameConverter converter,
			Class<? extends RawEntity<?>> from, Class<? extends RawEntity<?>> to) { 
		Set<String> back = new LinkedHashSet<String>();
		
		for (Method method : from.getMethods()) {
			Class<?> attributeType = getAttributeTypeFromMethod(method);
			
			if (attributeType == null) {
				continue;
			}
			
			if (interfaceInheritsFrom(attributeType, to)) {
				back.add(converter.getName(method));
			} else if (attributeType.getAnnotation(Polymorphic.class) != null 
					&& interfaceInheritsFrom(to, attributeType)) {
				back.add(converter.getName(method));
			}
		}
		
		return back.toArray(new String[back.size()]);
	}
	
	public static String[] getPolymorphicFieldNames(FieldNameConverter converter, Class<? extends RawEntity<?>> from, 
			Class<? extends RawEntity<?>> to) {
		Set<String> back = new LinkedHashSet<String>();
		
		for (Method method : from.getMethods()) {
			Class<?> attributeType = getAttributeTypeFromMethod(method);

			if (attributeType != null && interfaceInheritsFrom(to, attributeType)
					&& attributeType.getAnnotation(Polymorphic.class) != null) {
				back.add(converter.getPolyTypeName(method));
			}
		}
		
		return back.toArray(new String[back.size()]);
	}

	/**
	 * <b>Note</b>: this method leads to the creation and quick discard of
	 * large numbers of {@link AnnotationDelegate} objects.  Need to
	 * do some research to determine whether or not this is actually
	 * a problem.
	 */
	public static AnnotationDelegate getAnnotationDelegate(FieldNameConverter converter, Method method) {
		return new AnnotationDelegate(method, findCounterpart(converter, method));
	}
	
	/**
	 * Finds the corresponding method in an accessor/mutator pair based
	 * on the given method (or <code>null</code> if no corresponding method).
	 * @param converter TODO
	 */
	public static Method findCounterpart(FieldNameConverter converter, Method method) {
		return MethodFinder.getInstance().findCounterpart(converter, method);
	}
	
	public static boolean isAccessor(Method method) {
		if (method.getAnnotation(Accessor.class) != null) {
			return true;
		}
		
		if (method.getName().startsWith("get") || method.getName().startsWith("is")) {
			return method.getReturnType() != Void.TYPE;
		}
		
		return false;
	}
	
	public static boolean isMutator(Method method) {
		if (method.getAnnotation(Mutator.class) != null) {
			return true;
		}
		
		if (method.getName().startsWith("set")) {
			return method.getReturnType() == Void.TYPE;
		}
		
		return false;
	}
	
	public static Class<?> getAttributeTypeFromMethod(Method method) {
		Mutator mutatorAnnotation = method.getAnnotation(Mutator.class);
		Accessor accessorAnnotation = method.getAnnotation(Accessor.class);
		OneToOne oneToOneAnnotation = method.getAnnotation(OneToOne.class);
		OneToMany oneToManyAnnotation = method.getAnnotation(OneToMany.class);
		ManyToMany manyToManyAnnotation = method.getAnnotation(ManyToMany.class);
		
		Class<?> type = null;
		Class<?>[] parameterTypes = method.getParameterTypes();
		
		if (mutatorAnnotation != null) {
			if (parameterTypes.length != 1) {
				throw new IllegalArgumentException("Invalid method signature: " + method.toGenericString());
			}
			
			type = parameterTypes[0];
		} else if (accessorAnnotation != null) {
			if (method.getReturnType() == Void.TYPE) {
				throw new IllegalArgumentException("Invalid method signature: " + method.toGenericString());
			}
			
			type = method.getReturnType();
		} else if (oneToOneAnnotation != null) {
			return null;
		} else if (oneToManyAnnotation != null) {
			return null;
		} else if (manyToManyAnnotation != null) {
			return null;
		} else if (method.getName().startsWith("get")) {
			if (method.getReturnType() == Void.TYPE) {
				throw new IllegalArgumentException("Invalid method signature: " + method.toGenericString());
			}
			
			type = method.getReturnType();
		} else if (method.getName().startsWith("is")) {
			if (method.getReturnType() == Void.TYPE) {
				throw new IllegalArgumentException("Invalid method signature: " + method.toGenericString());
			}
			
			type = method.getReturnType();
		} else if (method.getName().startsWith("set")) {
			if (parameterTypes.length != 1) {
				throw new IllegalArgumentException("Invalid method signature: " + method.toGenericString());
			}
			
			type = parameterTypes[0];
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

	public static List<String> getSearchableFields(EntityManager manager, Class<? extends RawEntity<?>> type) {
		List<String> back = new ArrayList<String>();
		
		for (Method m : type.getMethods()) {
			Searchable annot = getAnnotationDelegate(manager.getFieldNameConverter(), m).getAnnotation(Searchable.class);
			
			if (annot != null) {
				Class<?> attributeType = Common.getAttributeTypeFromMethod(m);
				String name = manager.getFieldNameConverter().getName(m);
				
				// don't index Entity fields
				if (name != null && !Common.interfaceInheritsFrom(attributeType, RawEntity.class) && !back.contains(name)) {
					back.add(name);
				}
			}
		}
		
		return back;
	}
	
	public static Method getPrimaryKeyAccessor(Class<? extends RawEntity<?>> type) {
		Method[] methods = MethodFinder.getInstance().findAnnotation(PrimaryKey.class, type);
		if (methods.length == 0) {
			throw new RuntimeException("Entity " + type.getSimpleName() + " has no primary key field");
		}
		
		for (Method method : methods) {
			if (!method.getReturnType().equals(Void.TYPE) && method.getParameterTypes().length == 0) {
				return method;
			}
		}
		
		return null;
	}
	
	public static String getPrimaryKeyField(Class<? extends RawEntity<?>> type, FieldNameConverter converter) {
		Method[] annotatedMethods = MethodFinder.getInstance().findAnnotation(PrimaryKey.class, type);
		if (annotatedMethods.length == 0) {
			throw new RuntimeException("Entity " + type.getSimpleName() + " has no primary key field");
		}
		
		return converter.getName(annotatedMethods[0]);
	}
	
	public static Method getPrimaryKeyMethod(Class<? extends RawEntity<?>> type) {
		Method[] annotatedMethods = MethodFinder.getInstance().findAnnotation(PrimaryKey.class, type);
		if (annotatedMethods.length == 0) {
			throw new RuntimeException("Entity " + type.getSimpleName() + " has no primary key field");
		}
		
		return annotatedMethods[0];
	}
	
	public static <K> DatabaseType<K> getPrimaryKeyType(Class<? extends RawEntity<K>> type) {
		return TypeManager.getInstance().getType(getPrimaryKeyClassType(type));
	}
	
	public static <K> Class<K> getPrimaryKeyClassType(Class<? extends RawEntity<K>> type) {
		Method[] annotatedMethods = MethodFinder.getInstance().findAnnotation(PrimaryKey.class, type);
		if (annotatedMethods.length == 0) {
			throw new RuntimeException("Entity " + type.getSimpleName() + " has no primary key field");
		}
		
		Method meth = annotatedMethods[0];
		
		Class<K> keyType = (Class<K>) meth.getReturnType();
		if (keyType.equals(Void.TYPE)) {
			keyType = (Class<K>) meth.getParameterTypes()[0];
		}
		
		return keyType;
	}
	
	public static <K> K getPrimaryKeyValue(RawEntity<K> entity) {
		try {
			return (K) Common.getPrimaryKeyAccessor(entity.getEntityType()).invoke(entity);
		} catch (IllegalArgumentException e) {
			return null;
		} catch (IllegalAccessException e) {
			return null;
		} catch (InvocationTargetException e) {
			return null;
		}
	}
	
	public static boolean fuzzyCompare(Object a, Object b) {
		if (a == null && b == null) {
			return true;
		} else if (a == null || b == null) {	// implicitly, one or other is null, not both
			return false;
		}
		
		Object array = null;
		Object other = null;
		
		if (a.getClass().isArray()) {
			array = a;
			other = b;
		} else if (b.getClass().isArray()) {
			array = b;
			other = a;
		}
		
		if (array != null) {
			for (int i = 0; i < Array.getLength(array); i++) {
				if (fuzzyCompare(Array.get(array, i), other)) {
					return true;
				}
			}
		}
		
		if (a instanceof DatabaseFunction) {
			return a.equals(b);
		}
		
		return TypeManager.getInstance().getType(a.getClass()).valueEquals(a, b)
			|| TypeManager.getInstance().getType(b.getClass()).valueEquals(b, a);
	}
	
	public static boolean fuzzyTypeCompare(int typeA, int typeB) {
		if (typeA == Types.BOOLEAN) {
			switch (typeB) {
				case Types.BIGINT:
					return true;
					
				case Types.BIT:
					return true;
					
				case Types.INTEGER:
					return true;
					
				case Types.NUMERIC:
					return true;
					
				case Types.SMALLINT:
					return true;
					
				case Types.TINYINT:
					return true;
			}
		} else if (typeA == Types.BIGINT || typeA == Types.BIT || typeA == Types.INTEGER || typeA == Types.NUMERIC
				|| typeA == Types.SMALLINT || typeA == Types.TINYINT) {
			if (typeB == Types.BOOLEAN) {
				return true;
			}
		} else if (typeA == Types.CLOB) {
			if (typeB == Types.LONGVARCHAR || typeB == Types.VARCHAR) {
				return true;
			}
		} else if (typeA == Types.LONGVARCHAR || typeA == Types.VARCHAR) {
			if (typeB == Types.CLOB) {
				return true;
			}
		}
		
		return typeA == typeB;
	}
}
