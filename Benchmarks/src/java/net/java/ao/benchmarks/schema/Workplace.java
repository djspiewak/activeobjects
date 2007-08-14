/*
 * Created on Aug 13, 2007
 */
package net.java.ao.benchmarks.schema;

import net.java.ao.Entity;
import net.java.ao.Implementation;
import net.java.ao.OneToMany;

/**
 * @author Daniel Spiewak
 */
@Implementation(WorkplaceImpl.class)
public interface Workplace extends Entity {

	public String getOfficeName();
	public void setOfficeName(String name);
	
	public short getCoffeeQuality();
	public void setCoffeeQuality(short quality);
	
	@OneToMany
	public Person[] getPeople();
}
