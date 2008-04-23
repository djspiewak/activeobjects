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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import net.java.ao.EntityManager;

/**
 * @author Daniel Spiewak
 */
class BooleanType extends DatabaseType<Boolean> {

	protected BooleanType() {
		super(Types.BOOLEAN, -1, boolean.class, Boolean.class);
	}

	@Override
	public String getDefaultName() {
		return "BOOLEAN";
	}
	
	@Override
	public Boolean pullFromDatabase(EntityManager manager, ResultSet res, Class<? extends Boolean> type, String field) throws SQLException {
		return res.getBoolean(field);
	}

	@Override
	public Boolean defaultParseValue(String value) {
		return Boolean.parseBoolean(value.trim());
	}
	
	@Override
	public boolean valueEquals(Object a, Object b) {
		if (a instanceof Number) {
			if (b instanceof Boolean) {
				return (((Number) a).intValue() == 1) == ((Boolean) b).booleanValue();
			}
		} else if (a instanceof Boolean) {
			if (b instanceof Number) {
				return (((Number) b).intValue() == 1) == ((Boolean) a).booleanValue();
			}
		}
		
		return a.equals(b);
	}
}
