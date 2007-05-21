/*
 * Created on May 20, 2007
 */
package net.java.ao.schema.ddl;

/**
 * @author Daniel Spiewak
 */
public class DDLForeignKey {
	private String field = "";
	
	private String table = "";
	private String foreignField = "";

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}

	public String getTable() {
		return table;
	}

	public void setTable(String table) {
		this.table = table;
	}

	public String getForeignField() {
		return foreignField;
	}

	public void setForeignField(String foreignField) {
		this.foreignField = foreignField;
	}
	
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		
		if (obj instanceof DDLForeignKey) {
			DDLForeignKey key = (DDLForeignKey) obj;
			
			if (key.field.equals(field) && key.foreignField.equals(foreignField) && key.table.equals(table)) {
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		return field.hashCode() + table.hashCode() + foreignField.hashCode();
	}
}
