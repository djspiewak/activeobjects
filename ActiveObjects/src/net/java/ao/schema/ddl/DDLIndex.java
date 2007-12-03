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
package net.java.ao.schema.ddl;

import net.java.ao.types.DatabaseType;

/**
 * Database-agnostic reprensentation of a general field index 
 * statement (not related to full-text indexing).  To save on
 * object creation, as well as to simplify schema parsing, table
 * and field <i>names</i> are stored rather than full DDL
 * representations.  This class also defines the convention
 * imposed to generate the names of field indexes.  It is
 * important that all DDL renderers (i.e. database providers)
 * observe this convention, else migrations will do strange things.
 * 
 * @author Daniel Spiewak
 */
public class DDLIndex {
	private String table;
	private String field;
	private DatabaseType<?> type;

	public String getTable() {
		return table;
	}

	public void setTable(String table) {
		this.table = table;
	}

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}
	
	public String getName() {
		return "index_" + table.toLowerCase() + "_" + field.toLowerCase();
	}
	
	public DatabaseType<?> getType() {
		return type;
	}

	public void setType(DatabaseType<?> type) {
		this.type = type;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DDLIndex) {
			DDLIndex index = (DDLIndex) obj;
			
			if (index.getTable().equals(table) && index.getField().equals(field)) {
				return true;
			}
			
			return false;
		}
		
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		int back = 0;
		
		if (table != null) {
			back += table.hashCode();
		}
		if (field != null) {
			back += field.hashCode();
		}
		back %= 2 << 10;
		
		return back;
	}
}
