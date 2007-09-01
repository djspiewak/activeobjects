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

/**
 * To be implemented in the following way:
 * 
 * <pre>
 * final Room room = manager.getEntity(3, Room.class);
 * new Transaction() {
 *     public void run() {
 *         String name = room.getName();
 *         name += " (hang-out joint)";
 *         room.setName(name);
 *     }
 * }.execute();
 * </pre>
 * 
 * @author Daniel Spiewak
 */
public abstract class Transaction {
	private EntityManager manager;
	
	public Transaction(EntityManager manager) {
		this.manager = manager;
	}
	
	protected final EntityManager getEntityManager() {
		return manager;
	}
	
	public void execute() {
		Connection conn = null;
		
		try {
			conn = manager.getProvider().getConnection();
			((DelegateConnection) conn).setCloseable(false);
			
			conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
			conn.setAutoCommit(false);
			
			Transaction.this.run();
			
			conn.commit();
		} catch (SQLException e) {
		} finally {
			if (conn == null) {
				return;
			}
			
			try {
				conn.setAutoCommit(true);
				((DelegateConnection) conn).setCloseable(true);
				
				conn.close();
			} catch (SQLException e) {
			}
		}
	}
	
	public Thread executeConcurrently() {
		Thread back = new Thread() {
			@Override
			public void run() {
				execute();
			}
		};
		back.start();
		
		return back;
	}
	
	public abstract void run() throws SQLException;
}
