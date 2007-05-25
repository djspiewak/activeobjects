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
package net.java.ao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.java.ao.db.IDatabaseProvider;

/**
 * @author Daniel Spiewak
 */
class DBEncapsulator {
	private static final Map<Thread, DBEncapsulator> instances = new HashMap<Thread, DBEncapsulator>();
	private static final ReadWriteLock instancesLock = new ReentrantReadWriteLock();
	
	private IDatabaseProvider provider;
	private Connection connection;
	
	private DBEncapsulator(IDatabaseProvider provider) {
		this.provider = provider;
	}
	
	public Connection getConnection() throws SQLException {
		if (connection != null) {
			return connection;
		}
		
		return provider.getConnection();
	}
	
	void setConnection(Connection connection) {
		this.connection = connection;
	}
	
	boolean hasManualConnection() {
		return connection != null;
	}
	
	public void closeConnection(Connection connection) throws SQLException {
		if (!connection.equals(this.connection)) {
			connection.close();
		}
	}

	public static DBEncapsulator getInstance(IDatabaseProvider databaseProvider) {
		instancesLock.writeLock().lock();
		try {
			if (instances.containsKey(Thread.currentThread())) {
				return instances.get(Thread.currentThread());
			}
			
			DBEncapsulator instance = new DBEncapsulator(databaseProvider);
			instances.put(Thread.currentThread(), instance);
			
			return instance;
		} finally {
			instancesLock.writeLock().unlock();
		}
	}
	
	public static DBEncapsulator getInstance() {
		instancesLock.readLock().lock();
		try {
			return instances.get(Thread.currentThread());
		} finally {
			instancesLock.readLock().unlock();
		}
	}
}
