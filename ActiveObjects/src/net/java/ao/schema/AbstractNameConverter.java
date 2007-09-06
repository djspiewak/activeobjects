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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.ao.Entity;

/**
 * @author Daniel Spiewak
 */
public abstract class AbstractNameConverter implements PluggableTableNameConverter {
	private Map<Class<? extends Entity>, String> classMappings;
	
	private List<String> patterns;
	private Map<String, String> patternMappings;
	
	protected AbstractNameConverter() {
		classMappings = new HashMap<Class<? extends Entity>, String>();
		
		patterns = new LinkedList<String>();
		patternMappings = new HashMap<String, String>();
	}

	public void addClassMapping(Class<? extends Entity> clazz, String name) {
		classMappings.put(clazz, name);
	}

	public void addClassMappings(Map<Class<? extends Entity>, String> mappings) {
		classMappings.putAll(mappings);
	}

	public void addPatternMapping(String pattern, String result) {
		patterns.add(0, pattern);
		patternMappings.put(pattern, result);
	}

	public void addPatternMappings(Map<String, String> mappings, Iterator<String> keys) {
		int i = 0;
		while (keys.hasNext()) {
			patterns.add(i++, keys.next());
		}
		
		patternMappings.putAll(mappings);
	}

	public String getName(Class<? extends Entity> entity) {
		Table tableAnnotation = entity.getAnnotation(Table.class);
		if (tableAnnotation != null) {
			return tableAnnotation.value();
		}
		
		if (classMappings.containsKey(entity)) {
			return classMappings.get(entity);
		}
		
		String back = getNameImpl(entity);
		
		for (String regexp : patterns) {
			Pattern pattern = Pattern.compile("^" + regexp + "$");
			Matcher matcher = pattern.matcher(back);
			
			if (matcher.find()) {
				String mapResult = patternMappings.get(regexp);
				
				Pattern mapPattern = Pattern.compile("\\{\\d+\\}");
				Matcher mapMatcher = mapPattern.matcher(mapResult);
				
				while (mapMatcher.find()) {
					Matcher groupMatcher = Pattern.compile("\\{(\\d+)\\}").matcher(mapResult);
					groupMatcher.find();
					
					String toReplace = matcher.group(Integer.parseInt(groupMatcher.group(1)));
					
					mapResult = mapMatcher.replaceFirst(toReplace);
					mapMatcher = mapPattern.matcher(mapResult);
				}
				
				back = mapResult;
				
				break;
			}
		}
		
		return back;
	}
	
	public abstract String getNameImpl(Class<? extends Entity> entity);
}
