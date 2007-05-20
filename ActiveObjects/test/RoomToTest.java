import net.java.ao.Entity;

/*
 * Created on May 20, 2007
 */

/**
 * @author Daniel Spiewak
 */
public interface RoomToTest extends Entity {
	public Room getRoom();
	public void setRoom(Room room);
	
	public Test getTest();
	public void setTest(Test test);
}
