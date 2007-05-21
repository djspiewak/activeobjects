/*
 * Created on May 20, 2007
 */
package net.java.ao.db.ddl;

/**
 * @author Daniel Spiewak
 */
public class DDLTable {
	private String name;
	
	private DDLField[] fields = {};
	private DDLForeignKey[] foreignKeys = {};

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
}
