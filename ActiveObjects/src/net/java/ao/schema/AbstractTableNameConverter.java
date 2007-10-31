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

import net.java.ao.RawEntity;

// TODO	make this class thread-safe

/**
 * <p>An abstract implementation of {@link TableNameConverter} which provides basic
 * functionality common to most table name converters.  For most use-cases, it
 * is best to extend this class rather than directly implementing
 * <code>TableNameConverter</code>.</p>
 * 
 * <p>The primary functionality provided by this class is both class and pattern
 * mappings (between class names and generated table names).  These mappings
 * can be passed into arbitrary subclasses (such as {@link CamelCaseTableNameConverter})
 * allowing for maximum flexibility and control over auto-generated table names.</p>
 * 
 * <p>Most subclasses will only need to override the {@link #convertName(Class)} method
 * to accomplish most functionality.</p>
 * 
 * @author Daniel Spiewak
 */
public abstract class AbstractTableNameConverter implements TableNameConverter {
	private Map<Class<? extends RawEntity<?>>, String> classMappings;
	
	private List<String> patterns;
	private Map<String, String> patternMappings;
	
	/**
	 * Initializes the class mappings and pattern mappings to empty maps.
	 */
	protected AbstractTableNameConverter() {
		classMappings = new HashMap<Class<? extends RawEntity<?>>, String>();
		
		patterns = new LinkedList<String>();
		patternMappings = new HashMap<String, String>();
	}

	/**
	 * Adds an explicit mapping between a specific entity type and a
	 * corresponding table name.  This is basically a centralized version
	 * of the {@link Table} annotation.  This will override any name
	 * conversion <i>and</i> any pattern mappings available.  Additionally,
	 * it will override any <code>@Table</code> annotation placed upon the
	 * entity interface directly.  This is effectively the highest scoped
	 * method for specifying table names for correspondant class types.
	 * 
	 * @param clazz	The entity type for which the mapping will be created.
	 * @param name	The table name which will be used for the given entity type.
	 */
	public void addClassMapping(Class<? extends RawEntity<?>> clazz, String name) {
		classMappings.put(clazz, name);
	}

	/**
	 * Adds multiple class mappings in a single method call.  This allows the
	 * things such as loading class mappings from an external file and passing
	 * them to the name converter in bulk.
	 * 
	 * @param mappings	The map of all mappings which are to be added to the
	 * 		name converter.  Duplicate types will be overridden by the new
	 * 		mapping.
	 * @see #addClassMapping(Class, String)
	 */
	public void addClassMappings(Map<Class<? extends RawEntity<?>>, String> mappings) {
		classMappings.putAll(mappings);
	}

	/**
	 * <p>Adds a regular expression pattern and corresponding result mapping to
	 * the name converter.  These patterns will be applied after the
	 * generic {@link #convertName(Class)} value is retrieved.  The pattern
	 * will <i>not</i> be applied to explicitly specified class names using
	 * <code>@Table</code> or a class mapping.</p>
	 * 
	 * <p>The result value is a parsed string which can contain references to
	 * matched groups within the regular expression result.  Example:</p>
	 * 
	 * <table border="1">
	 * 		<tr>
	 * 			<td><b>Param: pattern</b></td>
	 * 			<td><b>Param: result</b></td>
	 * 		</tr>
	 * 
	 * 		<tr>
	 * 			<td><code>(.*p)erson</code></td>
	 * 			<td><code>{1}eople</code></td>
	 * 		</tr>
	 * </table>
	 * 
	 * <p>This mapping would pluralize any name ending in "person" (match
	 * case-insensetive) and replace the name with the pluralized form.
	 * Thus, "person" would become "people", "gafferPerson" would become 
	 * "gafferPeople" and so on.  As you may have guessed from the example,
	 * this sort of pattern matching is used to implement pluralization in
	 * table name conversion (using {@link PluralizedNameConverter}.</p>
	 * 
	 * @param pattern	A regular expression in Java format defining the pattern
	 * 		which should be matched.  (matching is case-insensetive)
	 * @param result	A parsed String defining the value which should be
	 * 		substituted for all matched names.
	 */
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

	public String getName(Class<? extends RawEntity<?>> entity) {
		Table tableAnnotation = entity.getAnnotation(Table.class);
		if (tableAnnotation != null) {
			return tableAnnotation.value();
		}
		
		if (classMappings.containsKey(entity)) {
			return classMappings.get(entity);
		}
		
		return postProcessName(processName(convertName(entity)));
	}
	
	protected abstract String convertName(Class<? extends RawEntity<?>> entity);
	
	protected String processName(String back) {
		for (String regexp : patterns) {
			Pattern pattern = Pattern.compile("^" + regexp + "$", Pattern.CASE_INSENSITIVE);
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
	
	protected String postProcessName(String back) {
		return back;
	}
}
