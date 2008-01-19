/*
 * Copyright 2008 Daniel Spiewak
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import net.java.ao.Common;
import net.java.ao.EntityManager;

/**
 * @author Daniel Spiewak
 */
class BlobType extends DatabaseType<Object> {
	
	public BlobType() {
		super(Types.BLOB, -1, byte[].class, InputStream.class);
	}
	
	@Override
	public boolean shouldCache(Class<?> type) {
		if (Common.typeInstanceOf(type, InputStream.class)) {
			return false;
		}
		
		return super.shouldCache(type);
	}

	@Override
	public Object pullFromDatabase(EntityManager manager, ResultSet res, Class<?> type, String field) throws SQLException {
		if (manager.getProvider().getURI().startsWith("jdbc:postgres")) {
			if (type.equals(InputStream.class)) {
				return res.getBinaryStream(field);
			} else if (type.equals(byte[].class)) {
				return res.getBytes(field);
			}
			
			return null;
		}
		
		Blob blob = res.getBlob(field);
		
		if (type.equals(InputStream.class)) {
			// derby handles BLOBs oddly
			if (manager.getProvider().getURI().startsWith("jdbc:derby")) {
				return new ByteArrayInputStream(blob.getBytes(1, (int) blob.length()));
			}
			
			return blob.getBinaryStream();
		} else if (type.equals(byte[].class)) {
			return blob.getBytes(1, (int) blob.length());
		}
		
		return null;
	}
	
	@Override
	public void putToDatabase(int index, PreparedStatement stmt, Object value) throws SQLException {
		InputStream is = null;
		
		if (value instanceof byte[]) {
			is = new ByteArrayInputStream((byte[]) value);
		} else if (value instanceof InputStream) {
			is = (InputStream) value;
		} else {
			throw new IllegalArgumentException("BLOB value must be of type byte[] or InputStream");
		}
		
		boolean handled = false;
		try {		// for Java 6
			Method m = stmt.getClass().getMethod("setBinaryStream", int.class, InputStream.class);
			m.invoke(stmt, index, is);
			
			handled = true;
		} catch (Throwable t) {
		}
		
		if (!handled) {
			try {
				stmt.setBinaryStream(index, is, is.available());
			} catch (IOException e) {
				SQLException e2 = new SQLException(e.getMessage());
				e2.initCause(e);
				
				throw e2;
			}
		}
	}

	@Override
	public Object defaultParseValue(String value) {
		if (value.equalsIgnoreCase("null")) {
			return null;
		}
		
		throw new IllegalArgumentException("Cannot assign a String representation to a BLOB");
	}
	
	@Override
	public String valueToString(Object value) {
		if (value == null) {
			return null;
		}
		
		throw new IllegalArgumentException("Cannot assign a String representation to a BLOB");
	}

	@Override
	public String getDefaultName() {
		return "BLOB";
	}
}
