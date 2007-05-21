/*
 * Created on May 21, 2007
 */
package net.java.ao;

/**
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
