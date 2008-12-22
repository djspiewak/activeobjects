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
package net.java.ao.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.ao.DatabaseProvider;
import net.java.ao.Query;
import net.java.ao.schema.ddl.DDLField;
import net.java.ao.schema.ddl.DDLIndex;
import net.java.ao.schema.ddl.DDLTable;
import net.java.ao.types.DatabaseType;

/**
 * @author Daniel Spiewak
 */
abstract class DerbyDatabaseProvider extends DatabaseProvider {
	private static final Set<String> RESERVED_WORDS = new HashSet<String>() {
		{
			addAll(Arrays.asList("ADD", "ALL", "ALLOCATE", "ALTER", "AND", "ANY", "ARE", "AS", 
					"ASC", "ASSERTION", "AT", "AUTHORIZATION", "AVG", "BEGIN", "BETWEEN", "BIT", 
					"BOOLEAN", "BOTH", "BY", "CALL", "CASCADE", "CASCADED", "CASE", "CAST", 
					"CHAR", "CHARACTER", "CHECK", "CLOSE", "COLLATE", "COLLATION", "COLUMN", 
					"COMMIT", "CONNECT", "CONNECTION", "CONSTRAINT", "CONSTRAINTS", "CONTINUE", 
					"CONVERT", "CORRESPONDING", "COUNT", "CREATE", "CURRENT", "CURRENT_DATE", 
					"CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_USER", "CURSOR", "DEALLOCATE", 
					"DEC", "DECIMAL", "DECLARE", "DEFERRABLE", "DEFERRED", "DELETE", "DESC", 
					"DESCRIBE", "DIAGNOSTICS", "DISCONNECT", "DISTINCT", "DOUBLE", "DROP", "ELSE", 
					"END", "ENDEXEC", "ESCAPE", "EXCEPT", "EXCEPTION", "EXEC", "EXECUTE", "EXISTS", 
					"EXPLAIN", "EXTERNAL", "FALSE", "FETCH", "FIRST", "FLOAT", "FOR", "FOREIGN", 
					"FOUND", "FROM", "FULL", "FUNCTION", "GET", "GET_CURRENT_CONNECTION", "GLOBAL", 
					"GO", "GOTO", "GRANT", "GROUP", "HAVING", "HOUR", "IDENTITY", "IMMEDIATE", 
					"IN", "INDICATOR", "INITIALLY", "INNER", "INOUT", "INPUT", "INSENSITIVE", 
					"INSERT", "INT", "INTEGER", "INTERSECT", "INTO", "IS", "ISOLATION", "JOIN", 
					"KEY", "LAST", "LEFT", "LIKE", "LONGINT", "LOWER", "LTRIM", "MATCH", "MAX", 
					"MIN", "MINUTE", "NATIONAL", "NATURAL", "NCHAR", "NVARCHAR", "NEXT", "NO", 
					"NOT", "NULL", "NULLIF", "NUMERIC", "OF", "ON", "ONLY", "OPEN", "OPTION", "OR", 
					"ORDER", "OUT", "OUTER", "OUTPUT", "OVERLAPS", "PAD", "PARTIAL", "PREPARE", 
					"PRESERVE", "PRIMARY", "PRIOR", "PRIVILEGES", "PROCEDURE", "PUBLIC", "READ", 
					"REAL", "REFERENCES", "RELATIVE", "RESTRICT", "REVOKE", "RIGHT", "ROLLBACK", 
					"ROWS", "RTRIM", "SCHEMA", "SCROLL", "SECOND", "SELECT", "SESSION_USER", "SET", 
					"SMALLINT", "SOME", "SPACE", "SQL", "SQLCODE", "SQLERROR", "SQLSTATE", "SUBSTR", 
					"SUBSTRING", "SUM", "SYSTEM_USER", "TABLE", "TEMPORARY", "TIMEZONE_HOUR", 
					"TIMEZONE_MINUTE", "TO", "TRAILING", "TRANSACTION", "TRANSLATE", "TRANSLATION", 
					"TRUE", "UNION", "UNIQUE", "UNKNOWN", "UPDATE", "UPPER", "USER", "USING", 
					"VALUES", "VARCHAR", "VARYING", "VIEW", "WHENEVER", "WHERE", "WITH", "WORK", 
					"WRITE", "XML", "XMLEXISTS", "XMLPARSE", "XMLSERIALIZE", "YEAR"));
		}
	};

	protected DerbyDatabaseProvider(String uri, String username, String password) {
		super(uri, username, password);
	}
	
	@Override
	public void setQueryStatementProperties(Statement stmt, Query query) throws SQLException {
		int limit = query.getLimit();
		
		if (limit >= 0) {
//			if (query.getOffset() > 0) {
//				limit += query.getOffset();
//			}
			
			stmt.setFetchSize(limit);
			stmt.setMaxRows(limit);
		}
	}
	
	@Override
	public void setQueryResultSetProperties(ResultSet res, Query query) throws SQLException {
		if (query.getOffset() > 0) {
			res.absolute(query.getOffset());
		}
	}
	
	@Override
	public ResultSet getTables(Connection conn) throws SQLException {
		return conn.getMetaData().getTables("APP", null, null, new String[] {"TABLE"});
	}
	
