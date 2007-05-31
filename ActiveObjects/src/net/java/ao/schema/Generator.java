/*
 * Copyright 2007, Daniel Spiewak
 * All rights reserved
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided
 *     with the distribution.
 *   * Neither the name of the ActiveObjects project nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.java.ao.schema;

import static net.java.ao.Common.getAttributeNameFromMethod;
import static net.java.ao.Common.getAttributeTypeFromMethod;
import static net.java.ao.Common.getTableName;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.java.ao.Common;
import net.java.ao.DatabaseProvider;
import net.java.ao.Entity;
import net.java.ao.ManyToMany;
import net.java.ao.schema.ddl.DDLField;
import net.java.ao.schema.ddl.DDLForeignKey;
import net.java.ao.schema.ddl.DDLTable;

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
	private static final String CL_INVOCATION = "java -jar activeobjects-*.jar <options> class1 class2 ...";
	
	public static void main(String... args) throws ParseException, IOException, ClassNotFoundException {
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
		
		if (cl.getOptionValue(classpathOption.getOpt()) == null || cl.getArgs().length == 0) {
			new HelpFormatter().printHelp(CL_INVOCATION, options);
			System.exit(-1);
		}
		
		System.out.println(generate(cl.getOptionValue(classpathOption.getOpt()), cl.getOptionValue(uriOption.getOpt()), cl.getArgs()));
	}
	
	public static String generate(String classpath, String uri, String... classes) throws ClassNotFoundException, MalformedURLException, IOException {
		ClassLoader classloader = new URLClassLoader(new URL[] {new URL("file://" + new File(classpath).getCanonicalPath() + "/")});
		
		String sql = "";
		DatabaseProvider provider = DatabaseProvider.getInstance(uri, null, null, false);
		
		String[] statements = generateImpl(provider, classloader, classes);
		for (String statement : statements) {
			sql += statement + ";\n";
		}
		
		return sql;
	}
	
	public static void migrate(DatabaseProvider provider, Class<? extends Entity>... classes) throws SQLException {
		List<String> classNames = new ArrayList<String>();
		
		for (Class<? extends Entity> clazz : classes) {
			classNames.add(clazz.getCanonicalName());
		}
		
		String[] statements = null;
		try {
			statements = generateImpl(provider, Generator.class.getClassLoader(), classNames.toArray(new String[classNames.size()]));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		Connection conn = provider.getConnection();
		try {
			Statement stmt = conn.createStatement();
			
			for (String statement : statements) {
				if (!statement.trim().equals("")) {
					stmt.executeUpdate(statement);
				}
			}
			
			stmt.close();
		} finally {
			conn.close();
		}
	}
	
	private static String[] generateImpl(DatabaseProvider provider, ClassLoader classloader, String... classes) throws ClassNotFoundException {
		List<String> back = new ArrayList<String>();
		List<Class<? extends Entity>> parsed = new LinkedList<Class<? extends Entity>>();
		
		for (String cls : classes) {
			parseInterface(provider, (Class<? extends Entity>) Class.forName(cls, true, classloader), parsed, back);
		}
		Collections.reverse(back);		// dependency tree is implicitly built by the recursive algorithm, reversing the list is sufficient
		
		return back.toArray(new String[back.size()]);
	}
	
	private static void parseInterface(DatabaseProvider provider, Class<? extends Entity> clazz, List<Class<? extends Entity>> parsed,
			List<String> back) {
		if (parsed.contains(clazz)) {
			return;
		}
		parsed.add(clazz);
		
		StringBuilder sql = new StringBuilder();
		String sqlName = Common.getTableName(clazz);
		
		Set<Class<? extends Entity>> classes = new LinkedHashSet<Class<? extends Entity>>();
		
		DDLTable table = new DDLTable();
		table.setName(sqlName);
		
		table.setFields(parseFields(clazz, classes));
		table.setForeignKeys(parseForeignKeys(clazz));
		
		sql.append(provider.render(table));
		
		back.add(sql.toString());
		
		for (Class<? extends Entity> refClass : classes) {
			parseInterface(provider, refClass, parsed, back);
		}
	}
	
	private static DDLField[] parseFields(Class<? extends Entity> clazz, Set<Class<? extends Entity>> classes) {
		List<DDLField> fields = new ArrayList<DDLField>();
		
		List<String> attributes = new LinkedList<String>();
		
		DDLField field = new DDLField();
		
		field.setName("id");
		field.setAutoIncrement(true);
		field.setNotNull(true);
		field.setType(Types.INTEGER);
		field.setPrimaryKey(true);
		
		fields.add(field);
		
		for (Method method : clazz.getDeclaredMethods()) {
			ManyToMany manyToManyAnnotation = method.getAnnotation(ManyToMany.class);
			
			String attributeName = getAttributeNameFromMethod(method);
			Class<?> type = getAttributeTypeFromMethod(method);
			
			if (manyToManyAnnotation != null) {
				classes.add(manyToManyAnnotation.value());
			}
			
			if (attributeName != null && type != null) {
				if (attributes.contains(attributeName)) {
					continue;
				}
				attributes.add(attributeName);
				
				int sqlType = -1;
				int precision = -1;
				int scale = -1;
				
				SQLType sqlTypeAnnotation = method.getAnnotation(SQLType.class);
				if (sqlTypeAnnotation != null) {
					sqlType = sqlTypeAnnotation.value();
					precision = sqlTypeAnnotation.precision();
					scale = sqlTypeAnnotation.scale();
				} else if (interfaceInheritsFrom(type, Entity.class)) {
					classes.add((Class<? extends Entity>) type);
					
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
				field.setScale(scale);
				
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
	
	private static DDLForeignKey[] parseForeignKeys(Class<? extends Entity> clazz) {
		Set<DDLForeignKey> back = new LinkedHashSet<DDLForeignKey>();
		
		for (Method method : clazz.getMethods()) {
			String attributeName = getAttributeNameFromMethod(method);
			Class<?> type =  getAttributeTypeFromMethod(method);
			
			if (type != null && attributeName != null && interfaceInheritsFrom(type, Entity.class)) {
				DDLForeignKey key = new DDLForeignKey();
				
				key.setField(attributeName);
				key.setTable(getTableName((Class<? extends Entity>) type));
				key.setForeignField("id");
				
				back.add(key);
			}
		}
		
		for (Class<?> superInterface : clazz.getInterfaces()) {
			if (!superInterface.equals(Entity.class)) {
				back.addAll(Arrays.asList(parseForeignKeys((Class<? extends Entity>) superInterface)));
			}
		}
		
		return back.toArray(new DDLForeignKey[back.size()]);
	}
}
