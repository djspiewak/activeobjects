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

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import net.java.ao.EntityManager;

/**
 * @author Nathan Hamblen
 */
class URIType extends DatabaseType<URI> {

	public URIType() {
		super(Types.VARCHAR, 255, URI.class);
	}

	@Override
	public String getDefaultName() {
		return "VARCHAR";
	}
	
	@Override
	public void putToDatabase(int index, PreparedStatement stmt, URI value) throws SQLException {
		stmt.setString(index, value.toString());
	}
	
	@Override
	public URI pullFromDatabase(EntityManager manager, ResultSet res, Class<? extends URI> type, String field) throws SQLException {
		try {
			return new URI(res.getString(field));
		} catch (URISyntaxException e) {
			throw new SQLException(e.getMessage());
		}
	}

	@Override
	public URI defaultParseValue(String value) {
		try {
			return new URI(value);
		} catch (URISyntaxException e) {
		}
		
		return null;
	}
}
