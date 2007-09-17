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
import java.sql.Types;
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

import net.java.ao.Common;
import net.java.ao.DatabaseFunction;
import net.java.ao.DatabaseProvider;
import net.java.ao.Entity;
import net.java.ao.ManyToMany;
import net.java.ao.OneToMany;
import net.java.ao.schema.ddl.DDLAction;
import net.java.ao.schema.ddl.DDLField;
import net.java.ao.schema.ddl.DDLForeignKey;
import net.java.ao.schema.ddl.DDLTable;
import net.java.ao.schema.ddl.SchemaReader;
import net.java.ao.types.DatabaseType;
import net.java.ao.types.TypeManager;

/**
 * @author Daniel Spiewak
 */
public final class Generator {
	
	public static void migrate(DatabaseProvider provider, Class<? extends Entity>... classes) throws SQLException {
		migrate(provider, new CamelCaseTableNameConverter(), new CamelCaseFieldNameConverter(), classes);
	}
	
	public static void migrate(DatabaseProvider provider, TableNameConverter nameConverter, FieldNameConverter fieldConverter,
			Class<? extends Entity>... classes) throws SQLException {
		String[] statements = null;
		try {
			statements = generateImpl(provider, nameConverter, fieldConverter, Generator.class.getClassLoader(), classes);
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
	
	public static boolean hasSchema(DatabaseProvider provider, TableNameConverter nameConverter,
			Class<? extends Entity>... classes) throws SQLException {
		if (classes.length == 0) {
			return true;
		}
		
		Connection conn = provider.getConnection();
		try {
			Statement stmt = conn.createStatement();
			
			try {
				// TODO	kind of a hacky way to figure this out, but it works
				stmt.executeQuery("SELECT * FROM " + nameConverter.getName(classes[0])).close();
			} catch (SQLException e) {
				return false;
			}
			
			stmt.close();
		} finally {
			conn.close();
		}
		
		return true;
	}
	
	private static String[] generateImpl(DatabaseProvider provider, TableNameConverter nameConverter, FieldNameConverter fieldConverter,
			ClassLoader classloader, Class<? extends Entity>... classes) throws ClassNotFoundException, SQLException {
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
			ClassLoader classloader, Class<? extends Entity>... classes) {
		Map<Class<? extends Entity>, Set<Class<? extends Entity>>> deps = 
			new HashMap<Class<? extends Entity>, Set<Class<? extends Entity>>>();
		Set<Class<? extends Entity>> roots = new LinkedHashSet<Class<? extends Entity>>();
		
		for (Class<? extends Entity> cls : classes) {
			parseDependencies(fieldConverter, deps, roots, cls);
		}
		
		List<DDLTable> parsedTables = new ArrayList<DDLTable>();
		
		while (!roots.isEmpty()) {
			Class<? extends Entity>[] rootsArray = roots.toArray(new Class[roots.size()]);
			roots.remove(rootsArray[0]);
			
			Class<? extends Entity> clazz = rootsArray[0];
			parsedTables.add(parseInterface(provider, nameConverter, fieldConverter, clazz));
			
			List<Class<? extends Entity>> toRemove = new LinkedList<Class<? extends Entity>>();
			Iterator<Class<? extends Entity>> depIterator = deps.keySet().iterator();
			while (depIterator.hasNext()) {
				Class<? extends Entity> depClass = depIterator.next();
				
				Set<Class<? extends Entity>> individualDeps = deps.get(depClass);
				individualDeps.remove(clazz);
				
				if (individualDeps.isEmpty()) {
					roots.add(depClass);
					toRemove.add(depClass);
				}
			}
			
			for (Class<? extends Entity> remove : toRemove) {
				deps.remove(remove);
			}
		}
		
		return parsedTables.toArray(new DDLTable[parsedTables.size()]);
	}
	
	private static void parseDependencies(FieldNameConverter fieldConverter, Map<Class <? extends Entity>, Set<Class<? extends Entity>>> deps, 
			Set<Class <? extends Entity>> roots, Class<? extends Entity>... classes) {
		for (Class<? extends Entity> clazz : classes) {
			if (deps.containsKey(clazz)) {
				continue;
			}
			
			Set<Class<? extends Entity>> individualDeps = new LinkedHashSet<Class<? extends Entity>>();
			
			for (Method method : clazz.getMethods()) {
				String attributeName = fieldConverter.getName(clazz, method);
				Class<?> type = Common.getAttributeTypeFromMethod(method);
				
				if (attributeName != null && type != null && Common.interfaceInheritsFrom(type, Entity.class)) {
					if (!type.equals(clazz)) {
						individualDeps.add((Class<? extends Entity>) type);
					
						parseDependencies(fieldConverter, deps, roots, (Class<? extends Entity>) type);
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
			FieldNameConverter fieldConverter, Class<? extends Entity> clazz) {
		String sqlName = nameConverter.getName(clazz);
		
		DDLTable table = new DDLTable();
		table.setName(sqlName);
		
		table.setFields(parseFields(clazz, fieldConverter));
		table.setForeignKeys(parseForeignKeys(nameConverter, fieldConverter, clazz));
		
		return table;
	}
	
	private static DDLField[] parseFields(Class<? extends Entity> clazz, FieldNameConverter fieldConverter) {
		List<DDLField> fields = new ArrayList<DDLField>();
		List<String> attributes = new LinkedList<String>();
		TypeManager manager = TypeManager.getInstance();
		
		DDLField field = new DDLField();
		
		field.setName("id");
		field.setAutoIncrement(true);
		field.setNotNull(true);
		field.setType(manager.getType(Types.INTEGER));
		field.setPrimaryKey(true);
		
		fields.add(field);
		
		for (Method method : clazz.getMethods()) {
			if (method.getAnnotation(Ignore.class) != null
					|| method.getAnnotation(OneToMany.class) != null
					|| method.getAnnotation(ManyToMany.class) != null) {
				continue;
			}
			
			String attributeName = fieldConverter.getName(clazz, method);
			Class<?> type = Common.getAttributeTypeFromMethod(method);
			
			if (attributeName != null && type != null) {
				if (attributes.contains(attributeName)) {
					continue;
				}
				attributes.add(attributeName);
				
				DatabaseType<?> sqlType = null;
				int precision = -1;
				int scale = -1;
				
				sqlType = manager.getType(type);
				precision = sqlType.getDefaultPrecision();
				
				SQLType sqlTypeAnnotation = method.getAnnotation(SQLType.class);
				if (sqlTypeAnnotation != null) {
					if (sqlTypeAnnotation.value() > 0) {
						sqlType = manager.getType(sqlTypeAnnotation.value());
					}
					precision = sqlTypeAnnotation.precision();
					scale = sqlTypeAnnotation.scale();
				}
				
				field = new DDLField();
				
				field.setName(attributeName);
				field.setType(sqlType);
				field.setPrecision(precision);
				field.setScale(scale);
				
				if (method.getAnnotation(PrimaryKey.class) != null) {
					field.setPrimaryKey(true);
				}
				
				if (method.getAnnotation(NotNull.class) != null) {
					field.setNotNull(true);
				}
				
				if (method.getAnnotation(Unique.class) != null) {
					field.setUnique(true);
					field.setNotNull(true);
				}
				
				if (method.getAnnotation(AutoIncrement.class) != null) {
					field.setAutoIncrement(true);
				} else if (method.getAnnotation(Default.class) != null) {
					field.setDefaultValue(convertStringValue(method.getAnnotation(Default.class).value(), sqlType));
				}
				
				if (method.getAnnotation(OnUpdate.class) != null) {
					field.setOnUpdate(convertStringValue(method.getAnnotation(OnUpdate.class).value(), sqlType));
				}
				
				fields.add(field);
			}
		}
		
		return fields.toArray(new DDLField[fields.size()]);
	}
	
	private static DDLForeignKey[] parseForeignKeys(TableNameConverter nameConverter, FieldNameConverter fieldConverter,
			Class<? extends Entity> clazz) {
		Set<DDLForeignKey> back = new LinkedHashSet<DDLForeignKey>();
		
		for (Method method : clazz.getMethods()) {
			String attributeName = fieldConverter.getName(clazz, method);
			Class<?> type =  Common.getAttributeTypeFromMethod(method);
			
			if (type != null && attributeName != null && Common.interfaceInheritsFrom(type, Entity.class)) {
				DDLForeignKey key = new DDLForeignKey();
				
				key.setField(attributeName);
				key.setTable(nameConverter.getName((Class<? extends Entity>) type));
				key.setForeignField("id");
				key.setDomesticTable(nameConverter.getName(clazz));
				
				back.add(key);
			}
		}
		
		for (Class<?> superInterface : clazz.getInterfaces()) {
			if (!superInterface.equals(Entity.class)) {
				back.addAll(Arrays.asList(parseForeignKeys(nameConverter, fieldConverter, (Class<? extends Entity>) superInterface)));
			}
		}
		
		return back.toArray(new DDLForeignKey[back.size()]);
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
