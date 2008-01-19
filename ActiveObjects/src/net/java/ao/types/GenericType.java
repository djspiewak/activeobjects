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

import net.java.ao.EntityManager;

/**
 * @author Daniel Spiewak
 */
class GenericType extends DatabaseType<Object> {

	protected GenericType(int type) {
		super(type, -1);
	}
	

	@Override
	public Object pullFromDatabase(EntityManager manager, ResultSet res, Class<? extends Object> type, String field) throws SQLException {
		return res.getObject(field);
	}

	@Override
	public String getDefaultName() {
		String back = "GENERIC";
		
		Class<Types> clazz = Types.class;
		for (Field field : clazz.getFields()) {
			if (Modifier.isStatic(field.getModifiers())) {
				try {
					if (field.get(null).equals(getType())) {
						back = field.getName();
					}
				} catch (IllegalArgumentException e) {
				} catch (IllegalAccessException e) {
				}
			}
		}
		
		return back;
	}

	@Override
	public Object defaultParseValue(String value) {
		return null;
	}
}
