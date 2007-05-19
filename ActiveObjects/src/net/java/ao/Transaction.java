/*
 * Created on May 17, 2007
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
		try {
			executeConcurrently().join();
		} catch (InterruptedException e) {
		}
	}
	
	public Thread executeConcurrently() {
		Thread back = new Thread() {
			@Override
			public void run() {
				Connection conn = null;
				boolean committed = false;
				
				try {
					conn = DBEncapsulator.getInstance(manager.getProvider()).getConnection();
					conn.setAutoCommit(false);
					DBEncapsulator.getInstance().setConnection(conn);
					
					Transaction.this.run();
					
					conn.commit();
					committed = true;
				} catch (SQLException e) {
				} finally {
					if (conn == null) {
						return;
					}
					
					try {
						if (!committed) {
							conn.rollback();
						}
						
						conn.close();
						DBEncapsulator.getInstance().setConnection(null);
					} catch (SQLException e) {
					}
				}
			}
		};
		back.start();
		
		return back;
	}
	
	public abstract void run() throws SQLException;
}
