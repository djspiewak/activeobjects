/*
 * Created on May 16, 2007
 */
package net.java.ao.db;

import java.sql.Driver;

import net.java.ao.DatabaseProvider;
import net.java.ao.schema.ddl.DDLField;
import net.java.ao.schema.ddl.DDLForeignKey;
import net.java.ao.schema.ddl.DDLTable;

/**
 * @author Daniel Spiewak
 */
public class MySQLDatabaseProvider extends DatabaseProvider {

	public MySQLDatabaseProvider(String uri, String username, String password) {
		super(uri, username, password);
	}

	public Class<? extends Driver> getDriverClass() throws ClassNotFoundException {
		return (Class<? extends Driver>) Class.forName("com.mysql.jdbc.Driver");
	}
	
	public String render(DDLTable table) {
		StringBuilder back = new StringBuilder("CREATE TABLE ");
		back.append(table.getName());
		back.append(" (\n");
		
		StringBuilder append = new StringBuilder();
		for (DDLField field : table.getFields()) {
			back.append("    ");
			back.append(field.getName());
			back.append(" ");
			back.append(convertTypeToString(field.getType()));
			
			if (field.getPrecision() > 0) {
				back.append('(');
				if (field.getScale() > 0) {
					back.append(field.getPrecision());
					back.append(',');
					back.append(field.getScale());
				} else {
					back.append(field.getPrecision());
				}
				back.append(')');
			}
			
			if (field.isNotNull()) {
				back.append(" NOT NULL");
			}
			if (field.isAutoIncrement()) {
				back.append(" AUTO_INCREMENT");
			}
			if (field.isUnique()) {
				back.append(" UNIQUE");
			}
			if (field.isPrimaryKey()) {
				append.append("    PRIMARY KEY (");
				append.append(field.getName());
				append.append("),\n");
			}
			
			back.append(",\n");
		}
		
		for (DDLForeignKey key : table.getForeignKeys()) {
			append.append("    FOREIGN KEY ");
			append.append(key.getField());
			append.append(" RESTRICT ");
			append.append(key.getTable());
			append.append('(');
			append.append(key.getForeignField());
			append.append("),\n");
		}
		
		if (append.length() > 0) {
			append.setLength(append.length() - ",\n".length());
			append.append('\n');
		}
		back.append(append);
		
		back.append(");");
		
		return back.toString();
	}
}
