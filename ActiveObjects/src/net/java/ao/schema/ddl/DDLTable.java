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

/**
 * Database-agnostic representation of a table within the schema.  A
 * table logically contains fields, keys and indexes (as represented
 * by this class).
 * 
 * @author Daniel Spiewak
 */
public class DDLTable {
	private String name;
	
	private DDLField[] fields = {};
	private DDLForeignKey[] foreignKeys = {};
	private DDLIndex[] indexes = {};

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public DDLField[] getFields() {
		return fields;
	}

	public void setFields(DDLField[] fields) {
		this.fields = fields;
	}

	public DDLForeignKey[] getForeignKeys() {
		return foreignKeys;
	}

	public void setForeignKeys(DDLForeignKey[] foreignKeys) {
		this.foreignKeys = foreignKeys;
	}
	
	public DDLIndex[] getIndexes() {
		return indexes;
	}
	
	public void setIndexes(DDLIndex[] indexes) {
		this.indexes = indexes;
	}
	
	@Override
	public String toString() {
		return getName();
	}
	
	@Override
	public int hashCode() {
		int back = 0;
		
		if (name != null) {
			back += name.hashCode();
		}
		
		return back;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DDLTable) {
			DDLTable table = (DDLTable) obj;
			if (table == this) {
				return true;
			}
			
			if (table.getName() != null && table.getName().equals(name)) {
				return true;
			}
			
			return false;
		}
		
		return super.equals(obj);
	}
}
