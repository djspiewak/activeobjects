/*
 * Created on Aug 13, 2007
 */
package net.java.ao.benchmarks.schema;

import java.sql.Types;

import net.java.ao.Entity;
import net.java.ao.ManyToMany;
import net.java.ao.Preload;
import net.java.ao.schema.SQLType;

/**
 * @author Daniel Spiewak
 */
@Preload({"firstName", "lastName"})
public interface Person extends Entity {
	
	public String getFirstName();
	public void setFirstName(String firstName);
	
	public String getLastName();
	public void setLastName(String lastName);
	
	public int getAge();
	public void setAge(int age);
	
	public boolean isAlive();
	public void setAlive(boolean alive);
	
	@SQLType(Types.CLOB)
	public String getBio();
	@SQLType(Types.CLOB)
	public void setBio(String bio);
	
	public Workplace getWorkplace();
	public void setWorkplace(Workplace workplace);
	
	@ManyToMany(Professional.class)
	public Profession[] getProfessions();
}
