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
package net.java.ao.schema;

import java.io.InputStream;
import java.sql.Types;
import java.util.Calendar;
import java.util.Date;

/**
 * @author Daniel Spiewak
 */
public enum SQLTypeEnum {
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
