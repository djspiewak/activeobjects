/*
 * Created on May 18, 2007
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
