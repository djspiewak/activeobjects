/*
 * Created on May 4, 2007
 */
package net.java.ao.schema;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;

/**
 * @author Daniel Spiewak
 */
public enum SQLTypeEnum {
	TINYINT(short.class, "TINYINT"),
	TINYINT_OBJ(Short.class, "TINYINT"),
	INTEGER(int.class, "INTEGER"),
	INTEGER_OBJ(int.class, "INTEGER"),
	BIGINT(long.class, "BIGINT"),
	BIGINT_OBJ(Long.class, "BIGINT"),
	CHAR(char.class, "CHAR"),
	CHAR_OBJ(Character.class, "CHAR"),
	BOOLEAN(boolean.class, "TINYINT(1)"),
	BOOLEAN_OBJ(Boolean.class, "TINYINT(1)"),
	VARCHAR(String.class, "VARCHAR(45)"),
	TEXT(String.class, "TEXT"),
	BLOB(InputStream.class, "BLOB"),
	TIMESTAMP(Calendar.class, "TIMESTAMP"),
	TIMESTAMP_DATE(Date.class, "TIMESTAMP");
	
	private final Class<?> type;
	private final String sqlName;
	
	private SQLTypeEnum(Class<?> type, String sqlName) {
		this.type = type;
		this.sqlName = sqlName;
	}
	
	public Class<?> getType() {
		return type;
	}

	public String getSqlName() {
		return sqlName;
	}

	public static SQLTypeEnum getType(Class<?> type) {
		for (SQLTypeEnum value : values()) {
			if (value.type.equals(type)) {
				return value;
			}
		}
		return null;
	}
}
