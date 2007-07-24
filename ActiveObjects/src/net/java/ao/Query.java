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

import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import net.java.ao.schema.PluggableNameConverter;

/**
 * @author Daniel Spiewak
 */
public class Query {
	public enum QueryType {
		SELECT
	}
	
	private final QueryType type;
	private final String fields;
	
	private boolean distinct = false;
	
	private Class<? extends Entity> tableType;
	private String table;
	
	private String whereClause;
	private Object[] whereParams;
	
	private String orderClause;
	private String groupClause;
	private int limit = -1;
	
	private Map<Class<? extends Entity>, String> joins;
	
	public Query(QueryType type, String fields) {
		this.type = type;
		this.fields = fields;
		
		joins = new HashMap<Class<? extends Entity>, String>();
	}
	
	public String[] getFields() {
		return fields.split(",");
	}
	
	public Query distinct() {
		distinct = true;
		
		return this;
	}
	
	public Query from(Class<? extends Entity> tableType) {
		table = null;
		this.tableType = tableType;
		
		return this;
	}
	
	public Query from(String table) {
		tableType = null;
		this.table = table;
		
		return this;
	}
	
	public Query where(String clause, Object... params) {
		whereClause = clause;
		whereParams = params;
		
		return this;
	}
	
	public Query order(String clause) {
		orderClause = clause;
		
		return this;
	}
	
	public Query group(String clause) {
		groupClause = clause;
		
		return this;
	}
	
	public Query limit(int limit) {
		this.limit = limit;
		
		return this;
	}
	
	public Query join(Class<? extends Entity> join, String on) {
		joins.put(join, on);
		
		return this;
	}
	
	public Query join(Class<? extends Entity> join) {
		joins.put(join, null);
		
		return this;
	}
	
	protected String toSQL(Class<? extends Entity> tableType, PluggableNameConverter nameConverter, boolean count) {
		StringBuilder sql = new StringBuilder();
		
		if (this.tableType != null) {
			tableType = this.tableType;
		}
		
		String tableName = nameConverter.getName(tableType);
		if (this.table != null) {
			tableName = this.table;
		}
		
		switch (type) {
			case SELECT:
				sql.append("SELECT ");
				
				if (distinct) {
					sql.append("DISTINCT ");
				}
				
				if (count) {
					sql.append("COUNT(*)");
				} else {
					sql.append(fields);
				}
				sql.append(" FROM ");
				
				sql.append(tableName);
			break;
		}
		
		if (joins.size() > 0) {
			for (Class<? extends Entity> join : joins.keySet()) {
				sql.append(" JOIN ");
				sql.append(nameConverter.getName(join));
				
				String on = joins.get(join);
				if (on != null) {
					sql.append(" ON ");
					sql.append(on);
				}
			}
		}
		
		if (whereClause != null) {
			sql.append(" WHERE ");
			sql.append(whereClause);
		}
		
		if (groupClause != null) {
			sql.append(" GROUP BY ");
			sql.append(groupClause);
		}
		
		if (orderClause != null) {
			sql.append(" ORDER BY ");
			sql.append(orderClause);
		}
		
		if (limit >= 0) {
			sql.append(" LIMIT ");
			sql.append(limit);
		}
		
		return sql.toString();
	}
	
	protected void setParameters(PreparedStatement stmt) throws SQLException {
		if (whereParams != null) {
			for (int i = 0; i < whereParams.length; i++) {
				if (whereParams[i] instanceof Entity) {
					whereParams[i] = ((Entity) whereParams[i]).getID();
				} else if (whereParams[i] instanceof URL) {
					whereParams[i] = whereParams[i].toString();
				}
				
				stmt.setObject(i + 1, whereParams[i]);
			}
		}
	}
	
	public static Query select() {
		return select("id");
	}
	
	public static Query select(String fields) {
		return new Query(QueryType.SELECT, fields);
	}
}
