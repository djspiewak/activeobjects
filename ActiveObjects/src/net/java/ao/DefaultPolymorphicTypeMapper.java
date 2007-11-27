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
package net.java.ao;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.java.ao.schema.PluralizedNameConverter;
import net.java.ao.schema.TableNameConverter;

/**
 * <p>Boiler-plate implementation of {@link PolymorphicTypeMapper} which requires
 * all mappings to be manually specified.  This type mapper will not attempt to
 * do anything clever like caching or storing of mapping results.  Rather, it
 * depends entirely upon the developer to explicitly specify the map values for
 * every polymorphic type in use.</p>
 * 
 * <p>If a type which has <i>not</i> been explicitly specified, this converter
 * will default to the fully qualified classname.  Thus the inverse conversion
 * will also default to a <code>Class.forName(String)</code> invocation in case
 * of no valid mapping in the relevant class hierarchy.</p>
 * 
 * @author Daniel Spiewak
 */
public class DefaultPolymorphicTypeMapper implements PolymorphicTypeMapper {
	private Map<Class<? extends RawEntity<?>>, String> mappings;
	private Map<String, Set<Class<? extends RawEntity<?>>>> reverse;
	
	/**
	 * <p>Convenience constructor which will construct a set of mappings on the fly
	 * from the given set of entity types and the specified table name converter.
	 * If the name converter is an instance of {@link PluralizedNameConverter},
	 * the delegate converter will be used to maintain "coolness conventions".</p>
	 * 
	 * <p>This method does nothing but construct the appropriate mappings and
	 * delegate to the {@link #DefaultPolymorphicTypeMapper(Map)} constructor.</p>
	 * 
	 * @param converter	The table name converter to be used to auto-generate the mappings.
	 * @param types	The polymorphic types which must be mapped.
	 */
	public DefaultPolymorphicTypeMapper(TableNameConverter converter, Class<? extends RawEntity<?>>... types) {
		Map<Class<? extends RawEntity<?>>, String> mappings = new HashMap<Class<? extends RawEntity<?>>, String>();
		
		while (converter instanceof PluralizedNameConverter) {
			converter = ((PluralizedNameConverter) converter).getDelegate();
		}
		
		for (Class<? extends RawEntity<?>> type : types) {
			mappings.put(type, converter.getName(type));
		}
		
		init(mappings);
	}
	
	/**
	 * Creates a new instance with the specified type =&gt; flag mappings.
	 * 
	 * @param mappings	The mappings from entity type to polymorphic flag value.
	 */
	public DefaultPolymorphicTypeMapper(Map<Class<? extends RawEntity<?>>, String> mappings) {
		init(mappings);
	}
	
	private void init(Map<Class<? extends RawEntity<?>>, String> mappings) {
		this.mappings = Collections.unmodifiableMap(mappings);	// ensures external code doesn't have access to state
		
		reverse = new HashMap<String, Set<Class<? extends RawEntity<?>>>>();
		
		for (Class<? extends RawEntity<?>> type : mappings.keySet()) {
			String value = mappings.get(type);
			
			Set<Class<? extends RawEntity<?>>> set = reverse.get(value);
			if (set == null) {
				set = new HashSet<Class<? extends RawEntity<?>>>();
				reverse.put(value, set);
			}
			
			set.add(type);
		}
	}

	public String convert(Class<? extends RawEntity<?>> type) {
		String back = mappings.get(type);
		if (back == null) {
			return type.getCanonicalName();		// sane default
		}
		
		return back;
	}

	public Class<? extends RawEntity<?>> invert(Class<? extends RawEntity<?>> parent, String type) {
		Set<Class<? extends RawEntity<?>>> set = reverse.get(type);
		if (set != null && set.size() != 0) {
			for (Class<? extends RawEntity<?>> clazz : set) {
				if (Common.interfaceInheritsFrom(clazz, parent)) {
					return clazz;
				}
			}
		}
		
		try {
			return (Class<? extends RawEntity<?>>) Class.forName(type);		// classname fallback
		} catch (Throwable t) {
		}
		
		throw new IllegalArgumentException("No valid inverse mapping for type value \"" + type + '"');
	}
}
