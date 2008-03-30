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

import java.sql.Driver;
import java.sql.Types;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.java.ao.DatabaseProvider;
import net.java.ao.schema.ddl.DDLIndex;
import net.java.ao.types.DatabaseType;

/**
 * @author Daniel Spiewak
 */
public class MySQLDatabaseProvider extends DatabaseProvider {
	private static final Set<String> RESERVED_WORDS = new HashSet<String>() {
		{
			addAll(Arrays.asList("ADD", "ALL", "ALTER", "ANALYZE", "AND", "AS", "ASC", 
					"ASENSITIVE", "BEFORE", "BETWEEN", "BIGINT", "BINARY", "BLOB", 
					"BOTH", "BY", "CALL", "CASCADE", "CASE", "CHANGE", "CHAR", 
					"CHARACTER", "CHECK", "COLLATE", "COLUMN", "COLUMNS", "CONDITION", 
					"CONNECTION", "CONSTRAINT", "CONTINUE", "CONVERT", "CREATE", "CROSS", 
					"CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_USER", 
					"CURSOR", "DATABASE", "DATABASES", "DAY_HOUR", "DAY_MICROSECOND", 
					"DAY_MINUTE", "DAY_SECOND", "DEC", "DECIMAL", "DECLARE", "DEFAULT", 
					"DELAYED", "DELETE", "DESC", "DESCRIBE", "DETERMINISTIC", "DISTINCT", 
					"DISTINCTROW", "DIV", "DOUBLE", "DROP", "DUAL", "EACH", "ELSE", 
					"ELSEIF", "ENCLOSED", "ESCAPED", "EXISTS", "EXIT", "EXPLAIN", "FALSE", 
					"FETCH", "FIELDS", "FLOAT", "FLOAT4", "FLOAT8", "FOR", "FORCE", 
					"FOREIGN", "FROM", "FULLTEXT", "GOTO", "GRANT", "GROUP", "HAVING", 
					"HIGH_PRIORITY", "HOUR_MICROSECOND", "HOUR_MINUTE", "HOUR_SECOND", 
					"IF", "IGNORE", "IN", "INDEX", "INFILE", "INNER", "INOUT", "INSENSITIVE", 
					"INSERT", "INT", "INT1", "INT2", "INT3", "INT4", "INT8", "INTEGER", 
					"INTERVAL", "INTO", "IS", "ITERATE", "JOIN", "KEY", "KEYS", "KILL", 
					"LABEL", "LEADING", "LEAVE", "LEFT", "LIKE", "LIMIT", "LINES", "LOAD", 
					"LOCALTIME", "LOCALTIMESTAMP", "LOCK", "LONG", "LONGBLOB", "LONGTEXT", 
					"LOOP", "LOW_PRIORITY", "MATCH", "MEDIUMBLOB", "MEDIUMINT", "MEDIUMTEXT", 
					"MIDDLEINT", "MINUTE_MICROSECOND", "MINUTE_SECOND", "MOD", "MODIFIES", 
					"NATURAL", "NOT", "NO_WRITE_TO_BINLOG", "NULL", "NUMERIC", "ON", 
					"OPTIMIZE", "OPTION", "OPTIONALLY", "OR", "ORDER", "OUT", "OUTER", 
					"OUTFILE", "PRECISION", "PRIMARY", "PRIVILEGES", "PROCEDURE", "PURGE", 
					"READ", "READS", "REAL", "REFERENCES", "REGEXP", "RELEASE", "RENAME", 
					"REPEAT", "REPLACE", "REQUIRE", "RESTRICT", "RETURN", "REVOKE", "RIGHT", 
					"RLIKE", "SCHEMA", "SCHEMAS", "SECOND_MICROSECOND", "SELECT", "SENSITIVE", 
					"SEPARATOR", "SET", "SHOW", "SMALLINT", "SONAME", "SPATIAL", "SPECIFIC", 
					"SQL", "SQLEXCEPTION", "SQLSTATE", "SQLWARNING", "SQL_BIG_RESULT", 
					"SQL_CALC_FOUND_ROWS", "SQL_SMALL_RESULT", "SSL", "STARTING", 
					"STRAIGHT_JOIN", "TABLE", "TABLES", "TERMINATED", "THEN", "TINYBLOB", 
					"TINYINT", "TINYTEXT", "TO", "TRAILING", "TRIGGER", "TRUE", "UNDO", 
					"UNION", "UNIQUE", "UNLOCK", "UNSIGNED", "UPDATE", "UPGRADE", "USAGE", 
					"USE", "USING", "UTC_DATE", "UTC_TIME", "UTC_TIMESTAMP", "VALUES", 
					"VARBINARY", "VARCHAR", "VARCHARACTER", "VARYING", "WHEN", "WHERE", 
					"WHILE", "WITH", "WRITE", "XOR", "YEAR_MONTH", "ZEROFILL"));
		}
	};

	public MySQLDatabaseProvider(String uri, String username, String password) {
		super(uri, username, password);
	}

	@Override
	public Class<? extends Driver> getDriverClass() throws ClassNotFoundException {
		return (Class<? extends Driver>) Class.forName("com.mysql.jdbc.Driver");
	}
	
	@Override
	protected String convertTypeToString(DatabaseType<?> type) {
		switch (type.getType()) {
			case Types.CLOB:
				return "TEXT";
		}
		
		return super.convertTypeToString(type);
	}

	@Override
	protected String renderAutoIncrement() {
		return "AUTO_INCREMENT";
	}
	
	@Override
	protected String renderAppend() {
		return "ENGINE=InnoDB";
	}
	
	@Override
	protected String renderCreateIndex(DDLIndex index) {
		StringBuilder back = new StringBuilder("CREATE INDEX ");
		back.append(index.getName()).append(" ON ");
		back.append(index.getTable()).append('(').append(index.getField());
		
		if (index.getType().getType() == Types.CLOB || index.getType().getType() == Types.VARCHAR) {
			int defaultPrecision = index.getType().getDefaultPrecision();
			back.append('(').append(defaultPrecision > 0 ? defaultPrecision : 255).append(')');
		}
		back.append(')');
		
		return back.toString();
	}

	@Override
	protected Set<String> getReservedWords() {
		return RESERVED_WORDS;
	}
}
