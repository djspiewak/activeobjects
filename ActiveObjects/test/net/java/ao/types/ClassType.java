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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import net.java.ao.EntityManager;

/**
 * Basic custom type example
 * 
 * @author Daniel Spiewak
 */
public class ClassType extends DatabaseType<Class<?>> {

	public ClassType() {
		super(Types.VARCHAR, 255, Class.class);
	}

	@Override
	public Class<?> pullFromDatabase(EntityManager manager, ResultSet res, Class<? extends Class<?>> type, String field) throws SQLException {
		try {
			return Class.forName(res.getString(field));
		} catch (Throwable t) {
			return null;
		}
	}
	
	@Override
	public void putToDatabase(EntityManager manager, PreparedStatement stmt, int index, Class<?> value) throws SQLException {
		stmt.setString(index, value.getName());
	}

	@Override
	public Object defaultParseValue(String value) {
		try {
			return Class.forName(value);
		} catch (Throwable t) {
			return null;
		}
	}
	
	@Override
	public String valueToString(Object value) {
		if (value instanceof Class<?>) {
			return ((Class<?>) value).getCanonicalName();
		}
		
		return super.valueToString(value);
	}

	@Override
	public String getDefaultName() {
		return "VARCHAR";
	}
}
