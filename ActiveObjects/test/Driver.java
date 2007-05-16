import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Calendar;

import net.java.ao.DatabaseProvider;
import net.java.ao.EntityManager;

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
		
		long millis = System.currentTimeMillis();
		EntityManager manager = EntityManager.getInstance(DatabaseProvider.getInstance(args[0], args[1], args[2]));
		
		runTestTest(manager);
		runRoomsTest(manager);
		
		System.out.println("Total time: " + (System.currentTimeMillis() - millis));
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
}
