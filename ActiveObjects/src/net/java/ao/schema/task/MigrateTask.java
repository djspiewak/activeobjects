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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.java.ao.EntityManager;
import net.java.ao.schema.CamelCaseNameConverter;
import net.java.ao.schema.PluggableNameConverter;

import org.apache.tools.ant.Task;

/**
 * @author Daniel Spiewak
 */
public class MigrateTask extends Task {
	private String classpath;
	private String uri, username, password;
	private String nameConverter;
	
	private List<String> entities = new ArrayList<String>();
	
	public void execute() {
		System.out.println("Migrating schema to match entity definition...");
		
		EntityManager manager = new EntityManager(uri, username, password);
		manager.setNameConverter(loadNameConverter());
		
		Logger.getLogger("net.java.ao").setLevel(Level.FINE);
		
		try {
			manager.migrate(loadClasses());
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		manager.getProvider().dispose();
	}
	
	private PluggableNameConverter loadNameConverter() {
		PluggableNameConverter back = new CamelCaseNameConverter();
		
		if (nameConverter != null) {
			try {
				back = (PluggableNameConverter) Class.forName(nameConverter).newInstance();
			} catch (InstantiationException e) {
			} catch (IllegalAccessException e) {
			} catch (ClassNotFoundException e) {
				try {
					back = (PluggableNameConverter) Class.forName("net.java.ao.schema." + nameConverter).newInstance();
				} catch (InstantiationException e1) {
				} catch (IllegalAccessException e1) {
				} catch (ClassNotFoundException e1) {
				}
			}
		}
		
		return back;
	}
	
	private Class<? extends net.java.ao.Entity>[] loadClasses() throws MalformedURLException, ClassNotFoundException {
		Class<? extends net.java.ao.Entity>[] back = new Class[entities.size()];
		URLClassLoader classloader = new URLClassLoader(new URL[] {new URL("file://" + classpath)});
		
		for (int i = 0; i < back.length; i++) {
			back[i] = (Class<? extends net.java.ao.Entity>) Class.forName(entities.get(i), true, classloader);
		}
		
		return back;
	}
	
	public void setClasspath(String classpath) throws IOException {
		this.classpath = new File(classpath).getCanonicalPath();
		
		if (!this.classpath.startsWith("/")) {
			this.classpath = '/' + this.classpath;
		}
	}
	
	public void setURI(String uri) {
		this.uri = uri;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public void setNameConverter(String nameConverter) {
		this.nameConverter = nameConverter;
	}
	
	public void add(Entity entity) {
		entities.add(entity.getText());
	}
}
