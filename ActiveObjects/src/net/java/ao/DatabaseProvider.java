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
package net.java.ao;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import net.java.ao.db.SupportedDBProvider;
import net.java.ao.db.SupportedPoolProvider;
import net.java.ao.schema.ddl.DDLField;
import net.java.ao.schema.ddl.DDLForeignKey;
import net.java.ao.schema.ddl.DDLTable;

/**
 * @author Daniel Spiewak
 */
public abstract class DatabaseProvider {
	private String uri, username, password;
	
	protected DatabaseProvider(String uri, String username, String password) {
		this.uri = uri;
		
		this.username = username;
		this.password = password;
	}
	
	public abstract Class<? extends Driver> getDriverClass() throws ClassNotFoundException;
	
	protected abstract void setPostConnectionProperties(Connection conn) throws SQLException;
	
	protected abstract String renderAutoIncrement();
	
	public String render(DDLTable table) {
		StringBuilder back = new StringBuilder("CREATE TABLE ");
		back.append(table.getName());
		back.append(" (\n");
		
		List<String> primaryKeys = new LinkedList<String>();
		StringBuilder append = new StringBuilder();
		for (DDLField field : table.getFields()) {
			back.append(renderField(field));
			
			if (field.isPrimaryKey()) {
				primaryKeys.add(field.getName());
			}
		}
		
		parseForeignKeys(append, table);
		
		back.append(append);
		
		if (primaryKeys.size() > 0) {
			back.append("    PRIMARY KEY(");
			back.append(primaryKeys.get(0));
			
			for (int i = 1; i < primaryKeys.size(); i++) {
				back.append(",");
				back.append(primaryKeys.get(i));
			}
			back.append(")\n");
		}
		
		back.append(")");
		
		String tailAppend = renderAppend();
		if (tailAppend != null) {
			back.append(' ');
			back.append(tailAppend);
		}
		
		return back.toString();
	}
	
	public String[] renderTriggers(DDLTable table) {
		List<String> back = new ArrayList<String>();
		
		for (DDLField field : table.getFields()) {
			String trigger = renderTriggerForField(table, field);
			if (trigger != null) {
				back.add(trigger);
			}
		}
		
		return back.toArray(new String[back.size()]);
	}
	
