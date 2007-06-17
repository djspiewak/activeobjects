/*
 * Copyright 2007, Daniel Spiewak
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided
 *     with the distribution.
 *   * Neither the name of the ActiveObjects project nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
public abstract class AbstractNameConverter implements PluggableNameConverter {
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
		patterns.add(pattern);
		patternMappings.put(pattern, result);
	}

	public void addPatternMappings(Map<String, String> mappings, Iterator<String> keys) {
		while (keys.hasNext()) {
			patterns.add(keys.next());
		}
		
		patternMappings.putAll(mappings);
	}

	public String getName(Class<? extends Entity> entity) {
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
