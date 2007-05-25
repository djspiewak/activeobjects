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
package net.java.ao.db;

import java.sql.Driver;

import net.java.ao.DatabaseProvider;
import net.java.ao.schema.ddl.DDLField;
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
		
		parseForeignKeys(append, table);
		
		if (append.length() > 0) {
			append.setLength(append.length() - ",\n".length());
			append.append('\n');
		}
		back.append(append);
		
		back.append(") ENGINE=InnoDB;");
		
		return back.toString();
	}
}
