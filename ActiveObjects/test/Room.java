import net.java.ao.Entity;
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
	
	@OneToMany
	public Test[] getTests();
}
