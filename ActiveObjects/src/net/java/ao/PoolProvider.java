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
package net.java.ao;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;

import net.java.ao.schema.ddl.DDLTable;

/**
 * @author Daniel Spiewak
 */
public abstract class PoolProvider extends DatabaseProvider {
	private DatabaseProvider delegate;

	protected PoolProvider(DatabaseProvider delegate) {
		super(delegate.getURI(), delegate.getUsername(), delegate.getPassword());
		
		this.delegate = delegate;
	}
	
	@Override
	public Class<? extends Driver> getDriverClass() throws ClassNotFoundException {
		return null;
	}

	public final DatabaseProvider getDelegate() {
		return delegate;
	}
	
	@Override
	public String render(DDLTable table) {
		return delegate.render(table);
	}
	
	@Override
	protected void setPostConnectionProperties(Connection conn) throws SQLException {
		delegate.setPostConnectionProperties(conn);
	}
	
	@Override
	protected String renderAutoIncrement() {
		return delegate.renderAutoIncrement();
	}
	
	@Override
	public void dispose() {
		delegate.dispose();
		
		super.dispose();
	}
}
