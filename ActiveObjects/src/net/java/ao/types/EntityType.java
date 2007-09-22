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

import net.java.ao.Common;
import net.java.ao.EntityManager;
import net.java.ao.RawEntity;

/**
 * @author Daniel Spiewak
 */
class EntityType extends DatabaseType<RawEntity> {

	public EntityType() {
		super(Types.INTEGER, -1, RawEntity.class);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void putToDatabase(int index, PreparedStatement stmt, RawEntity value) throws SQLException {
		DatabaseType dbType = Common.getPrimaryKeyType(value.getEntityType());
		dbType.putToDatabase(index, stmt, Common.getPrimaryKeyValue(value));
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public RawEntity convert(EntityManager manager, ResultSet res, Class<? extends RawEntity> type, String field) throws SQLException {
		DatabaseType dbType = Common.getPrimaryKeyType(type);
		Class pkType = Common.getPrimaryKeyClassType(type);
		
		return manager.get(type, dbType.convert(manager, res, pkType, field));
	}

	@Override
	public String getDefaultName() {
		return "INTEGER";
	}

	@Override
	public Object defaultParseValue(String value) {
		return Integer.parseInt(value);
	}
}
