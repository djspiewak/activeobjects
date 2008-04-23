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
 * <p>Allows for the syntactically simple use of database transactions within the
 * ActiveObjects API.  This class's syntax is modeled after the <code>transaction
 * do ... end</code> syntax provided by Rails's ActiveRecord ORM.  The intention
 * is to provide the simplest possible encapsulation around the transaction
 * functionality.  As such, AO transactions lack some of the power of the
 * underlying database transaction function (such as arbitrary save-points).</p>
 * 
 * <p>The design behind <code>Transaction</code> is modeled after the
 * following code snippet:</p>
 * 
 * <pre>new Transaction&lt;Object&gt;(manager) {
 *     public Object run() {
 *         Account a = getEntityManager().get(Account.class, 1);
 *         Account b = getEntityManager().get(Account.class, 2);
 *         
 *         a.setBalance(a.getBalance() - 1000);
 *         a.save();
 *         
 *         b.setBalance(b.getBalance() + 1000);
 *         b.save();
 *         
 *         return null;
 *     }
 * }.execute();</pre>
 * 
 * <p>The transaction will be committed only after the <code>run()</code>
 * method returns.  Thus, <code>a.save()</code> doesn't immediately modify
 * the database values, only upon the committal of the transaction.  If any
 * conflicts are detected, JDBC will automatically throw an {@link SQLException}.
 * <code>Transaction</code> catches this exception and rolls back the
 * transaction, ensuring data integrity.  Once the transaction is rolled back, the
 * exception is rethrown from the <code>execute()</code> method.</p>
 * 
 * <p>In cases where the transaction generates data which must be returned, this
 * can be accomplished by returning from the {@link #run()} method against the
 * parameterized type.  Thus if a transaction to create an account is utilized:</p>
 * 
 * <pre>Account result = new Transaction&lt;Account&gt;(manager) {
 *     public Account run() throws SQLException {
 *         Account back = getEntityManager().create(Account.class);
 *         
 *         back.setBalance(0);
 *         back.save():
 *         
 *         return back;
 *     }
 * }.execute();</pre>
 * 
 * <p>The value returned from <code>run()</code> will be passed back up the call
 * stack to <code>execute()</code>, which will return the value to the caller.
 * Thus in this example, <code>result</code> will be precisely the <code>back</code>
 * instance from within the transaction.  This feature allows data to escape the
 * scope of the transaction, thereby achieving a greater usefulness.</p>
 * 
 * <p>The JDBC transaction type used is {@link Connection#TRANSACTION_SERIALIZABLE}.</p>
 * 
 * @author Daniel Spiewak
 * @see java.sql.Connection
 */
public abstract class Transaction<T> {
	private EntityManager manager;
	
	/**
	 * Creates a new <code>Transaction</code> using the specified
	 * {@link EntityManager} instance.  If the specified instance is <code>null</code>,
	 * an exception will be thrown.
	 * 
	 * @param manager	The <code>EntityManager</code> instance against which the
	 * 	transaction should run.
	 * @throws IllegalArgumentException	If the {@link EntityManager} instance is <code>null</code>.
	 */
	public Transaction(EntityManager manager) {
		if (manager == null) {
			throw new IllegalArgumentException("EntityManager instance cannot be null");
		}
		
		this.manager = manager;
	}
	
	protected final EntityManager getEntityManager() {
		return manager;
	}
	
	/**
	 * <p>Executes the transaction defined within the overridden {@link #run()}
	 * method.  If the transaction fails for any reason (such as a conflict), it will
	 * be rolled back and an exception thrown.  The value returned from the
	 * <code>run()</code> method will be returned from <code>execute()</code>.</p>
	 * 
	 * <p>Custom JDBC code can be executed within a transaction.  However, one
	 * should be a bit careful with the mutable state of the {@link Connection}
	 * instance obtained from <code>getEntityManager().getProvider().getConnection()</code>.
	 * This is because it is this <i>exact</i> instance which is used in all database
	 * operations for that transaction.  Thus it is technically possible to commit a 
	 * transaction prematurely, disable the transaction entirely, or otherwise really
	 * mess up the internals of the implementation.  You do <i>not</i> have to
	 * call <code>setAutoCommit(boolean)</code> on the {@link Connection}
	 * instance retrieved from the {@link DatabaseProvider}.  The connection is
	 * already initialized and within an open transaction by the time it gets to your
	 * custom code within the transaction.</p>
	 * 
	 * @return	The value (if any) returned from the transaction <code>run()</code>
	 * @throws SQLException	If the transaction failed for any reason and was rolled back.
	 * @see #run()
	 */
	public T execute() throws SQLException {
		Connection conn = null;
		SQLException toThrow = null;
		T back = null;
		
		try {
			conn = manager.getProvider().getConnection();
			((DelegateConnection) conn).setCloseable(false);
			
			conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
			conn.setAutoCommit(false);
			
			back = Transaction.this.run();
			
			conn.commit();
		} catch (SQLException e) {
			if (conn != null) {
				try {
					conn.rollback();
				} catch (SQLException e1) {
				}
			}
			
			toThrow = e;
		} finally {
			if (conn == null) {
				return null;
			}
			
			try {
				conn.setAutoCommit(true);
				((DelegateConnection) conn).setCloseable(true);
				
				conn.close();
			} catch (SQLException e) {
			}
		}
		
		if (toThrow != null) {
			throw toThrow;
		}
		
		return back;
	}
	
	/**
	 * <p>Called internally by {@link #execute()} to actually perform the actions
	 * within the transaction.  Any <code>SQLException(s)</code> should be
	 * allowed to propogate back up to the calling method, which will ensure
	 * that the transaction is rolled back and the proper resources disposed.  If
	 * the transaction generates a value which must be passed back to the calling
	 * method, this value may be returned as long as it is of the parameterized
	 * type.  If no value is generated, <code>null</code> is an acceptable return
	 * value.</p>
	 * 
	 * <p>Be aware that <i>any</i> operations performed within a transaction
	 * (even if indirectly invoked by the <code>run()</code> method) will use
	 * the <i>exact same</i> {@link Connection} instance.  This is to ensure
	 * integrity of the transaction's operations while at the same time allowing
	 * custom JDBC code and queries within the transaction.</p>
	 * 
	 * @return	Any value which must be passed back to the calling point (outside
	 * 		the transaction), or <code>null</code>.
	 * @throws SQLException	If something has gone wrong within the transaction and
	 * 		it requires a roll-back.
	 */
	protected abstract T run() throws SQLException;
}
