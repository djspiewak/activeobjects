/*
 * Created on Aug 13, 2007
 */
package net.java.ao.benchmarks.schema;

import net.java.ao.Entity;
import net.java.ao.Preload;

/**
 * @author Daniel Spiewak
 */
@Preload("name")
public interface Profession extends Entity {
	
	public String getName();
	public void setName(String name);
}
