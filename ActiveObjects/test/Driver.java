import java.sql.SQLException;
import java.text.DateFormat;

import net.java.ao.DatabaseProvider;
import net.java.ao.EntityManager;
import net.java.ao.Transaction;
import net.java.ao.db.DBCPPoolProvider;
import net.java.ao.db.IPoolProvider;

/*
 * Created on May 2, 2007
 */

/**
 * @author Daniel Spiewak
 */
public class Driver {
	public static void main(String... args) throws SQLException {
		if (args.length < 3) {
			System.err.println("You must specify a URI, username and password (in that order)");
			System.exit(-1);
		}
		
		IPoolProvider provider = new DBCPPoolProvider(DatabaseProvider.getInstance(args[0], args[1], args[2]));

		long millis = System.currentTimeMillis();
		EntityManager manager = EntityManager.getInstance(provider);
		
		runTestTest(manager);
		runRoomsTest(manager);
		runManyTest(manager);
		runTransactionTest(manager);
		
		System.out.println("Total time: " + (System.currentTimeMillis() - millis));
		
		provider.close();
	}
	
	private static void runTestTest(EntityManager manager) throws SQLException {
		DateFormat format = DateFormat.getDateInstance(DateFormat.MEDIUM);
		for (Test test : manager.getAllEntities(Test.class)) {
			System.out.print("id = " + test.getID() + ", ");
			System.out.print("name = " + test.getName() + ", ");
			System.out.print("age = " + test.getAge() + ", ");
			System.out.print("date = " + format.format(test.getDate().getTime()) + ", ");
			System.out.print("room = " + test.getRoom().getName() + ", ");
			
			String parentName = "null";
			Room parent = test.getRoom().getParent();
			if (parent != null) {
				parentName = parent.getName();
			}
			
			System.out.println("roomParent = " + parentName);
		}
		System.out.println();
	}
	
	private static void runRoomsTest(EntityManager manager) throws SQLException {
		for (Room room : manager.getAllEntities(Room.class)) {
			System.out.print(room.getName() + " = {");
			
			for (Test test : room.getTests()) {
				System.out.print(test.getName() + ",");
			}
			System.out.println("}");
		}
		System.out.println();
	}
	
	private static void runManyTest(EntityManager manager) throws SQLException {
		Room room = manager.getEntity(1, Room.class);
		Test[] tests = manager.getAllEntities(Test.class);
		
		room.setManyTests(tests);
		tests[0].setRooms(manager.getAllEntities(Room.class));
		
		for (Test test : tests) {
			for (Room relatedRoom : test.getRooms()) {
				System.out.print("id = " + relatedRoom.getID());
				System.out.print(", name = " + relatedRoom.getName());
				if (relatedRoom.getParent() != null) {
					System.out.println(", parent = " + relatedRoom.getParent().getName());
				} else {
					System.out.println();
				}
			}
		}
		System.out.println();
	}
	
	private static void runTransactionTest(final EntityManager manager) {
		Thread threadA = new Transaction(manager) {
			public void run() {
				Room room = manager.getEntity(1, Room.class);
				
				String name = room.getName();
				room.setName(name + " (test1)");
				
				name = room.getName();
				room.setName(name.substring(1));
			}
		}.executeConcurrently();

		Thread threadB = new Transaction(manager) {
			public void run() {
				Room room = manager.getEntity(1, Room.class);
				
				String name = room.getName();
				room.setName(name + " (test2)");
				
				name = room.getName();
				room.setName(name.substring(2));
			}
		}.executeConcurrently();

		Thread threadC = new Transaction(manager) {
			public void run() {
				Room room = manager.getEntity(1, Room.class);
				
				String name = room.getName();
				room.setName(name + " (test3)");
				
				name = room.getName();
				room.setName(name.substring(3));
			}
		}.executeConcurrently();
		
		try {
			threadA.join();
			threadB.join();
			threadC.join();
		} catch (InterruptedException e) {
		}
	}
}
