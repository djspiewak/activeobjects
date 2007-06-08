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
package net.java.ao.db;

import java.sql.Connection;
import java.sql.SQLException;

import net.java.ao.DatabaseProvider;
import net.java.ao.PoolProvider;

import org.apache.commons.dbcp.BasicDataSource;

/**
 * @author Daniel Spiewak
 */
public class DBCPPoolProvider extends PoolProvider {
	private BasicDataSource ds;
	
	public DBCPPoolProvider(DatabaseProvider delegate) {
		super(delegate);
		
		ds = new BasicDataSource();
		try {
			ds.setDriverClassName(delegate.getDriverClass().getCanonicalName());
		} catch (ClassNotFoundException e) {
		}
		ds.setUsername(getUsername());
		ds.setPassword(getPassword());
		ds.setUrl(getURI());
	}
	
	@Override
	public Connection getConnection() throws SQLException {
		Connection conn = ds.getConnection();
		setPostConnectionProperties(conn);
		
		return conn;
	}

	@Override
	public void dispose() {
		try {
			ds.close();
		} catch (SQLException e) {
		}
		
		ds = null;
		
		super.dispose();
	}

	public static boolean isAvailable() {
		try {
			Class.forName("org.apache.commons.dbcp.BasicDataSource");
		} catch (ClassNotFoundException e) {
			return false;
		}
		
		return true;
	}
}
