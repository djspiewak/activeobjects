/*
 * Created on May 17, 2007
 */
package net.java.ao;

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
	
	protected Transaction() {
	}
	
	public void execute() throws SQLException {
		
	}
	
	public void rollback() throws SQLException {
		
	}
	
	public abstract void run();
}
