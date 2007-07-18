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
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Daniel Spiewak
 */
class DBEncapsulator {
	private static final Map<Thread, DBEncapsulator> instances = new HashMap<Thread, DBEncapsulator>();
	private static final ReadWriteLock instancesLock = new ReentrantReadWriteLock();
	
	private DatabaseProvider provider;
	private Connection connection;
	
	private DBEncapsulator(DatabaseProvider provider) {
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

	public static DBEncapsulator getInstance(DatabaseProvider databaseProvider) {
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
