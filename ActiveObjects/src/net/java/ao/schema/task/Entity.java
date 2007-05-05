/*
 * Created on May 5, 2007
 */
package net.java.ao.schema.task;

/**
 * @author Daniel Spiewak
 */
public class Entity {
	private String text = "";
	
	public void addText(String text) {
		this.text += text;
	}

	public String getText() {
		return text;
	}
}
