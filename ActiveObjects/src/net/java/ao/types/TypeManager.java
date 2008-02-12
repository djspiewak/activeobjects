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
package net.java.ao.types;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.java.ao.Common;
import net.java.ao.EntityManager;
import net.java.ao.RawEntity;

/**
 * <p>Central managing class for the ActiveObjects type system.  The type
 * system in AO is designed to allow extensibility and control over
 * how specific data types are handled internally.  All database-agnostic,
 * type-specific tasks are delegated to the actual type instances.  This
 * class acts as a singleton container for every available type, indexing
 * them based on corresponding Java type and JDBC integer type.</p>
 * 
 * <p>This container is thread safe and so may be used from within multiple
 * contexts.</p>
 * 
 * @author Daniel Spiewak
 * @see net.java.ao.types.DatabaseType
 */
public class TypeManager {
	private static TypeManager instance;
	
	private final List<DatabaseType<?>> types;
	
	private final Map<Class<?>, DatabaseType<?>> classIndex;
	private final ReadWriteLock classIndexLock;
	
	private final Map<Integer, DatabaseType<?>> intIndex;
	private final ReadWriteLock intIndexLock;
	
	private TypeManager() {
		types = Collections.synchronizedList(new ArrayList<DatabaseType<?>>());
		classIndex = new HashMap<Class<?>, DatabaseType<?>>();
		intIndex = new HashMap<Integer, DatabaseType<?>>();
		
		classIndexLock = new ReentrantReadWriteLock();
		intIndexLock = new ReentrantReadWriteLock();
		
		// init built-in types
		types.add(new BigIntType());
		types.add(new BooleanType());
		types.add(new BlobType());
		types.add(new CharType());
		types.add(new DoubleType());
		types.add(new FloatType());
		types.add(new IntegerType());
		types.add(new TimestampType());
		types.add(new TimestampDateType());
		types.add(new TinyIntType());
		types.add(new VarcharType());

		types.add(new DateType());
		types.add(new DateDateType());
		types.add(new EnumType());
		types.add(new RealType());
		types.add(new URLType());
		types.add(new URIType());
	}
	
	/**
	 * Adds a new type to the container.  Once the type is added, it
	 * will be availble to <i>every</i> {@link EntityManager} instance.
	 * This method is used internally to set up the default types
	 * (such as <code>int</code>, <code>String</code> and so on).  Any
	 * custom types should be added using this method.
	 * 
	 * @param type	The type instance to add to the container.
	 */
	public void addType(DatabaseType<?> type) {
		types.add(type);
	}
	
	/**
	 * <p>Returns the corresponding {@link DatabaseType} for a given Java
	 * class.  This is the primary mechanism used by ActiveObjects
	 * internally to obtain type instances.  Code external to the
	 * framework may also make use of this method to obtain the relevant
	 * type information or to just test if a type is in fact
	 * available.  Types are internally prioritized by entry order.  The
	 * first type to respond <code>true</code> to the {@link DatabaseType#isHandlerFor(Class)}
	 * method will be returned.</p>
	 * 
	 * <p>It's worth noting that this method worst case runs in <code>O(n)</code>
	 * time.  This is because a linear search must be made through the
	 * raw list of available types.  However, once the type has been found
	 * it is placed into a hash indexed by class type.  Thus for most types,
	 * this method will run in constant time (<code>O(1)</code>).</p>
	 * 
	 * @param javaType	The {@link Class} type for which a type instance
	 * 		should be returned.
	 * @return	The type instance which corresponds to the specified class.
	 * @throws	RuntimeException	If no type was found correspondant to the
	 * 		given class.
	 * @see #getType(int)
	 */
	public <T> DatabaseType<T> getType(Class<T> javaType) {
		DatabaseType<T> back = null;
		
		if (Common.typeInstanceOf(javaType, RawEntity.class)) {
			return (DatabaseType<T>) new EntityType<Object>((Class<? extends RawEntity<Object>>) javaType);
		}
		
		classIndexLock.writeLock().lock();
		try {
			if (classIndex.containsKey(javaType)) {
				return (DatabaseType<T>) classIndex.get(javaType);
			}
			
			for (DatabaseType<?> type : types) {
				if (type.isHandlerFor(javaType)) {
					back = (DatabaseType<T>) type;
					break;
				}
			}
			
			if (back != null) {
				classIndex.put(javaType, back);
			} else {
				throw new RuntimeException("Unrecognized type: " + javaType.getName());
			}
		} finally {
			classIndexLock.writeLock().unlock();
		}
		
		return back;
	}
	
	/**
	 * <p>Returns the corresponding {@link DatabaseType} for a given JDBC
	 * integer type.  Code external to the framework may also make use of 
	 * this method to obtain the relevant type information or to just test 
	 * if a type is in fact available.  Types are internally prioritized by 
	 * entry order.  The first type to respond <code>true</code> to the 
	 * {@link DatabaseType#isHandlerFor(int)} method will be returned.</p>
	 * 
	 * <p>It's worth noting that this method worst case runs in <code>O(n)</code>
	 * time.  This is because a linear search must be made through the
	 * raw list of available types.  However, once the type has been found
	 * it is placed into a hash indexed by int value.  Thus for most types,
	 * this method will run in constant time (<code>O(1)</code>).</p>
	 * 
	 * @param sqlType	The JDBC {@link Types} constant for which a type
	 * 		instance should be retrieved.
	 * @return	The type instance which corresponds to the specified type constant.
	 * @throws	RuntimeException	If no type was found correspondant to the
	 * 		given type constant.
	 * @see #getType(Class)
	 */
	public DatabaseType<?> getType(int sqlType) {
		DatabaseType<?> back = null;
		
		intIndexLock.writeLock().lock();
		try {
			if (intIndex.containsKey(sqlType)) {
				return intIndex.get(sqlType);
			}
			
			for (DatabaseType<?> type : types) {
				if (type.isHandlerFor(sqlType)) {
					back = type;
					break;
				}
			}
			
			if (back == null) {
				back = new GenericType(sqlType);
			}
			
			intIndex.put(sqlType, back);
		} finally {
			intIndexLock.writeLock().unlock();
		}
		
		return back;
	}
	
	/**
	 * Retrieves the singleton instance of the container.  This method is
	 * thread-safe and synchronized using pre-Java 5 mechanisms (meaning it
	 * may be a little slower than it could be).  For optimal efficiency, 
	 * do not make repeated calls.
	 * 
	 * @return	The global singleton instance.
	 */
	public static synchronized TypeManager getInstance() {
		if (instance == null) {
			instance = new TypeManager();
		}
		
		return instance;
	}
}
