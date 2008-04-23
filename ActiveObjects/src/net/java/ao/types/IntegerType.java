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
class IntegerType extends DatabaseType<Integer> {
	
	public IntegerType() {
		super(Types.INTEGER, -1, int.class, Integer.class);
	}

	@Override
	public String getDefaultName() {
		return "INTEGER";
	}
	
	@Override
	public Integer pullFromDatabase(EntityManager manager, ResultSet res, Class<? extends Integer> type, int index) throws SQLException {
		return res.getInt(index);		// for oracle
	}
	
	@Override
	public Integer pullFromDatabase(EntityManager manager, ResultSet res, Class<? extends Integer> type, String field) throws SQLException {
		return res.getInt(field);
	}

	@Override
	public Integer defaultParseValue(String value) {
		return Integer.parseInt(value);
	}
}
