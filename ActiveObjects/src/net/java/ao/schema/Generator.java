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

import static net.java.ao.Common.getAttributeNameFromMethod;
import static net.java.ao.Common.getAttributeTypeFromMethod;
import static net.java.ao.Common.interfaceInheritsFrom;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.java.ao.DatabaseFunction;
import net.java.ao.DatabaseProvider;
import net.java.ao.Entity;
import net.java.ao.schema.ddl.DDLAction;
import net.java.ao.schema.ddl.DDLField;
import net.java.ao.schema.ddl.DDLForeignKey;
import net.java.ao.schema.ddl.DDLTable;
import net.java.ao.schema.ddl.SchemaReader;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/**
 * @author Daniel Spiewak
 */
public final class Generator {
	private static final String CL_INVOCATION = "java -jar activeobjects-*.jar <options> class1 class2 ...";
	
	public static void main(String... args) throws ParseException, IOException, ClassNotFoundException, SQLException {
		Options options = new Options();
		
		Option classpathOption = new Option("C", "classpath", true, "(required) The path from which to load the " +
				"specified entity classes");
		classpathOption.setRequired(true);
		options.addOption(classpathOption);
		
		Option nameConverterOption = new Option("N", "converter", true, "A qualified Java class-name specifying which " +
				"name converter to use");
		options.addOption(nameConverterOption);
		
		Option uriOption = new Option("U", "uri", true, "(required) The JDBC URI used to access the database against " +
				"which the schema will be created");
		uriOption.setRequired(true);
		options.addOption(uriOption);
		
		CommandLineParser parser = new PosixParser();
		CommandLine cl = null;
		try {
			cl = parser.parse(options, args);
		} catch (ParseException e) {
			new HelpFormatter().printHelp(CL_INVOCATION, options);
			System.exit(-1);
		}
		
		if (cl.getOptionValue(classpathOption.getOpt()) == null || cl.getArgs().length == 0) {
			new HelpFormatter().printHelp(CL_INVOCATION, options);
			System.exit(-1);
		}
		
		System.out.println(generate(cl.getOptionValue(classpathOption.getOpt()), cl.getOptionValue(uriOption.getOpt()), 
				cl.getOptionValue(nameConverterOption.getOpt()), cl.getArgs()));
	}
	
	public static String generate(String classpath, String uri, String... classes) throws ClassNotFoundException, MalformedURLException, IOException, SQLException {
		return generate(classpath, uri, null, classes);
	}
	
	public static String generate(String classpath, String uri, String nameConverterClassname, 
			String... classes) throws ClassNotFoundException, MalformedURLException, IOException, SQLException {
		ClassLoader classloader = new URLClassLoader(new URL[] {new URL("file://" + new File(classpath).getCanonicalPath() + "/")});
		
		String sql = "";
		DatabaseProvider provider = DatabaseProvider.getInstance(uri, null, null, false);
		
		String[] statements = generateImpl(provider, loadConverter(classloader, nameConverterClassname), classloader, classes);
		for (String statement : statements) {
			sql += statement + ";\n";
		}
		
		return sql;
	}
	
	public static void migrate(DatabaseProvider provider, Class<? extends Entity>... classes) throws SQLException {
		migrate(provider, new CamelCaseNameConverter(), classes);
	}
	
