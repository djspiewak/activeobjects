import java.sql.SQLException;

import net.java.ao.EntityManager;
import net.java.ao.benchmarks.schema.Person;
import net.java.ao.benchmarks.schema.Profession;
import net.java.ao.benchmarks.schema.Professional;
import net.java.ao.schema.PluralizedNameConverter;

/*
 * Created on Aug 13, 2007
 */

/**
 * @author Daniel Spiewak
 */
@SuppressWarnings("unused")
public class ActiveObjectsDriver {
	private long time = 0;
	
	private EntityManager manager;
	
	public ActiveObjectsDriver(EntityManager manager) throws SQLException {
		this.manager = manager;
		
		Person[] people = testQueries();
		testRetrieval(people);
		testPersisting(people);
		testRelations(people);
	}
	
	public void testPersisting(Person[] people) {
		String[] newNames = {"Daniel", "Chris", "Joseph", "Renee", "Bethany", "Grace", "Karen", "Larry", "Moya"};
		String[] lastNames = {"Smith", "Donovich", "Quieones", "Felger", "Gere", "Covis", "Dawes"};
		
		startTimer();
		int firstIter = 0;
		int lastIter = 0;
		
		for (Person person : people) {
			person.setFirstName(newNames[firstIter++]);
			person.setLastName(lastNames[lastIter++]);
			person.save();
			
			if (firstIter == newNames.length) {
				firstIter = 0;
			}
			if (lastIter == lastNames.length) {
				lastIter = 0;
			}
		}
		
		int iter = 15;
		for (Person person : people) {
			person.setAge(iter++);
			person.setAlive(iter % 2 == 0);		// only people with even ages are still living
	
			person.setBio("This is the story of two mice.  Well, actually it's the story of more than " +
					"two mice, but we only have time for the shortened version.  They (the mice) " +
					"were on this road one day, looking for upturned clods of grass - for you see, " +
					"this is what mice do - and they came across a peddler, peddling his wares. " +
					"After the usual confusion between 'ware', 'where' and 'were' (leading to some " +
					"dicy moments involving a silver bullet and frantic references to the impending " +
					"lunar cycle, the mice managed to extract a piece of useful information out of the " +
					"peddler's ramblings.  However, the remainder of this story, and the usefulness " +
					"of the peddlers account will have to wait for the SQL, which I am afraid is " +
					"going to be very late in arrival.");
			person.save();
		}
		
		System.out.println("Persistence test: " + stopTimer() + " ms");
	}
	
	public void testRetrieval(Person[] people) {
		startTimer();
		
		for (Person person : people) {
			String fname = person.getFirstName();
			String lname = person.getLastName();
			int age = person.getAge();
			boolean alive = person.isAlive();
			String bio = person.getBio();
		}

		System.out.println("Retrieval test: " + stopTimer() + " ms");		
	}
	
	public void testRelations(Person[] people) {
		startTimer();
		
		for (Person person : people) {
			for (Profession profession : person.getProfessions()) {
				String proName = profession.getName();
			}
			
			for (Person subPerson : person.getWorkplace().getPeople()) {
				String subFname = subPerson.getFirstName();
				String subLname = subPerson.getLastName();
			}
		}
		
		System.out.println("Relations test: " + stopTimer() + " ms");	
	}
	
	public Person[] testQueries() throws SQLException {
		startTimer();
		
		Person[] children = manager.find(Person.class, "age < 18");
		Person[] adults = manager.find(Person.class, "age >= 18");
		Profession[] professions = manager.find(Profession.class);
		Professional[] professionals = manager.find(Professional.class);
		Person[] people = manager.find(Person.class);

		System.out.println("Queries test: " + stopTimer() + " ms");	
		
		return people;
	}
	
	private void startTimer() {
		time = System.currentTimeMillis();
	}
	
	private long stopTimer() {
		return System.currentTimeMillis() - time;
	}
	
	public static void main(String... args) throws SQLException {
		EntityManager manager = new EntityManager("jdbc:mysql://localhost/ao_test", "root", "mysqlroot");
		/*Logger.getLogger("net.java.ao").setLevel(Level.FINE);*/
		
		manager.setNameConverter(new PluralizedNameConverter());
		manager.migrate(Professional.class);
		
		new ActiveObjectsDriver(manager);
		manager.getProvider().dispose();
	}
}
