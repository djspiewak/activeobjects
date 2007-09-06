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
package net.java.ao.schema;

import java.util.Iterator;
import java.util.Map;

import net.java.ao.Entity;

/**
 * @author Daniel Spiewak
 */
public interface TableNameConverter {
	public String getName(Class<? extends Entity> clazz);
	
	/**
	 * pattern example: "(.+)y"
	 * result example: "{1}ies"
	 * 
	 * Would map "company" to "companies"
	 * 
	 * Pattern mappings are applied after Class to String
	 * mapping and is bypassed by any explicit class mappings.
	 */
	public void addPatternMapping(String pattern, String result);
	
	public void addPatternMappings(Map<String, String> mappings, Iterator<String> keys);
	
	public void addClassMapping(Class<? extends Entity> clazz, String name);
	public void addClassMappings(Map<Class<? extends Entity>, String> mappings);
}
