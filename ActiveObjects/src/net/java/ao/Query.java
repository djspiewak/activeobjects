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

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import net.java.ao.schema.FieldNameConverter;
import net.java.ao.schema.TableNameConverter;
import net.java.ao.types.TypeManager;

/**
 * @author Daniel Spiewak
 */
public class Query implements Serializable {
	public enum QueryType {
		SELECT
	}
	
	private final static String PRIMARY_KEY_FIELD = "''''primary_key_field''''";
	
	private final QueryType type;
	private String fields;
	
	private boolean distinct = false;
	
	private Class<? extends RawEntity<?>> tableType;
	private String table;
	
	private String whereClause;
	private Object[] whereParams;
	
	private String orderClause;
	private String groupClause;
	private int limit = -1;
	private int offset = -1;
	
	private Map<Class<? extends RawEntity<?>>, String> joins;
	
	private static final long serialVersionUID = 1l;
	
	public Query(QueryType type, String fields) {
		this.type = type;
		this.fields = fields;
		
		joins = new HashMap<Class<? extends RawEntity<?>>, String>();
	}
	
	public String[] getFields() {
		if (fields.contains(PRIMARY_KEY_FIELD)) {
			return new String[0];
		}
		
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
	
	<K> void resolveFields(Class<? extends RawEntity<K>> tableType, FieldNameConverter converter) {
		fields = fields.replaceAll(PRIMARY_KEY_FIELD, Common.getPrimaryKeyField(tableType, converter));
	}
	
	public Query distinct() {
		distinct = true;
		
		return this;
	}
	
	public Query from(Class<? extends RawEntity<?>> tableType) {
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
		setWhereParams(params);
		
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
	
	public Query join(Class<? extends RawEntity<?>> join, String on) {
		joins.put(join, on);
		
		return this;
	}
	
	public Query join(Class<? extends RawEntity<?>> join) {
		joins.put(join, null);
		
		return this;
	}
	
	public boolean isDistinct() {
		return distinct;
	}

	public void setDistinct(boolean distinct) {
		this.distinct = distinct;
	}

	public Class<? extends RawEntity<?>> getTableType() {
		return tableType;
	}

	public void setTableType(Class<? extends RawEntity<?>> tableType) {
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
		
		if (whereParams != null) {
			for (int i = 0; i < whereParams.length; i++) {
				if (whereParams[i] instanceof RawEntity) {
					whereParams[i] = Common.getPrimaryKeyValue((RawEntity<?>) whereParams[i]);
				}
			}
		}
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

	public Map<Class<? extends RawEntity<?>>, String> getJoins() {
		return joins;
	}

	public void setJoins(Map<Class<? extends RawEntity<?>>, String> joins) {
		this.joins = joins;
	}

	public QueryType getType() {
		return type;
	}

	protected <K> String toSQL(Class<? extends RawEntity<K>> tableType, DatabaseProvider provider, TableNameConverter converter, 
			FieldNameConverter fieldConverter, boolean count) {
		if (this.tableType == null && table == null) {
			this.tableType = tableType;
		}
		
		resolveFields(tableType, fieldConverter);
		
		return provider.renderQuery(this, converter, count);
	}

	@SuppressWarnings("unchecked")
	protected void setParameters(EntityManager manager, PreparedStatement stmt) throws SQLException {
		if (whereParams != null) {
			TypeManager typeManager = TypeManager.getInstance();
			
			for (int i = 0; i < whereParams.length; i++) {
				if (whereParams[i] == null) {
					manager.getProvider().putNull(stmt, i + 1);
				} else {
					Class javaType = whereParams[i].getClass();
					
					if (whereParams[i] instanceof RawEntity) {
						javaType = ((RawEntity) whereParams[i]).getEntityType();
					}
					
					typeManager.getType(javaType).putToDatabase(i + 1, stmt, whereParams[i]);
				}
			}
		}
	}

	public static Query select() {
		return select(PRIMARY_KEY_FIELD);
	}

	public static Query select(String fields) {
		return new Query(QueryType.SELECT, fields);
	}
}
