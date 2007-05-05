/*
 * Created on May 4, 2007
 */
package net.java.ao.schema;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.java.ao.Accessor;
import net.java.ao.Entity;
import net.java.ao.EntityProxy;
import net.java.ao.Mutator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import static net.java.ao.Utilities.*;

/**
 * @author Daniel Spiewak
 */
public class Generator {
	private static List<Class<? extends Entity>> parsed = new LinkedList<Class<? extends Entity>>(); 
	
	public static void main(String... args) throws ParseException, IOException, ClassNotFoundException {
		Options options = new Options();
		
		Option classpathOption = new Option("C", "classpath", true, "");
		options.addOption(classpathOption);
		
		CommandLineParser parser = new PosixParser();
		CommandLine cl = parser.parse(options, args);
		
		String[] classes = cl.getArgs();
		String classpathValue = cl.getOptionValue(classpathOption.getOpt());
		
		if (classpathValue == null || classes.length == 0) {
			usage();
			System.exit(-1);
		}
		
		URLClassLoader classloader = new URLClassLoader(new URL[] {new URL("file://" + new File(classpathValue).getCanonicalPath())});
		
		String sql = "";
		for (String cls : classes) {
			sql += parseInterface((Class<? extends Entity>) Class.forName(cls, true, classloader));
		}
		
		System.out.println(sql);
	}
	
	private static void usage() {
		System.err.println("Usage: java org.ds.dao.schema.Generator [-C|--classpath] <dir> class1 class2 class3 ...");
	}
	
	private static String parseInterface(Class<? extends Entity> clazz) {
		if (parsed.contains(clazz)) {
			return "";
		}
		parsed.add(clazz);
		
		StringBuffer sql = new StringBuffer();
		
		String sqlName = convertDowncaseName(convertSimpleClassName(clazz.getName()));
		
		if (clazz.getAnnotation(Table.class) != null) {
			sqlName = clazz.getAnnotation(Table.class).value();
		}
		
		Method[] methods = clazz.getDeclaredMethods();
		List<String> attributes = new LinkedList<String>();
		List<Class<? extends Entity>> classes = new ArrayList<Class<? extends Entity>>();
		
		sql.append("CREATE TABLE IF NOT EXISTS ");
		sql.append(sqlName);
		sql.append(" (\n");
		
		for (Method method : methods) {
			Mutator mutatorAnnotation = method.getAnnotation(Mutator.class);
			Accessor accessorAnnotation = method.getAnnotation(Accessor.class);
			String attributeName = null;
			Class<?> type = null;
			
			if (method.getName().startsWith("get")) {
				attributeName = convertDowncaseName(method.getName().substring(3));
				type = method.getReturnType();
			} else if (method.getName().startsWith("is")) {
				attributeName = convertDowncaseName(method.getName().substring(2));
				type = method.getReturnType();
			} else if (method.getName().startsWith("set")) {
				attributeName = convertDowncaseName(method.getName().substring(3));
				type = method.getParameterTypes()[0];
			} else if (mutatorAnnotation != null) {
				attributeName = mutatorAnnotation.value();
				type = method.getParameterTypes()[0];
			} else if (accessorAnnotation != null) {
				attributeName = accessorAnnotation.value();
				type = method.getReturnType();
			}
			
			if (attributeName != null && type != null) {
				if (attributes.contains(attributeName)) {
					continue;
				}
				attributes.add(attributeName);
				
				String sqlType = null;
				
				if (method.getAnnotation(SQLType.class) != null) {
					sqlType = method.getAnnotation(SQLType.class).value();
				} else if (interfaceIneritsFrom(type, Entity.class)) {
					classes.add((Class<? extends Entity>) type);
					
					attributeName += "ID";
					sqlType = "INTEGER";
				} else if (type.isArray()) {
					continue;
				} else {
					sqlType = SQLTypeEnum.getType(type).getSqlName();
				}
				
				sql.append(attributeName);
				sql.append(" AS ");
				sql.append(sqlType);
				
				if (method.getAnnotation(NotNull.class) != null) {
					sql.append(" NOT NULL");
				}
				
				if (method.getAnnotation(AutoIncrement.class) != null) {
					sql.append(" AUTO_INCREMENT");
				} else if (method.getAnnotation(Default.class) != null) {
					sql.append(" DEFAULT ");
					sql.append(method.getAnnotation(Default.class).value());
				}
				
				sql.append(",\n");
			}
		}
		
		sql.setLength(sql.length() - 2);
		sql.append("\n);\n");
		
		for (Class<? extends Entity> refClass : classes) {
			sql.append(parseInterface(refClass));
		}
		
		return sql.toString();
	}
}
