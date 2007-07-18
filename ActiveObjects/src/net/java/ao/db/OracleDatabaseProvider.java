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

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.Types;

import net.java.ao.DatabaseProvider;
import net.java.ao.schema.ddl.DDLField;
import net.java.ao.schema.ddl.DDLTable;

/**
 * @author Daniel Spiewak
 */
public class OracleDatabaseProvider extends DatabaseProvider {

	public OracleDatabaseProvider(String uri, String username, String password) {
		super(uri, username, password);
	}

	@Override
	public Class<? extends Driver> getDriverClass() throws ClassNotFoundException {
		return (Class<? extends Driver>) Class.forName("oracle.jdbc.OracleDriver");
	}
	
	@Override
	protected void setPostConnectionProperties(Connection conn) throws SQLException {
	}
	
	@Override
	protected String convertTypeToString(int type) {
		switch (type) {
			case Types.INTEGER:
				return "NUMBER";

			case Types.NUMERIC:
				return "NUMBER";
				
			case Types.DECIMAL:
				return "NUMBER";
				
			case Types.SMALLINT:
				return "NUMBER";
				
			case Types.FLOAT:
				return "NUMBER";
				
			case Types.DOUBLE:
				return "NUMBER";
				
			case Types.REAL:
				return "NUMBER";
		}
		
		return super.convertTypeToString(type);
	}
	
	@Override
	protected String renderAutoIncrement() {
		return "INCREMENT_BY 1";
	}
	
	@Override
	protected String renderOnUpdate(DDLField field) {
		return "";
	}
	
	@Override
	protected String renderTriggerForField(DDLTable table, DDLField field) {
		if (field.getOnUpdate() != null) {
			StringBuilder back = new StringBuilder();
			String value = renderValue(field.getOnUpdate());
			
			back.append("CREATE TRIGGER ").append(table.getName()).append('_').append(field.getName()).append("onupdate\n");
			back.append("BEFORE UPDATE\n").append("    ON ").append(table.getName()).append("\n    FOR EACH ROW\n");
			back.append("BEGIN\n");
			back.append("    :new.").append(field.getName()).append(" := ").append(value).append(";\nEND");
			
			return back.toString();
		}
		
		return super.renderTriggerForField(table, field);
	}
}