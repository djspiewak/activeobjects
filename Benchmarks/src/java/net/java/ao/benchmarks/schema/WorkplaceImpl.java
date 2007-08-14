/*
 * Created on Aug 13, 2007
 */
package net.java.ao.benchmarks.schema;

/**
 * @author Daniel Spiewak
 */
public class WorkplaceImpl {
	private Workplace workplace;
	
	public WorkplaceImpl(Workplace workplace) {
		this.workplace = workplace;
	}
	
	public String getOfficeName() {
		return "Office: " + workplace.getOfficeName();
	}
}
