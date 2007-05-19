import net.java.ao.Entity;

/*
 * Created on May 18, 2007
 */

/**
 * @author Daniel Spiewak
 */
public interface Account extends Entity {
	public int getBalance();
	public void setBalance(int balance);
}
