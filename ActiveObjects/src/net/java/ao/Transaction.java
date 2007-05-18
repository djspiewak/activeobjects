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
	
	public void execute() throws SQLException {
		Connection conn = manager.getProvider().getConnection();
		conn.setAutoCommit(false);
		
		// TODO	come up with something clever here
		
		conn.commit();
		conn.close();
	}
	
	public abstract void run();
}
