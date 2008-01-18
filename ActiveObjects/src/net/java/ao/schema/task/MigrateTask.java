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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;

/**
 * @author Daniel Spiewak
 */
public class MigrateTask extends Task {
	private String[] classpath;
	private String uri, username, password;
	private String nameConverter;
	
	private List<EntityType> entities = new ArrayList<EntityType>();
	
	@Override
	public void execute() {
		try {
			System.out.println("Migrating schema to match entity definition...");
			
			URL[] urls = new URL[classpath.length];
			
			for (int i = 0; i < classpath.length; i++) {
				String cp = classpath[i];
				cp = cp.replace('\\', '/');
				
				if (Pattern.compile("^[A-Z]:").matcher(cp).find()) {
					cp = "/" + cp;
				}
				
				urls[i] = new URL("file://" + cp);
			}
			
			URLClassLoader classloader = new URLClassLoader(urls);
			
			Class<?> emClass = Class.forName("net.java.ao.EntityManager", true, classloader);
			Object manager = emClass.getConstructor(String.class, String.class, String.class).newInstance(uri, username, password);
			
			emClass.getMethod("setNameConverter", Class.forName("net.java.ao.schema.PluggableNameConverter", true, classloader)).invoke(
					manager, loadNameConverter(classloader));
			
			emClass.getMethod("migrate", Class[].class).invoke(manager, (Object) loadClasses(classloader));
			
			Object provider = emClass.getMethod("getProvider").invoke(manager);
			provider.getClass().getMethod("dispose").invoke(provider);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	private Object loadNameConverter(ClassLoader classloader) {
		if (nameConverter != null && nameConverter.split(".").length == 0) {
			nameConverter = "net.java.ao.schema." + nameConverter;
		}
		
		Class<?> converterClass = null;
		try {
			converterClass = Class.forName(nameConverter, true, classloader);
		} catch (Throwable t) {
			t.printStackTrace();
		}
		
		if (converterClass == null) {
			throw new IllegalArgumentException("Unable to locate table name converter");
		}
		
		Object back = null;
		try {
			back = converterClass.newInstance();
		} catch (Throwable t) {
			System.err.println("Unable to load \"" + nameConverter + '\"');
			System.err.println("Using default name converter...");
			
			try {
				back = Class.forName("net.java.ao.schema.CamelCaseNameConverter", true, classloader).newInstance();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		return back;
	}
	
	private Class<?>[] loadClasses(ClassLoader classloader) throws MalformedURLException, 
			ClassNotFoundException {
		Class<?>[] back = new Class[entities.size()];
		
		for (int i = 0; i < back.length; i++) {
			back[i] = Class.forName(entities.get(i).getText(), true, classloader);
		}
		
		return back;
	}
	
	public void setClasspath(String path) throws IOException {
		String[] paths = path.split(File.pathSeparator);
		classpath = new String[paths.length];
		
		for (int i = 0; i < paths.length; i++) {
			File file = new File(paths[i]);
			
			classpath[i] = file.getCanonicalPath().replace('\\', '/');
			if (file.isDirectory()) {
				classpath[i] += '/';
			}
		}
	}
	
	public void setClasspathRef(String reference) {
		Path path = (Path) getProject().getReference(reference);
		
		classpath = path.list();
		for (int i = 0; i < classpath.length; i++) {
			if (new File(classpath[i]).isDirectory()) {
				classpath[i] += '/';
			}
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
	
	public void addEntity(EntityType entity) {
		entities.add(entity);
	}
}
