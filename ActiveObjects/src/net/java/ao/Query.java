/*
 * Copyright 2007, Daniel Spiewak
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided
 *     with the distribution.
 *   * Neither the name of the ActiveObjects project nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.java.ao;

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
	private final String fields;
	
	private String whereClause;
	private Object[] whereParams;
	
	private String orderClause;
	private String groupClause;
	private int limit = -1;
	
	private Map<String, String> joins;
	
	public Query(QueryType type, String fields) {
		this.type = type;
		this.fields = fields;
		
		joins = new HashMap<String, String>();
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
	
	public Query join(String join, String on) {
		joins.put(join, on);
		
		return this;
	}
	
	public Query join(String join) {
		joins.put(join, null);
		
		return this;
	}
	
	public String toSQL(String tableName) {
		StringBuilder sql = new StringBuilder();
		
		switch (type) {
			case SELECT:
				sql.append("SELECT ");
				sql.append(fields);
				sql.append(" FROM ");
				sql.append(tableName);
			break;
		}
		
		if (joins.size() > 0) {
			for (String join : joins.keySet()) {
				sql.append(" JOIN ");
				sql.append(join);
				
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
		
		if (limit >= 0) {
			sql.append(" LIMIT ");
			sql.append(limit);
		}
		
		if (groupClause != null) {
			sql.append(" GROUP BY ");
			sql.append(groupClause);
		}
		
		if (orderClause != null) {
			sql.append(" ORDER BY ");
			sql.append(orderClause);
		}
		
		return sql.toString();
	}
	
	public void setParameters(PreparedStatement stmt) throws SQLException {
		if (whereParams != null) {
			for (int i = 0; i < whereParams.length; i++) {
				if (whereParams[i] instanceof Entity) {
					whereParams[i] = ((Entity) whereParams[i]).getID();
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
