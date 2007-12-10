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
 * <p>You should not attempt to use the same instance of this class with more than
 * one instance of {@link EntityManager}.  This is because <code>EntityManager</code>
 * performs some initialization on the mapper when it is set.  While this initialization
 * is in fact thread-safe, it may cause undesired behavior if using two separate
 * entity managers with different table name converters.</p>
 * 
 * @author Daniel Spiewak
 */
public class DefaultPolymorphicTypeMapper implements PolymorphicTypeMapper {
	private final Map<Class<? extends RawEntity<?>>, String> mappings;
	private final Map<String, Set<Class<? extends RawEntity<?>>>> reverse;
	
	private Class<? extends RawEntity<?>>[] types;
	
	/**
	 * <p>Convenience constructor which will construct a set of mappings on the fly based
	 * on the specified entity types.  Actual construction of the mappings is delayed
	 * and invoked directly by the {@link EntityManager} instance which receives the
	 * instance of this class.  This is to allow <code>EntityManager</code> to pass the
	 * relevant table name converter to be used in the initialization process.</p>
	 * 
	 * <p>It is very important that an instance created using this constructor is not
	 * used with more than one <code>EntityManager</code>.</p>
	 * 
	 * @param types	The polymorphic types which must be mapped.
	 */
	public DefaultPolymorphicTypeMapper(Class<? extends RawEntity<?>>... types) {
		this(new HashMap<Class<? extends RawEntity<?>>, String>());
		
		this.types = types;
	}
	
	/**
	 * Creates a new instance with the specified {type =&gt; flag} mappings.
	 * 
	 * @param mappings	The mappings from entity type to polymorphic flag value.
	 */
	public DefaultPolymorphicTypeMapper(Map<Class<? extends RawEntity<?>>, String> mappings) {
		this.mappings = mappings;
		reverse = new HashMap<String, Set<Class<? extends RawEntity<?>>>>();
		
		createReverseMappings();
	}
	
	private void createReverseMappings() {
		reverse.clear();
		
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
	
	void resolveMappings(TableNameConverter converter) {
		if (types == null) {
			return;
		}
		
		while (converter instanceof PluralizedNameConverter) {
			converter = ((PluralizedNameConverter) converter).getDelegate();
		}
		
		for (Class<? extends RawEntity<?>> type : types) {
			mappings.put(type, converter.getName(type));
		}
		
		types = null;
		
		createReverseMappings();
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
