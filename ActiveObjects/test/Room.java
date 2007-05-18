import net.java.ao.Entity;
import net.java.ao.ManyToMany;
import net.java.ao.OneToMany;

/*
 * Created on May 3, 2007
 */

/**
 * @author Daniel Spiewak
 */
public interface Room extends Entity {
	public String getName();
	public void setName(String name);
	
	public Room getParent();
	public void setParent(Room parent);
	
	@OneToMany("room")
	public Test[] getTests();
	
	@ManyToMany
	public Test[] getManyTests();
	
	@ManyToMany
	public void setManyTests(Test[] tests);
}
