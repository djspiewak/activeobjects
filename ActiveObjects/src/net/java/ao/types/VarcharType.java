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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * @author Daniel Spiewak
 */
public class VarcharType extends DatabaseType<String> {

	public VarcharType() {
		super(Types.VARCHAR, 45, String.class);
	}
	
	public String getDefaultName() {
		return "VARCHAR";
	}
	
	@Override
	public String convert(ResultSet res, String field) throws SQLException {
		return res.getString(field);
	}
}
