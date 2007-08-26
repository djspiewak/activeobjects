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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * @author Daniel Spiewak
 */
public abstract class DatabaseType<T> {
	private final int type, precision;
	
	private final Class<?>[] handledTypes;
	
	protected DatabaseType(int type, int precision, Class<?>... handledTypes) {
		this.type = type;
		this.precision = precision;
		this.handledTypes = handledTypes;
	}
	
	public int getType() {
		return type;
	}
	
	public int getPrecision() {
		return precision;
	}
	
	public boolean isHandlerFor(int type) {
		return this.type == type;
	}
	
	public boolean isHandlerFor(Class<?> type) {
		for (Class<?> handled : handledTypes) {
			if (handled.equals(type)) {
				return true;
			}
		}
		
		return false;
	}
	
	public abstract T convert(ResultSet res, String field) throws SQLException;
	
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
		
		if (precision > 0) {
			back += "(" + precision + ")";
		}
		
		return back;
	}
}
