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

/**
 * @author Daniel Spiewak
 */
public class Query {
	public enum QueryType {
		SELECT
	}
	
	private final QueryType type;
	private String fields;
	
	private boolean distinct = false;
	
	private Class<? extends Entity> tableType;
	private String table;
	
	private String whereClause;
	private Object[] whereParams;
	
	private String orderClause;
	private String groupClause;
	private int limit = -1;
	private int offset = -1;
	
	private Map<Class<? extends Entity>, String> joins;
	
	public Query(QueryType type, String fields) {
		this.type = type;
		this.fields = fields;
		
		joins = new HashMap<Class<? extends Entity>, String>();
	}
	
	public String[] getFields() {
		String[] fieldsArray = fields.split(",");
		String[] back = new String[fieldsArray.length];
		
		for (int i = 0; i < fieldsArray.length; i++) {
			back[i] = fieldsArray[i].trim();
		}
		
		return back;
	}
	
	void setFields(String[] fields) {
		if (fields.length == 0) {
			return;
		}
		
		StringBuilder builder = new StringBuilder();
		for (String field : fields) {
			builder.append(field).append(',');
		}
		if (fields.length > 1) {
			builder.setLength(builder.length() - 1);
		}
		
		this.fields = builder.toString();
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
	
	public Query offset(int offset) {
		this.offset = offset;
		
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
	
	public boolean isDistinct() {
		return distinct;
	}

	public void setDistinct(boolean distinct) {
		this.distinct = distinct;
	}

	public Class<? extends Entity> getTableType() {
		return tableType;
	}

	public void setTableType(Class<? extends Entity> tableType) {
		this.tableType = tableType;
	}

	public String getTable() {
		return table;
	}

	public void setTable(String table) {
		this.table = table;
	}

	public String getWhereClause() {
		return whereClause;
	}

	public void setWhereClause(String whereClause) {
		this.whereClause = whereClause;
	}

	public Object[] getWhereParams() {
		return whereParams;
	}

	public void setWhereParams(Object[] whereParams) {
		this.whereParams = whereParams;
	}

	public String getOrderClause() {
		return orderClause;
	}

	public void setOrderClause(String orderClause) {
		this.orderClause = orderClause;
	}

	public String getGroupClause() {
		return groupClause;
	}

	public void setGroupClause(String groupClause) {
		this.groupClause = groupClause;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public int getOffset() {
		return offset;
	}
	
	public void setOffset(int offset) {
		this.offset = offset;
	}

	public Map<Class<? extends Entity>, String> getJoins() {
		return joins;
	}

	public void setJoins(Map<Class<? extends Entity>, String> joins) {
		this.joins = joins;
	}

	public QueryType getType() {
		return type;
	}

	protected String toSQL(Class<? extends Entity> tableType, EntityManager manager, boolean count) {
		if (this.tableType == null && table == null) {
			this.tableType = tableType;
		}
		
		return manager.getProvider().renderQuery(this, manager.getNameConverter(), count);
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
