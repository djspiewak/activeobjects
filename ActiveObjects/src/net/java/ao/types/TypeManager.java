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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Daniel Spiewak
 */
public class TypeManager {
	private static TypeManager instance;
	
	private final List<DatabaseType<?>> types;
	
	// TODO	make thread safe
	private final Map<Class<?>, DatabaseType<?>> classIndex;
	private final ReadWriteLock classIndexLock;
	
	private final Map<Integer, DatabaseType<?>> intIndex;
	private final ReadWriteLock intIndexLock;
	
	private TypeManager() {
		types = new ArrayList<DatabaseType<?>>();
		classIndex = new HashMap<Class<?>, DatabaseType<?>>();
		intIndex = new HashMap<Integer, DatabaseType<?>>();
		
		classIndexLock = new ReentrantReadWriteLock();
		intIndexLock = new ReentrantReadWriteLock();
		
		// init built-in types
		types.add(new BigIntType());
		types.add(new BooleanType());
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
		types.add(new RealType());
		types.add(new URLType());
		
		types.add(new EntityType());
	}
	
	public void addType(DatabaseType<?> type) {
		types.add(type);
	}
	
	public <T> DatabaseType<T> getType(Class<T> javaType) {
		DatabaseType<T> back = null;
		
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
	
	public static TypeManager getInstance() {
		if (instance == null) {
			instance = new TypeManager();
		}
		
		return instance;
	}
}
