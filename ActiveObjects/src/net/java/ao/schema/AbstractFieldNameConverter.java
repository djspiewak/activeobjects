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

import net.java.ao.Accessor;
import net.java.ao.Common;
import net.java.ao.ManyToMany;
import net.java.ao.Mutator;
import net.java.ao.OneToMany;
import net.java.ao.OneToOne;
import net.java.ao.RawEntity;

/**
 * An abstract implementation of {@link FieldNameConverter} which handles common 
 * tasks for the name converter (i.e. relations annotations, accessor/mutator
 * annotations, etc).  For most tasks, custom field name converters should extend
 * this class, rather than directly implementing <code>FieldNameConverter</code>.
 * 
 * @author Daniel Spiewak
 */
public abstract class AbstractFieldNameConverter implements FieldNameConverter {
	
	/**
	 * Dummy constructor with protected visibility.  Does nothing.
	 */
	protected AbstractFieldNameConverter() {}

	/**
	 * <p>Handles operations which should be common to all field name converters
	 * such as overriding of the generated field name through annotations, etc.
	 * This method also handles the converting through the Java Bean method
	 * prefix convention (get/set/is), allowing the implementing class to only
	 * concern itself with converting one <code>String</code> (from the method
	 * name) into another.</p>
	 * 
	 * <p>This method delegates the actual conversion logic to the
	 * {@link #convertName(String, boolean, boolean)} method.  There is rarely a need
	 * for subclasses to override this method.</p>
	 * @param method	The method for which a field name must be generated.
	 * 
	 * @return	A valid database identifier to be used as the field name representative
	 * 		of the method in question.
	 * @see net.java.ao.schema.FieldNameConverter#getName(Method)
	 */
	public String getName(Method method) {
		return getNameImpl(method, false);
	}
	
	/**
	 * Docuentation on the {@link #getName(Method)} method.
	 * 
	 * @return	A valid database identifier to be used as the field name representative
	 * 		of the method in question.
	 * @see net.java.ao.schema.FieldNameConverter#getPolyTypeName(Method)
	 */
	public String getPolyTypeName(Method method) {
		return getNameImpl(method, true);
	}
	
	private String getNameImpl(Method method, boolean polyType) {
		if (method == null) {
			throw new IllegalArgumentException("Problem in ActiveObjects core, looking for field name for null method");
		}
		
		String attributeName = null;
		Class<?> type = Common.getAttributeTypeFromMethod(method);
		
		Mutator mutatorAnnotation = method.getAnnotation(Mutator.class);
		Accessor accessorAnnotation = method.getAnnotation(Accessor.class);
		PrimaryKey primaryKeyAnnotation = method.getAnnotation(PrimaryKey.class);
		OneToOne oneToOneAnnotation = method.getAnnotation(OneToOne.class);
		OneToMany oneToManyAnnotation = method.getAnnotation(OneToMany.class);
		ManyToMany manyToManyAnnotation = method.getAnnotation(ManyToMany.class);
		
		if (mutatorAnnotation != null) {
			attributeName = mutatorAnnotation.value();
			
			if (!polyType) {
				return attributeName;
			}
		} else if (accessorAnnotation != null) {
			attributeName = accessorAnnotation.value();
			
			if (!polyType) {
				return attributeName;
			}
		} else if (primaryKeyAnnotation != null && !primaryKeyAnnotation.value().trim().equals("")) {
			attributeName = primaryKeyAnnotation.value();
			
			if (!polyType) {
				return attributeName;
			}
		} else if (oneToOneAnnotation != null) {
			return null;
		} else if (oneToManyAnnotation != null) {
			return null;
		} else if (manyToManyAnnotation != null) {
			return null;
		} else if (method.getName().startsWith("get") || method.getName().startsWith("set")) {
			attributeName = method.getName().substring(3);
		} else if (method.getName().startsWith("is")) {
			attributeName = method.getName().substring(2);
		} else {
			return null;
		}
		
		return convertName(attributeName, Common.interfaceInheritsFrom(type, RawEntity.class), polyType);
	}
	
	/**
	 * <p>Performs the actual conversion logic between a method name (or, more normally
	 * a trimmed method name) and the corresponding database field identifier.  This
	 * method may impose conventions such as camelCase, all-lowercase with underscores
	 * and so on.  There is no need for this method to concern itself with method
	 * prefixes such as get, set or is.  All of these should be handled within the
	 * {@link #getName(Method)} method.</p>
	 * 
	 * <p>Some examples of input and their corresponding return values for this method
	 * (assuming the {@link CamelCaseFieldNameConverter} is in use):</p>
	 * 
	 * <table border="1">
	 * 		<tr>
	 * 			<td><b>Actual Method Name</b></td>
	 * 			<td><b>Param: name</b></td>
	 * 			<td><b>Param: entity</b></td>
	 * 			<td><b>Param: polyType</b></td>
	 * 			<td><b>Return Value</b></td>
	 * 		</tr>
	 * 		<tr>
	 * 			<td>getFirstName</td>
	 * 			<td>FirstName</td>
	 * 			<td><code>false</code></td>
	 * 			<td><code>false</code></td>
	 * 			<td>firstName</td>
	 * 		</tr>
	 * 		<tr>
	 * 			<td>getCompany</td>
	 * 			<td>Company</td>
	 * 			<td><code>true</code></td>
	 * 			<td><code>false</code></td>
	 * 			<td>companyID</td>
	 * 		</tr>
	 * 		<tr>
	 * 			<td>getCompany</td>
	 * 			<td>Company</td>
	 * 			<td><code>true</code></td>
	 * 			<td><code>true</code></td>
	 * 			<td>companyType</td>
	 * 		</tr>
	 * 		<tr>
	 * 			<td>isCool</td>
	 * 			<td>Cool</td>
	 * 			<td><code>false</code></td>
	 * 			<td><code>false</code></td>
	 * 			<td>cool</td>
	 * 		</tr>
	 * 		<tr>
	 * 			<td>setLastName</td>
	 * 			<td>LastName</td>
	 * 			<td><code>false</code></td>
	 * 			<td><code>false</code></td>
	 * 			<td>lastName</td>
	 * 		</tr>
	 * </table>
	 * 
	 * <p>The implementation of this method must execute extremely quickly and be
	 * totally thread-safe (stateless if possible).  This is because this method will
	 * be called many times for some operations.  A slow algorithm here will dramaticly
	 * affect the execution time of basic tasks.</p>
	 * 
	 * @param name	The (often trimmed) method name for which a field name is reqiured.
	 * @param entity	Indicates whether or not the method in question returns an
	 * 		entity value.
	 * @param polyType	Indicates whether or not the field in question is a polymorphic
	 * 		type flagging field.
	 * @return	A valid database field name which uniquely corresponds to the method
	 *		name in question.  Should <i>never</i> return <code>null</code>.
	 */
	protected abstract String convertName(String name, boolean entity, boolean polyType);
}
