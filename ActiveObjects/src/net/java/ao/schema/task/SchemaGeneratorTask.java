/*
 * Copyright 2007 Daniel Spiewak
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 *	    http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.ao.schema.task;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.java.ao.schema.Generator;

import org.apache.tools.ant.Task;

/**
 * @author Daniel Spiewak
 */
public class SchemaGeneratorTask extends Task {
	private String classpath;
	private String uri;
	private String nameConverter;
	
	private String dest;
	
	private List<String> entities = new ArrayList<String>();
	
	// TODO	migrate task should actually execute the migration
	public void execute() {
		System.out.println("Generating SQL schema from entities...");
		
		FileWriter writer = null;
		try {
			writer = new FileWriter(dest);
			writer.append(Generator.generate(classpath, uri, nameConverter, entities.toArray(new String[entities.size()])));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}
	
	public void setClasspath(String classpath) {
		this.classpath = classpath;
	}
	
	public void setURI(String uri) {
		this.uri = uri;
	}
	
	public void setNameConverter(String nameConverter) {
		this.nameConverter = nameConverter;
	}
	
	public void setDest(String dest) {
		this.dest = dest;
	}
	
	public void add(Entity entity) {
		entities.add(entity.getText());
	}
}