	public static void migrate(DatabaseProvider provider, PluggableNameConverter nameConverter,
			Class<? extends Entity>... classes) throws SQLException {
		List<String> classNames = new ArrayList<String>();
		
		for (Class<? extends Entity> clazz : classes) {
			classNames.add(clazz.getCanonicalName());
		}
		
		String[] statements = null;
		try {
			statements = generateImpl(provider, nameConverter, Generator.class.getClassLoader(), classNames.toArray(new String[classNames.size()]));
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
	
	public static boolean hasSchema(DatabaseProvider provider, PluggableNameConverter nameConverter,
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
	
	private static PluggableNameConverter loadConverter(ClassLoader classloader, String name) {
		if (name != null && name.split(".").length == 0) {
			name = "net.java.ao.schema." + name;
		}
		
		Class<? extends PluggableNameConverter> converterClass = null;
		try {
			converterClass = (Class<? extends PluggableNameConverter>) Class.forName(name);
		} catch (Throwable t) {
		}
		
		if (converterClass == null) {
			try {
				converterClass = (Class<? extends PluggableNameConverter>) Class.forName(name, true, classloader);
			} catch (Throwable t) {
			}
		}
		
		PluggableNameConverter nameConverter = null;
		try {
			nameConverter = converterClass.newInstance();
		} catch (Throwable t) {
			System.err.println("Unable to load " + name);
			System.err.println("Using default name converter...");
			
			nameConverter = new CamelCaseNameConverter();
		}
		
		return nameConverter;
	}
	
	private static String[] generateImpl(DatabaseProvider provider, PluggableNameConverter nameConverter, ClassLoader classloader, String... classes) throws ClassNotFoundException, SQLException {
		List<String> back = new ArrayList<String>();
		Map<Class<? extends Entity>, Set<Class<? extends Entity>>> deps = 
			new HashMap<Class<? extends Entity>, Set<Class<? extends Entity>>>();
		Set<Class<? extends Entity>> roots = new LinkedHashSet<Class<? extends Entity>>();
		
		for (String cls : classes) {
			parseDependencies(deps, roots, (Class<? extends Entity>) Class.forName(cls, true, classloader));
		}
		
		List<DDLTable> parsedTables = new ArrayList<DDLTable>();
		DDLTable[] readTables = SchemaReader.readSchema(provider);
		
		while (!roots.isEmpty()) {
			Class<? extends Entity>[] rootsArray = roots.toArray(new Class[roots.size()]);
			roots.remove(rootsArray[0]);
			
			Class<? extends Entity> clazz = rootsArray[0];
			parsedTables.add(parseInterface(provider, nameConverter, clazz));
			
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
		
		DDLAction[] actions = SchemaReader.diffSchema(parsedTables.toArray(new DDLTable[parsedTables.size()]), readTables);
		for (DDLAction action : actions) {
			back.addAll(Arrays.asList(provider.renderAction(action)));
		}
		
		return back.toArray(new String[back.size()]);
	}
	
	private static void parseDependencies(Map<Class <? extends Entity>, Set<Class<? extends Entity>>> deps, 
			Set<Class <? extends Entity>> roots, Class<? extends Entity>... classes) {
		for (Class<? extends Entity> clazz : classes) {
			if (deps.containsKey(clazz)) {
				continue;
			}
			
			Set<Class<? extends Entity>> individualDeps = new LinkedHashSet<Class<? extends Entity>>();
			
			for (Method method : clazz.getMethods()) {
				String attributeName = getAttributeNameFromMethod(method);
				Class<?> type = getAttributeTypeFromMethod(method);
				
				if (attributeName != null && type != null && interfaceInheritsFrom(type, Entity.class)) {
					if (!type.equals(clazz)) {
						individualDeps.add((Class<? extends Entity>) type);
					
						parseDependencies(deps, roots, (Class<? extends Entity>) type);
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
	
	private static DDLTable parseInterface(DatabaseProvider provider, PluggableNameConverter nameConverter, Class<? extends Entity> clazz) {
		String sqlName = nameConverter.getName(clazz);
		
		DDLTable table = new DDLTable();
		table.setName(sqlName);
		
		table.setFields(parseFields(clazz));
		table.setForeignKeys(parseForeignKeys(nameConverter, clazz));
		
		return table;
	}
	
	private static DDLField[] parseFields(Class<? extends Entity> clazz) {
		List<DDLField> fields = new ArrayList<DDLField>();
		List<String> attributes = new LinkedList<String>();
		
		DDLField field = new DDLField();
		
		field.setName("id");
		field.setAutoIncrement(true);
		field.setNotNull(true);
		field.setType(Types.INTEGER);
		field.setPrimaryKey(true);
		
		fields.add(field);
		
		for (Method method : clazz.getMethods()) {
			if (method.getAnnotation(Ignore.class) != null) {
				continue;
			}
			
			String attributeName = getAttributeNameFromMethod(method);
			Class<?> type = getAttributeTypeFromMethod(method);
			
			if (attributeName != null && type != null) {
				if (attributes.contains(attributeName)) {
					continue;
				}
				attributes.add(attributeName);
				
				int sqlType = -1;
				int precision = -1;
				int scale = -1;
				
				if (interfaceInheritsFrom(type, Entity.class)) {
					sqlType = Types.INTEGER;
				} else if (type.isArray()) {
					continue;
				} else {
					sqlType = SQLTypeEnum.getType(type).getSQLType();
					precision = SQLTypeEnum.getType(type).getPrecision();
				}
				
				SQLType sqlTypeAnnotation = method.getAnnotation(SQLType.class);
				if (sqlTypeAnnotation != null) {
					if (sqlTypeAnnotation.value() > 0) {
						sqlType = sqlTypeAnnotation.value();
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
	
	private static DDLForeignKey[] parseForeignKeys(PluggableNameConverter nameConverter, Class<? extends Entity> clazz) {
		Set<DDLForeignKey> back = new LinkedHashSet<DDLForeignKey>();
		
		for (Method method : clazz.getMethods()) {
			String attributeName = getAttributeNameFromMethod(method);
			Class<?> type =  getAttributeTypeFromMethod(method);
			
			if (type != null && attributeName != null && interfaceInheritsFrom(type, Entity.class)) {
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
				back.addAll(Arrays.asList(parseForeignKeys(nameConverter, (Class<? extends Entity>) superInterface)));
			}
		}
		
		return back.toArray(new DDLForeignKey[back.size()]);
	}
	
	private static Object convertStringValue(String value, int type) {
		if (value == null) {
			return null;
		} else if (value.trim().equals("NULL")) {
			return value.trim();
		}
		
		DatabaseFunction func = DatabaseFunction.get(value.trim());
		if (func != null) {
			return func;
		}
		
		switch (type) {
			case Types.BIGINT:
				return Long.parseLong(value.trim());
				
			case Types.BIT:
				return Byte.parseByte(value.trim());
				
			case Types.BOOLEAN:
				return Boolean.parseBoolean(value.trim());
				
			case Types.CHAR:
				return value.charAt(0);
				
			case Types.DATE:
				return parseCalendar(value.trim());
				
			case Types.DECIMAL:
				return Double.parseDouble(value.trim());
				
			case Types.DOUBLE:
				return Double.parseDouble(value.trim());
				
			case Types.FLOAT:
				return Float.parseFloat(value.trim());
				
			case Types.INTEGER:
				return Integer.parseInt(value.trim());
				
			case Types.NUMERIC:
				return Integer.parseInt(value.trim());
				
			case Types.REAL:
				return Double.parseDouble(value.trim());
				
			case Types.SMALLINT:
				return Short.parseShort(value.trim());
				
			case Types.TIME:
				return parseCalendar(value.trim());
				
			case Types.TIMESTAMP:
				return parseCalendar(value.trim());
				
			case Types.TINYINT:
				return Short.parseShort(value.trim());
				
			case Types.VARCHAR:
				return "'" + value + "'";
		}
		
		return null;
	}
	
	private static Calendar parseCalendar(String ts) {
		try {
			Calendar cal = Calendar.getInstance();
			cal.setTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse(ts));
			
			return cal;
		} catch (java.text.ParseException e) {
			return Calendar.getInstance();
		}
	}
}
