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
import net.java.ao.RawEntity;

/**
 * @author Daniel Spiewak
 */
public abstract class AbstractFieldNameConverter implements FieldNameConverter {
	
	protected AbstractFieldNameConverter() {}

	public String getName(Class<? extends RawEntity> clazz, Method method) {
		Mutator mutatorAnnotation = method.getAnnotation(Mutator.class);
		Accessor accessorAnnotation = method.getAnnotation(Accessor.class);
		OneToMany oneToManyAnnotation = method.getAnnotation(OneToMany.class);
		ManyToMany manyToManyAnnotation = method.getAnnotation(ManyToMany.class);
		
		String attributeName = null;
		Class<?> type = Common.getAttributeTypeFromMethod(method);
		
		if (mutatorAnnotation != null) {
			return mutatorAnnotation.value();
		} else if (accessorAnnotation != null) {
			return accessorAnnotation.value();
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
		
		return convertName(attributeName, Common.interfaceInheritsFrom(type, RawEntity.class));
	}
	
	protected abstract String convertName(String name, boolean entity);
}
