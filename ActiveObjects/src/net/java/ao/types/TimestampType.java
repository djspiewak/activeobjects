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
import java.text.SimpleDateFormat;
import java.util.Calendar;

import net.java.ao.EntityManager;

/**
 * @author Daniel Spiewak
 */
class TimestampType extends DatabaseType<Calendar> {
	private DateFormat dateFormat;
	
	public TimestampType() {
		super(Types.TIMESTAMP, -1, Calendar.class);
		
		dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	}

	@Override
	public String getDefaultName() {
		return "TIMESTAMP";
	}
	
	@Override
	public void putToDatabase(int index, PreparedStatement stmt, Calendar value) throws SQLException {
		stmt.setTimestamp(index, new Timestamp(value.getTimeInMillis()));
	}
	
	@Override
	public Calendar pullFromDatabase(EntityManager manager, ResultSet res, Class<? extends Calendar> type, String field) throws SQLException {
		Calendar back = Calendar.getInstance();
		back.setTime(res.getTimestamp(field));
		
		return back;
	}

	@Override
	public Calendar defaultParseValue(String value) {
		try {
			Calendar cal = Calendar.getInstance();
			cal.setTime(dateFormat.parse(value));
			
			return cal;
		} catch (java.text.ParseException e) {
		}

		return Calendar.getInstance();
	}
	
	@Override
	public String valueToString(Object value) {
		return dateFormat.format(((Calendar) value).getTime());
	}
}
