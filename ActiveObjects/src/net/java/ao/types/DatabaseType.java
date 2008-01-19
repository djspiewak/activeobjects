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
package net.java.ao.types;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import net.java.ao.EntityManager;

/**
 * @author Daniel Spiewak
 */
public abstract class DatabaseType<T> {
	private final int type, defaultPrecision;
	
	private final Class<?>[] handledTypes;
	
	protected DatabaseType(int type, int defaultPrecision, Class<?>... handledTypes) {
		this.type = type;
		this.defaultPrecision = defaultPrecision;
		this.handledTypes = handledTypes;
	}
	
	public int getType() {
		return type;
	}
	
	public int getDefaultPrecision() {
		return defaultPrecision;
	}
	
	public boolean isHandlerFor(int type) {
		return this.type == type;
	}
	
	public boolean isHandlerFor(Class<?> type) {
		for (Class<?> handled : handledTypes) {
			if (isSubclass(handled, type)) {
				return true;
			}
		}
		
		return false;
	}
	
	@SuppressWarnings("unchecked") 
	private boolean isSubclass(Class sup, Class sub) {
		if (sub.equals(sup)) {
			return true;
		} else if (sub.equals(Object.class)) {
			return false;
		}
		
		Class superclass = sub.getSuperclass();
		List<Class> superclasses = new LinkedList<Class>();
		superclasses.addAll(Arrays.asList(sub.getInterfaces()));
		
		if (superclass != null) {
			superclasses.add(superclass);
		}
		
		for (Class parent : superclasses) {
			if (isSubclass(sup, parent)) {
				return true;
			}
		}
		
		return false;
	}
	
	public boolean shouldCache(Class<?> type) {
		return true;
	}
	
	public void putToDatabase(int index, PreparedStatement stmt, T value) throws SQLException {
		stmt.setObject(index, value, getType());
	}
	
	public boolean valueEquals(Object val1, Object val2) {
		return val1.equals(val2);
	}
	
	public abstract String getDefaultName();
	
	public abstract T pullFromDatabase(EntityManager manager, ResultSet res, Class<? extends T> type, String field) throws SQLException;
	
	public T pullFromDatabase(EntityManager manager, ResultSet res, Class<? extends T> type, int index) throws SQLException {
		return pullFromDatabase(manager, res, type, res.getMetaData().getColumnLabel(index));
	}
	
	public abstract Object defaultParseValue(String value);
	
	public String valueToString(Object value) {
		return value.toString();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DatabaseType) {
			DatabaseType<?> type = (DatabaseType<?>) obj;
			
			if (type.type == this.type && type.defaultPrecision == defaultPrecision && Arrays.equals(type.handledTypes, handledTypes)) {
				return true;
			}
		}
		
		return super.equals(obj);
	}
	
	@Override
	public int hashCode() {
		int hashCode = type + defaultPrecision;
		
		for (Class<?> type : handledTypes) {
			hashCode += type.hashCode();
		}
		hashCode %= 2 << 7;
		
		return hashCode;
	}
	
	@Override
	public String toString() {
		String back = "GENERIC";
		
		Class<Types> clazz = Types.class;
		for (Field field : clazz.getFields()) {
			if (Modifier.isStatic(field.getModifiers())) {
				try {
					if (field.get(null).equals(type)) {
						back = field.getName();
					}
				} catch (IllegalArgumentException e) {
				} catch (IllegalAccessException e) {
				}
			}
		}
		
		if (defaultPrecision > 0) {
			back += "(" + defaultPrecision + ")";
		}
		
		return back;
	}
}
