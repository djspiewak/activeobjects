/*
 * Created on Aug 13, 2007
 */
package net.java.ao.benchmarks.schema;

import net.java.ao.Entity;

/**
 * @author Daniel Spiewak
 */
public interface Professional extends Entity {
	
	public Person getPerson();
	public void setPerson(Person person);
	
	public Profession getProfession();
	public void setProfession(Profession profession);
}
