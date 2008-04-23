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

/**
 * Class designed to encapsulate a single field/value parameter to be passed
 * to {@link EntityManager#create(Class, DBParam...)}.  This class is literally
 * nothing more than a value container.  There is no checking done to ensure
 * that the values match the type of the field.  Thus the responsibility of value/field
 * matching lies with the developer.
 * 
 * @author Daniel Spiewak
 */
public class DBParam {
	private String field;
	private Object value;
	
	public DBParam(String field, Object value) {
		this.field = field;
		this.value = value;
	}

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		
		if (obj instanceof DBParam) {
			DBParam param = (DBParam) obj;
			
			if (param.field.equals(field) && param.value.equals(value)) {
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		return field.hashCode() + value.hashCode();
	}
}