	public String getURI() {
		return uri;
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public Connection getConnection() throws SQLException {
		try {
			getDriverClass();
		} catch (ClassNotFoundException e) {
			return null;
		}
		
		Connection conn = DriverManager.getConnection(getURI(), getUsername(), getPassword());
		setPostConnectionProperties(conn);
		
		return conn;
	}
	
	public void dispose() {
	}
	
	protected void parseForeignKeys(StringBuilder append, DDLTable table) {
		for (DDLForeignKey key : table.getForeignKeys()) {
			append.append("    FOREIGN KEY (");
			append.append(key.getField());
			append.append(") REFERENCES ");
			append.append(key.getTable());
			append.append('(');
			append.append(key.getForeignField());
			append.append("),\n");
		}
	}
	
	protected int sanitizeType(int type) {
		return type;
	}

	protected String convertTypeToString(int type) {
		type = sanitizeType(type);
		
		switch (type) {
			case Types.BIGINT:
				return "BIGINT";
				
			case Types.BINARY:
				return "BINARY";
				
			case Types.BIT:
				return "BIT";
				
			case Types.BLOB:
				return "BLOB";
				
			case Types.BOOLEAN:
				return "BOOLEAN";
				
			case Types.CHAR:
				return "CHAR";
				
			case Types.CLOB:
				return "CLOB";
				
			case Types.DATE:
				return "DATE";
				
			case Types.DECIMAL:
				return "DECIMAL";
				
			case Types.DOUBLE:
				return "DOUBLE";
				
			case Types.FLOAT:
				return "FLOAT";
				
			case Types.INTEGER:
				return "INTEGER";
				
			case Types.LONGVARBINARY:
				return "LONGVARBINARY";
			
			case Types.LONGVARCHAR:
				return "LONGVARCHAR";
				
			case Types.NULL:
				return "NULL";
				
			case Types.NUMERIC:
				return "NUMERIC";
				
			case Types.REAL:
				return "REAL";
				
			case Types.REF:
				return "REF";
				
			case Types.SMALLINT:
				return "SMALLINT";
				
			case Types.SQLXML:
				return "SQLXML";
				
			case Types.STRUCT:
				return "STRUCT";
				
			case Types.TIME:
				return "TIME";
				
			case Types.TIMESTAMP:
				return "TIMESTAMP";
				
			case Types.VARBINARY:
				return "VARBINARY";
				
			case Types.VARCHAR:
				return "VARCHAR";
		}
		
		return null;
	}
	
	protected String renderAppend() {
		return null;
	}
	
	protected String renderField(DDLField field) {
		StringBuilder back = new StringBuilder();
		
		back.append("    ");
		back.append(field.getName());
		back.append(" ");
		back.append(renderFieldType(field));
		
		if (considerPrecision(field) && field.getPrecision() > 0) {
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
			back.append(' ');
			back.append(renderAutoIncrement());
		} else if (field.getDefaultValue() != null) {
			back.append(" DEFAULT ");
			back.append(renderValue(field.getDefaultValue()));
		}
		if (field.getOnUpdate() != null) {
			back.append(renderOnUpdate(field));
		}
		if (field.isUnique()) {
			back.append(" UNIQUE");
		}
		
		back.append(",\n");
		return back.toString();
	}
	
	protected String renderValue(Object value) {
		if (value == null) {
			return "NULL";
		} else if (value instanceof Calendar) {
			return "'" + renderCalendar((Calendar) value) + "'";
		} else if (value instanceof Boolean) {
			return ((Boolean) value ? "1" : "0");
		} else if (value instanceof DatabaseFunction) {
			return renderFunction((DatabaseFunction) value);
		}
		
		return value.toString();
	}
	
	protected String renderCalendar(Calendar calendar) {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(calendar.getTime());
	}
	
	protected String renderFieldType(DDLField field) {
		return convertTypeToString(field.getType());
	}

	protected String renderFunction(DatabaseFunction func) {
		switch (func) {
			case CURRENT_DATE:
				return "CURRENT_DATE";
				
			case CURRENT_TIMESTAMP:
				return "CURRENT_TIMESTAMP";
		}
		
		return null;
	}
	
	protected String renderOnUpdate(DDLField field) {
		StringBuilder back = new StringBuilder();
		
		back.append(" ON UPDATE ");
		back.append(renderValue(field.getOnUpdate()));
		
		return back.toString();
	}
	
	protected boolean considerPrecision(DDLField field) {
		return true;
	}
	
	protected String renderTriggerForField(DDLTable table, DDLField field) {
		return null;
	}
	
	public final static DatabaseProvider getInstance(String uri, String username, String password) {
		return getInstance(uri, username, password, true);		// enable pooling by default (if available)
	}
	
	public final static DatabaseProvider getInstance(String uri, String username, String password, boolean enablePooling) {
		SupportedDBProvider provider = SupportedDBProvider.getProviderForURI(uri);
		if (provider == null) {
			throw new RuntimeException("Unable to locate a valid database provider for URI: " + uri);
		}
		
		DatabaseProvider back = provider.createInstance(uri, username, password);
		if (back == null) {
			throw new RuntimeException("Unable to instantiate database provider for URI: " + uri);
		}
		
		if (enablePooling) {
			for (SupportedPoolProvider supportedProvider : SupportedPoolProvider.values()) {
				Class<? extends PoolProvider> providerClass = supportedProvider.getProvider();
				
				try {
					if ((Boolean) providerClass.getMethod("isAvailable").invoke(null)) {
						back = providerClass.getConstructor(DatabaseProvider.class).newInstance(back);
						break;
					}
				} catch (IllegalArgumentException e) {
					continue;
				} catch (SecurityException e) {
					continue;
				} catch (IllegalAccessException e) {
					continue;
				} catch (InvocationTargetException e) {
					continue;
				} catch (NoSuchMethodException e) {
					continue;
				} catch (InstantiationException e) {
					continue;
				}
			}
		}
		
		return back;
	}
}
