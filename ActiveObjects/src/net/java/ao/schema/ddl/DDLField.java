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
 * Database-agnostic representation of a field within a table containing
 * all associated attributes such as type and constraints.  The only
 * field-relative attribute not contained within this class is whether
 * or not the field is indexed at the database level.
 * 
 * @author Daniel Spiewak
 */
public class DDLField {
	private String name;
	
	private DatabaseType<?> type;
	private int precision;
	private int scale;
	
	private boolean primaryKey;
	private boolean autoIncrement;
	private boolean notNull;
	private boolean unique;
	
	private Object defaultValue;
	private Object onUpdate;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public DatabaseType<?> getType() {
		return type;
	}

	public void setType(DatabaseType<?> type) {
		this.type = type;
	}

	public int getPrecision() {
		if (precision <= 0) {
			if (type != null && type.getDefaultPrecision() > 0) {
				return type.getDefaultPrecision();
			}
		}
		
		return precision;
	}

	public void setPrecision(int precision) {
		this.precision = precision;
	}

	public int getScale() {
		return scale;
	}

	public void setScale(int scale) {
		this.scale = scale;
	}

	public boolean isPrimaryKey() {
		return primaryKey;
	}

	public void setPrimaryKey(boolean primaryKey) {
		this.primaryKey = primaryKey;
	}

	public boolean isAutoIncrement() {
		return autoIncrement;
	}

	public void setAutoIncrement(boolean autoIncrement) {
		this.autoIncrement = autoIncrement;
	}

	public boolean isNotNull() {
		return notNull;
	}

	public void setNotNull(boolean notNull) {
		this.notNull = notNull;
	}

	public boolean isUnique() {
		return unique;
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	public Object getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = defaultValue;
	}

	public Object getOnUpdate() {
		return onUpdate;
	}

	public void setOnUpdate(Object onUpdate) {
		this.onUpdate = onUpdate;
	}
	
	@Override
	public String toString() {
		return getName() + "(" + getPrecision() + "," + getScale() + ")";
	}
	
	@Override
	public int hashCode() {
		int back = type.hashCode();
		
		if (name != null) {
			back += name.hashCode();
		}
		
		return back;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DDLField) {
			DDLField field = (DDLField) obj;
			if (field == this) {
				return true;
			}
			
			if ((field.getName() == null || field.getName().equals(name))
					&& field.getType() == type) {
				return true;
			}
			
			return false;
		}
		
		return super.equals(obj);
	}
}