	@Override
	public Object parseValue(int type, String value) {
		if (value == null || value.equals("") || value.equals("NULL")) {
			return null;
		}
		
		switch (type) {
			case Types.TIMESTAMP:
				Matcher matcher = Pattern.compile("'(.+)'.*").matcher(value);
				if (matcher.find()) {
					value = matcher.group(1);
				}
			break;

			case Types.DATE:
				matcher = Pattern.compile("'(.+)'.*").matcher(value);
				if (matcher.find()) {
					value = matcher.group(1);
				}
			break;
			
			case Types.TIME:
				matcher = Pattern.compile("'(.+)'.*").matcher(value);
				if (matcher.find()) {
					value = matcher.group(1);
				}
			break;
		}
		
		return super.parseValue(type, value);
	}
	
	@Override
	protected void setPostConnectionProperties(Connection conn) throws SQLException {
		Statement stmt = conn.createStatement();
		
		stmt.executeUpdate("SET SCHEMA app");
		stmt.close();
	}
	
	@Override
	protected String renderQueryLimit(Query query) {
		return "";
	}
	
	@Override
	protected String convertTypeToString(DatabaseType<?> type) {
		switch (type.getType()) {
			case Types.TINYINT:
				return "SMALLINT";
				
			case Types.BOOLEAN:
				return "SMALLINT";
				
			case Types.BIT:
				return "SMALLINT";
		}
		
		return super.convertTypeToString(type);
	}
	
	@Override
	protected String renderAutoIncrement() {
		return "GENERATED BY DEFAULT AS IDENTITY";
	}
	
	@Override
	protected String renderOnUpdate(DDLField field) {
		return "";
	}
	
	@Override
	protected String renderTriggerForField(DDLTable table, DDLField field) {
		Object onUpdate = field.getOnUpdate();
		if (onUpdate != null) {
			StringBuilder back = new StringBuilder();
			
			DDLField pkField = null;
			for (DDLField f : table.getFields()) {
				if (f.isPrimaryKey()) {
					pkField = f;
					break;
				}
			}
			
			if (pkField == null) {
				throw new IllegalArgumentException("No primary key field found in table '" + table.getName() + '\'');
			}
			
			back.append("CREATE TRIGGER ").append(processID(table.getName() + '_' + field.getName()+ "_onupdate") + '\n');
			back.append("    AFTER UPDATE ON ").append(processID(table.getName()));
			back.append("\n    REFERENCING NEW AS inserted\n    FOR EACH ROW MODE DB2SQL\n        ");
			back.append("UPDATE ").append(processID(table.getName())).append(" SET ").append(
					processID(field.getName())).append(" = ").append(renderValue(onUpdate));
			back.append("\n            WHERE " + processID(pkField.getName()) + " = inserted." 
					+ processID(pkField.getName()) + " AND inserted.");
			back.append(processID(field.getName())).append(" <> ").append(renderValue(onUpdate));
			
			return back.toString();
		}
		
		return super.renderTriggerForField(table, field);
	}
	
	@Override
	protected String getTriggerNameForField(DDLTable table, DDLField field) {
		if (field.getOnUpdate() != null) {
			return processID(table.getName() + '_' + field.getName() + "_onupdate");
		}
		
		return super.getTriggerNameForField(table, field);
	}
	
	@Override
	protected boolean considerPrecision(DDLField field) {
		boolean considerPrecision = true;
		switch (field.getType().getType()) {
			case Types.BIGINT:
				considerPrecision = false;
			break;
			
			case Types.DATE:
				considerPrecision = false;
			break;
			
			case Types.DOUBLE:
				considerPrecision = false;
			break;
			
			case Types.INTEGER:
				considerPrecision = false;
			break;
			
			case Types.REAL:
				considerPrecision = false;
			break;
			
			case Types.SMALLINT:
				considerPrecision = false;
			break;
			
			case Types.TIME:
				considerPrecision = false;
			break;
			
			case Types.TIMESTAMP:
				considerPrecision = false;
			break;
			
			case Types.TINYINT:
				considerPrecision = false;
			break;
			
			case Types.BIT:
				considerPrecision = false;
			break;
		}
		
		return considerPrecision;
	}
	
	@Override
	protected String[] renderAlterTableChangeColumn(DDLTable table, DDLField oldField, DDLField field) {
		System.err.println("WARNING: Derby doesn't support CHANGE TABLE statements");
		System.err.println("WARNING: Migration may not be entirely in sync as a result");
		
		return new String[0];
	}
	
	@Override
	protected String[] renderAlterTableDropColumn(DDLTable table, DDLField field) {
		System.err.println("WARNING: Derby doesn't support ALTER TABLE DROP COLUMN statements");
		
		return new String[0];
	}
	
	@Override
	protected String renderDropIndex(DDLIndex index) {
		StringBuilder back = new StringBuilder("DROP INDEX ");
		
		back.append(processID(index.getName()));
		
		return back.toString();
	}

	@Override
	protected Set<String> getReservedWords() {
		return RESERVED_WORDS;
	}
	
	@Override
	public boolean isCaseSensetive() {
		return false;
	}
}
