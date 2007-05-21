/*
 * Created on May 4, 2007
 */
package net.java.ao.schema;

import static net.java.ao.Utilities.convertDowncaseName;
import static net.java.ao.Utilities.interfaceIneritsFrom;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.java.ao.Accessor;
import net.java.ao.DatabaseProvider;
import net.java.ao.Entity;
import net.java.ao.ManyToMany;
import net.java.ao.Mutator;
import net.java.ao.OneToMany;
import net.java.ao.Utilities;
import net.java.ao.db.ddl.DDLField;
import net.java.ao.db.ddl.DDLTable;

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
public class Generator {
	private static final String CL_INVOCATION = "java net.java.ao.schema.Generator <options> class1 class2 ...";
	
	private static List<Class<? extends Entity>> parsed = new LinkedList<Class<? extends Entity>>();
	
	public static void main(String... args) throws ParseException, IOException, ClassNotFoundException {
		
		System.out.println(generate(args));
	}
	
	public static String generate(String... args) throws ClassNotFoundException, MalformedURLException, IOException {
		Options options = new Options();
		
		Option classpathOption = new Option("C", "classpath", true, "(required) The path from which to load the " +
				"specified entity classes");
		classpathOption.setRequired(true);
		options.addOption(classpathOption);
		
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
		
		String[] classes = cl.getArgs();
		String classpathValue = cl.getOptionValue(classpathOption.getOpt());
		
		if (classpathValue == null || classes.length == 0) {
			new HelpFormatter().printHelp(CL_INVOCATION, options);
			System.exit(-1);
		}
		
		URLClassLoader classloader = new URLClassLoader(new URL[] {new URL("file://" + new File(classpathValue).getCanonicalPath())});
		
		String sql = "";
		DatabaseProvider provider = DatabaseProvider.getInstance(cl.getOptionValue(uriOption.getOpt()), null, null);
		for (String cls : classes) {
			sql += parseInterface(provider, (Class<? extends Entity>) Class.forName(cls, true, classloader));
		}
		
		return sql;
	}
	
	private static String parseInterface(DatabaseProvider provider, Class<? extends Entity> clazz) {
		if (parsed.contains(clazz)) {
			return "";
		}
		parsed.add(clazz);
		
		StringBuilder sql = new StringBuilder();
		String sqlName = Utilities.getTableName(clazz);
		
		Set<Class<? extends Entity>> classes = new LinkedHashSet<Class<? extends Entity>>();
		
		DDLTable table = new DDLTable();
		table.setName(sqlName);
		
		table.setFields(parseFields(clazz, classes));
		
		sql.append(provider.render(table));
		sql.append('\n');
		
		for (Class<? extends Entity> refClass : classes) {
			sql.append(parseInterface(provider, refClass));
		}
		
		return sql.toString();
	}
	
	private static DDLField[] parseFields(Class<? extends Entity> clazz, Set<Class<? extends Entity>> classes) {
		List<DDLField> fields = new ArrayList<DDLField>();
		
		Method[] methods = clazz.getDeclaredMethods();
		List<String> attributes = new LinkedList<String>();
		
		DDLField field = new DDLField();
		
		field.setName("id");
		field.setAutoIncrement(true);
		field.setNotNull(true);
		field.setType(Types.INTEGER);
		field.setPrimaryKey(true);
		
		fields.add(field);
		
		for (Method method : methods) {
			Mutator mutatorAnnotation = method.getAnnotation(Mutator.class);
			Accessor accessorAnnotation = method.getAnnotation(Accessor.class);
			OneToMany oneToManyAnnotation = method.getAnnotation(OneToMany.class);
			ManyToMany manyToManyAnnotation = method.getAnnotation(ManyToMany.class);
			
			String attributeName = null;
			Class<?> type = null;
			
			if (mutatorAnnotation != null) {
				attributeName = mutatorAnnotation.value();
				type = method.getParameterTypes()[0];
			} else if (accessorAnnotation != null) {
				attributeName = accessorAnnotation.value();
				type = method.getReturnType();
			} else if (oneToManyAnnotation != null) {
			} else if (manyToManyAnnotation != null) {
				classes.add(manyToManyAnnotation.value());
			} else if (method.getName().startsWith("get")) {
				attributeName = convertDowncaseName(method.getName().substring(3));
				type = method.getReturnType();
			} else if (method.getName().startsWith("is")) {
				attributeName = convertDowncaseName(method.getName().substring(2));
				type = method.getReturnType();
			} else if (method.getName().startsWith("set")) {
				attributeName = convertDowncaseName(method.getName().substring(3));
				type = method.getParameterTypes()[0];
			}
			
			if (attributeName != null && type != null) {
				if (attributes.contains(attributeName)) {
					continue;
				}
				attributes.add(attributeName);
				
				int sqlType = -1;
				int precision = -1;
				
				if (method.getAnnotation(SQLType.class) != null) {
					sqlType = method.getAnnotation(SQLType.class).value();
				} else if (interfaceIneritsFrom(type, Entity.class)) {
					classes.add((Class<? extends Entity>) type);
					
					if (mutatorAnnotation == null && accessorAnnotation == null) {
						attributeName += "ID";
					}
					
					sqlType = Types.INTEGER;
				} else if (type.isArray()) {
					continue;
				} else {
					sqlType = SQLTypeEnum.getType(type).getSQLType();
					precision = SQLTypeEnum.getType(type).getPrecision();
				}
				
				field = new DDLField();
				
				field.setName(attributeName);
				field.setType(sqlType);
				field.setPrecision(precision);
				
				if (method.getAnnotation(NotNull.class) != null) {
					field.setNotNull(true);
				}
				
				if (method.getAnnotation(AutoIncrement.class) != null) {
					field.setAutoIncrement(true);
				} else if (method.getAnnotation(Default.class) != null) {
					field.setDefaultValue(method.getAnnotation(Default.class).value());
				}
				
				fields.add(field);
			}
		}
		
		for (Class<?> superInterface : clazz.getInterfaces()) {
			if (!superInterface.equals(Entity.class)) {
				fields.addAll(Arrays.asList(parseFields((Class<? extends Entity>) superInterface, classes)));
			}
		}
		
		return fields.toArray(new DDLField[fields.size()]);
	}
}
