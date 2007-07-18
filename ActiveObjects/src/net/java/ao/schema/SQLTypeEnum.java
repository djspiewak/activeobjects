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
package net.java.ao.schema;

import java.io.InputStream;
import java.net.URL;
import java.sql.Types;
import java.util.Calendar;
import java.util.Date;

/**
 * @author Daniel Spiewak
 */
enum SQLTypeEnum {
	TINYINT(short.class, Types.TINYINT, -1),
	TINYINT_OBJ(Short.class, Types.TINYINT, -1),
	INTEGER(int.class, Types.INTEGER, -1),
	INTEGER_OBJ(int.class, Types.INTEGER, -1),
	BIGINT(long.class, Types.BIGINT, -1),
	BIGINT_OBJ(Long.class, Types.BIGINT, -1),
	FLOAT(float.class, Types.FLOAT, -1),
	FLOAT_OBJ(Float.class, Types.FLOAT, -1),
	DOUBLE(double.class, Types.DOUBLE, -1),
	DOUBLE_OBJ(Double.class, Types.DOUBLE, -1),
	CHAR(char.class, Types.CHAR, -1),
	CHAR_OBJ(Character.class, Types.CHAR, -1),
	BOOLEAN(boolean.class, Types.BOOLEAN, -1),
	BOOLEAN_OBJ(Boolean.class, Types.BOOLEAN, -1),
	VARCHAR(String.class, Types.VARCHAR, 45),
	URL(URL.class, Types.VARCHAR, 255),
	BLOB(InputStream.class, Types.BLOB, -1),
	TIMESTAMP(Calendar.class, Types.TIMESTAMP, -1),
	TIMESTAMP_DATE(Date.class, Types.TIMESTAMP, -1);
	
	private final Class<?> type;
	private final int sqlType;
	private final int precision;
	
	private SQLTypeEnum(Class<?> type, int sqlType, int precision) {
		this.type = type;
		this.sqlType = sqlType;
		this.precision = precision;
	}
	
	public Class<?> getType() {
		return type;
	}

	public int getSQLType() {
		return sqlType;
	}
	
	public int getPrecision() {
		return precision;
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
