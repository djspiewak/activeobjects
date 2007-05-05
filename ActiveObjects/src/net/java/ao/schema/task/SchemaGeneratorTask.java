/*
 * Created on May 5, 2007
 */
package net.java.ao.schema.task;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.java.ao.schema.Generator;

import org.apache.commons.cli.ParseException;
import org.apache.tools.ant.Task;

/**
 * @author Daniel Spiewak
 */
public class SchemaGeneratorTask extends Task {
	private String classpath;
	private String dest;
	
	private List<String> entities = new ArrayList<String>();
	
	public void execute() {
		System.out.println("Generating SQL schema from entities...");
		
		List<String> copy = new ArrayList<String>();
		
		copy.add("--classpath");
		copy.add(classpath);
		
		copy.addAll(entities);
		
		try {
			FileWriter writer = new FileWriter(dest);
			writer.append(Generator.generate(copy.toArray(new String[copy.size()])));
			writer.close();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public void setClasspath(String classpath) {
		this.classpath = classpath;
	}
	
	public void setDest(String dest) {
		this.dest = dest;
	}
	
	public void add(Entity entity) {
		entities.add(entity.getText());
	}
}
