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
class EntityType<T> extends DatabaseType<RawEntity<T>> {
	private DatabaseType<T> primaryKeyType;
	
	public EntityType(Class<? extends RawEntity<T>> type) {
		super(Types.INTEGER, -1, RawEntity.class);
		
		primaryKeyType = Common.getPrimaryKeyType(type);
	}
	
	@Override
	public int getType() {
		return primaryKeyType.getType();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void putToDatabase(int index, PreparedStatement stmt, RawEntity value) throws SQLException {
		DatabaseType dbType = Common.getPrimaryKeyType(value.getEntityType());
		dbType.putToDatabase(index, stmt, Common.getPrimaryKeyValue(value));
	}
	
	@Override
	public RawEntity<T> pullFromDatabase(EntityManager manager, ResultSet res, Class<? extends RawEntity<T>> type, String field) throws SQLException {
		DatabaseType<T> dbType = Common.getPrimaryKeyType(type);
		Class<T> pkType = Common.getPrimaryKeyClassType(type);
		
		return manager.get(type, dbType.pullFromDatabase(manager, res, pkType, field));
	}

	@Override
	public String getDefaultName() {
		return primaryKeyType.getDefaultName();
	}

	@Override
	public Object defaultParseValue(String value) {
		return Integer.parseInt(value);
	}
	
	@Override
	public int hashCode() {
		return (super.hashCode() + primaryKeyType.hashCode()) % (2 << 10);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj)) {
			if (obj instanceof EntityType) {
				if (((EntityType<?>) obj).primaryKeyType.equals(primaryKeyType)) {
					return true;
				}
			} else {
				return true;
			}
		}
		
		return false;
	}
}
