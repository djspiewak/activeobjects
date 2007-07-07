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
	
	protected Transaction(EntityManager manager) {
		this.manager = manager;
	}
	
	public void execute() {
		Connection conn = null;
		
		try {
			conn = DBEncapsulator.getInstance(manager.getProvider()).getConnection();
			
			conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
			conn.setAutoCommit(false);
			
			DBEncapsulator.getInstance().setConnection(conn);
			
			Transaction.this.run();
			
			conn.commit();
		} catch (SQLException e) {
		} finally {
			if (conn == null) {
				return;
			}
			
			try {
				conn.setAutoCommit(true);
				
				conn.close();
				DBEncapsulator.getInstance().setConnection(null);
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
