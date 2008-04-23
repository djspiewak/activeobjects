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

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.java.ao.AnnotationDelegate;
import net.java.ao.Common;
import net.java.ao.DatabaseFunction;
import net.java.ao.DatabaseProvider;
import net.java.ao.ManyToMany;
import net.java.ao.OneToMany;
import net.java.ao.OneToOne;
import net.java.ao.Polymorphic;
import net.java.ao.RawEntity;
import net.java.ao.schema.ddl.DDLAction;
import net.java.ao.schema.ddl.DDLField;
import net.java.ao.schema.ddl.DDLForeignKey;
import net.java.ao.schema.ddl.DDLIndex;
import net.java.ao.schema.ddl.DDLTable;
import net.java.ao.schema.ddl.SchemaReader;
import net.java.ao.types.DatabaseType;
import net.java.ao.types.TypeManager;

/**
 * WARNING: <i>Not</i> part of the public API.  This class is public only
 * to allow its use within other packages in the ActiveObjects library.
 * 
 * @author Daniel Spiewak
 */
public final class SchemaGenerator {
	
	public static void migrate(DatabaseProvider provider, TableNameConverter nameConverter, FieldNameConverter fieldConverter,
			Class<? extends RawEntity<?>>... classes) throws SQLException {
		String[] statements = null;
		try {
			statements = generateImpl(provider, nameConverter, fieldConverter, SchemaGenerator.class.getClassLoader(), classes);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		Connection conn = provider.getConnection();
		try {
			Statement stmt = conn.createStatement();
			
			for (String statement : statements) {
				if (!statement.trim().equals("")) {
					Logger.getLogger("net.java.ao").log(Level.INFO, statement);
					stmt.executeUpdate(statement);
				}
			}
			
			stmt.close();
		} finally {
			conn.close();
		}
	}
	
	private static String[] generateImpl(DatabaseProvider provider, TableNameConverter nameConverter, FieldNameConverter fieldConverter,
			ClassLoader classloader, Class<? extends RawEntity<?>>... classes) throws ClassNotFoundException, SQLException {
		List<String> back = new ArrayList<String>();
		
		DDLTable[] parsedTables = parseDDL(provider, nameConverter, fieldConverter, classloader, classes);
		DDLTable[] readTables = SchemaReader.readSchema(provider);
		
		DDLAction[] actions = SchemaReader.sortTopologically(SchemaReader.diffSchema(parsedTables, readTables));
		for (DDLAction action : actions) {
			back.addAll(Arrays.asList(provider.renderAction(action)));
		}
		
		return back.toArray(new String[back.size()]);
	}
	
	static DDLTable[] parseDDL(DatabaseProvider provider, TableNameConverter nameConverter, FieldNameConverter fieldConverter,
			ClassLoader classloader, Class<? extends RawEntity<?>>... classes) {
		Map<Class<? extends RawEntity<?>>, Set<Class<? extends RawEntity<?>>>> deps = 
			new HashMap<Class<? extends RawEntity<?>>, Set<Class<? extends RawEntity<?>>>>();
		Set<Class<? extends RawEntity<?>>> roots = new LinkedHashSet<Class<? extends RawEntity<?>>>();
		
		for (Class<? extends RawEntity<?>> cls : classes) {
			try {
				parseDependencies(fieldConverter, deps, roots, cls);
			} catch (StackOverflowError e) {
				throw new RuntimeException("Circular dependency detected in or below " + cls.getCanonicalName());
			}
		}
		
		List<DDLTable> parsedTables = new ArrayList<DDLTable>();
		
		while (!roots.isEmpty()) {
			Class<? extends RawEntity<?>>[] rootsArray = roots.toArray(new Class[roots.size()]);
			roots.remove(rootsArray[0]);
			
			Class<? extends RawEntity<?>> clazz = rootsArray[0];
			if (clazz.getAnnotation(Polymorphic.class) == null) {
				parsedTables.add(parseInterface(provider, nameConverter, fieldConverter, clazz));
			}
			
			List<Class<? extends RawEntity<?>>> toRemove = new LinkedList<Class<? extends RawEntity<?>>>();
			Iterator<Class<? extends RawEntity<?>>> depIterator = deps.keySet().iterator();
			while (depIterator.hasNext()) {
				Class<? extends RawEntity<?>> depClass = depIterator.next();
				
				Set<Class<? extends RawEntity<?>>> individualDeps = deps.get(depClass);
				individualDeps.remove(clazz);
				
				if (individualDeps.isEmpty()) {
					roots.add(depClass);
					toRemove.add(depClass);
				}
			}
			
			for (Class<? extends RawEntity<?>> remove : toRemove) {
				deps.remove(remove);
			}
		}
		
		return parsedTables.toArray(new DDLTable[parsedTables.size()]);
	}
	
	private static void parseDependencies(FieldNameConverter fieldConverter, Map<Class <? extends RawEntity<?>>, 
			Set<Class<? extends RawEntity<?>>>> deps, Set<Class <? extends RawEntity<?>>> roots, Class<? extends RawEntity<?>>... classes) {
		for (Class<? extends RawEntity<?>> clazz : classes) {
			if (deps.containsKey(clazz)) {
				continue;
			}
			
			Set<Class<? extends RawEntity<?>>> individualDeps = new LinkedHashSet<Class<? extends RawEntity<?>>>();
			
			for (Method method : clazz.getMethods()) {
				String attributeName = fieldConverter.getName(method);
				Class<?> type = Common.getAttributeTypeFromMethod(method);
				
				if (attributeName != null && type != null && Common.interfaceInheritsFrom(type, RawEntity.class)) {
					if (!type.equals(clazz)) {
						individualDeps.add((Class<? extends RawEntity<?>>) type);
					
						parseDependencies(fieldConverter, deps, roots, (Class<? extends RawEntity<?>>) type);
					}
				}
			}
			
			if (individualDeps.size() == 0) {
				roots.add(clazz);
			} else {
				deps.put(clazz, individualDeps);
			}
		}
	}
	
	private static DDLTable parseInterface(DatabaseProvider provider, TableNameConverter nameConverter,
			FieldNameConverter fieldConverter, Class<? extends RawEntity<?>> clazz) {
		String sqlName = nameConverter.getName(clazz);
		
		DDLTable table = new DDLTable();
		table.setName(sqlName);
		
		table.setFields(parseFields(clazz, fieldConverter));
		table.setForeignKeys(parseForeignKeys(nameConverter, fieldConverter, clazz));
		table.setIndexes(parseIndexes(nameConverter, fieldConverter, clazz));
		
		return table;
	}
	
	private static DDLField[] parseFields(Class<? extends RawEntity<?>> clazz, FieldNameConverter fieldConverter) {
		List<DDLField> fields = new ArrayList<DDLField>();
		List<String> attributes = new LinkedList<String>();
		
		for (Method method : clazz.getMethods()) {
			AnnotationDelegate annotations = Common.getAnnotationDelegate(fieldConverter, method);
			
			if (annotations.getAnnotation(Ignore.class) != null
					|| annotations.getAnnotation(OneToOne.class) != null
					|| annotations.getAnnotation(OneToMany.class) != null
					|| annotations.getAnnotation(ManyToMany.class) != null) {
				continue;
			}
			
			String attributeName = fieldConverter.getName(method);
			Class<?> type = Common.getAttributeTypeFromMethod(method);
			
			if (attributeName != null && type != null) {
				if (attributes.contains(attributeName)) {
					continue;
				}
				attributes.add(attributeName);
				
				DatabaseType<?> sqlType = getSQLTypeFromMethod(type, annotations);
				int precision = getPrecisionFromMethod(type, method, fieldConverter);
				int scale = getScaleFromMethod(type, method, fieldConverter);
				
				DDLField field = new DDLField();
				
				field.setName(attributeName);
				field.setType(sqlType);
				field.setPrecision(precision);
				field.setScale(scale);
				
				if (annotations.getAnnotation(PrimaryKey.class) != null) {
					field.setPrimaryKey(true);
				}
				
				if (annotations.getAnnotation(NotNull.class) != null) {
					field.setNotNull(true);
				}
				
				if (annotations.getAnnotation(Unique.class) != null) {
					field.setUnique(true);
				}
				
				if (annotations.getAnnotation(AutoIncrement.class) != null) {
					field.setAutoIncrement(true);
				} else if (annotations.getAnnotation(Default.class) != null) {
					field.setDefaultValue(convertStringValue(annotations.getAnnotation(Default.class).value(), sqlType));
				}
				
				if (annotations.getAnnotation(OnUpdate.class) != null) {
					field.setOnUpdate(convertStringValue(annotations.getAnnotation(OnUpdate.class).value(), sqlType));
				}
				
				if (field.isPrimaryKey()) {
					fields.add(0, field);
				} else {
					fields.add(field);
				}
				
				if (Common.interfaceInheritsFrom(type, RawEntity.class)
						&& type.getAnnotation(Polymorphic.class) != null) {
					field.setDefaultValue(null);		// polymorphic fields can't have default
					field.setOnUpdate(null);		// or on update
					
					attributeName = fieldConverter.getPolyTypeName(method);
					
					field = new DDLField();
					
					field.setName(attributeName);
					field.setType(TypeManager.getInstance().getType(String.class));
					field.setPrecision(127);
					field.setScale(-1);
					
					if (annotations.getAnnotation(NotNull.class) != null) {
						field.setNotNull(true);
					}
					
					fields.add(field);
				}
			}
		}
		
		return fields.toArray(new DDLField[fields.size()]);
	}
	
	private static DatabaseType<?> getSQLTypeFromMethod(Class<?> type, AnnotationDelegate annotations) {
		DatabaseType<?> sqlType = null;
		TypeManager manager = TypeManager.getInstance();
		
		sqlType = manager.getType(type);
		
		SQLType sqlTypeAnnotation = annotations.getAnnotation(SQLType.class);
		if (sqlTypeAnnotation != null) {
			if (sqlTypeAnnotation.value() > 0) {
				sqlType = manager.getType(sqlTypeAnnotation.value());
			}
		}
		
		return sqlType;
	}
	
	private static int getPrecisionFromMethod(Class<?> type, Method method, FieldNameConverter converter) {
		TypeManager manager = TypeManager.getInstance();
		int precision = -1;
		
		precision = manager.getType(type).getDefaultPrecision();
		
		SQLType sqlTypeAnnotation = Common.getAnnotationDelegate(converter, method).getAnnotation(SQLType.class);
		if (sqlTypeAnnotation != null) {
			precision = sqlTypeAnnotation.precision();
		}
		
		return precision;
	}
	
	private static int getScaleFromMethod(Class<?> type, Method method, FieldNameConverter converter) {
		int scale = -1;
		
		SQLType sqlTypeAnnotation = Common.getAnnotationDelegate(converter, method).getAnnotation(SQLType.class);
		if (sqlTypeAnnotation != null) {
			scale = sqlTypeAnnotation.scale();
		}
		
		return scale;
	}
	
	private static DDLForeignKey[] parseForeignKeys(TableNameConverter nameConverter, FieldNameConverter fieldConverter,
			Class<? extends RawEntity<?>> clazz) {
		Set<DDLForeignKey> back = new LinkedHashSet<DDLForeignKey>();
		
		for (Method method : clazz.getMethods()) {
			String attributeName = fieldConverter.getName(method);
			Class<?> type =  Common.getAttributeTypeFromMethod(method);
			
			if (type != null && attributeName != null && Common.interfaceInheritsFrom(type, RawEntity.class)
					&& type.getAnnotation(Polymorphic.class) == null) {
				DDLForeignKey key = new DDLForeignKey();
				
				key.setField(attributeName);
				key.setTable(nameConverter.getName((Class<? extends RawEntity<?>>) type));
				key.setForeignField(Common.getPrimaryKeyField((Class<? extends RawEntity<?>>) type, fieldConverter));
				key.setDomesticTable(nameConverter.getName(clazz));
				
				back.add(key);
			}
		}
		
		return back.toArray(new DDLForeignKey[back.size()]);
	}
	
	private static DDLIndex[] parseIndexes(TableNameConverter nameConverter, FieldNameConverter fieldConverter, 
			Class<? extends RawEntity<?>> clazz) {
		Set<DDLIndex> back = new LinkedHashSet<DDLIndex>();
		String tableName = nameConverter.getName(clazz);
		
		for (Method method : clazz.getMethods()) {
			String attributeName = fieldConverter.getName(method);
			AnnotationDelegate annotations = Common.getAnnotationDelegate(fieldConverter, method);
			
			if (Common.isAccessor(method) || Common.isMutator(method)) {
				Indexed indexedAnno = annotations.getAnnotation(Indexed.class);
				Class<?> type = Common.getAttributeTypeFromMethod(method);
				
				if (indexedAnno != null || (type != null && Common.interfaceInheritsFrom(type, RawEntity.class))) {
					DDLIndex index = new DDLIndex();
					index.setField(attributeName);
					index.setTable(tableName);
					index.setType(getSQLTypeFromMethod(type, annotations));
					
					back.add(index);
				}
			}
		}
		
		for (Class<?> superInterface : clazz.getInterfaces()) {
			if (!superInterface.equals(RawEntity.class)) {
				back.addAll(Arrays.asList(parseIndexes(nameConverter, fieldConverter, (Class<? extends RawEntity<?>>) superInterface)));
			}
		}
		
		return back.toArray(new DDLIndex[back.size()]);
	}
	
	private static Object convertStringValue(String value, DatabaseType<?> type) {
		if (value == null) {
			return null;
		} else if (value.trim().equalsIgnoreCase("NULL")) {
			return value.trim();
		}
		
		DatabaseFunction func = DatabaseFunction.get(value.trim());
		if (func != null) {
			return func;
		}
		
		return type.defaultParseValue(value);
	}
}
