import java.util.Calendar;

import net.java.ao.Accessor;
import net.java.ao.Entity;
import net.java.ao.Mutator;
import net.java.ao.ManyToMany;
import net.java.ao.schema.SQLType;

/*
 * Created on May 2, 2007
 */

/**
 * @author Daniel Spiewak
 */
public interface Test extends Entity {
	public String getName();
	public void setName(String name);

	@SQLType(java.sql.Types.CLOB)
	public String getText();

	@SQLType(java.sql.Types.CLOB)
	public void setText(String text);
	
	public int getAge();
	public void setAge(int age);
	
	public Calendar getDate();
	public void setDate(Calendar date);
	
	@Accessor("room")
	public Room getRoom();
	
	@Mutator("room")
	public void setRoom(Room room);
	
	@ManyToMany(RoomToTest.class)
	public Room[] getRooms();
}
