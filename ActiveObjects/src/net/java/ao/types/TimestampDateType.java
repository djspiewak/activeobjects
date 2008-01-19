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
package net.java.ao.types;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.java.ao.EntityManager;

/**
 * @author Daniel Spiewak
 */
class TimestampDateType extends DatabaseType<Date> {
	private DateFormat dateFormat;
	
	public TimestampDateType() {
		super(Types.TIMESTAMP, -1, Date.class);
		
		dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	}

	@Override
	public String getDefaultName() {
		return "TIMESTAMP";
	}
	
	@Override
	public void putToDatabase(int index, PreparedStatement stmt, Date value) throws SQLException {
		stmt.setTimestamp(index, new Timestamp(value.getTime()));
	}
	
	@Override
	public Date pullFromDatabase(EntityManager manager, ResultSet res, Class<? extends Date> type, String field) throws SQLException {
		return res.getTimestamp(field);
	}

	@Override
	public Date defaultParseValue(String value) {
		try {
			return dateFormat.parse(value);
		} catch (ParseException e) {
		}
		
		return new Date();
	}
	
	@Override
	public String valueToString(Object value) {
		return dateFormat.format((Date) value);
	}
}
