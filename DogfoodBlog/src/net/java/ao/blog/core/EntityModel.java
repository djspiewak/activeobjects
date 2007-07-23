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
package net.java.ao.blog.core;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.java.ao.Entity;
import net.java.ao.EntityManager;

import org.apache.wicket.model.IModel;

/**
 * @author Daniel Spiewak
 */
public abstract class EntityModel implements IModel, Serializable {

	private int id;

	private String property;

	private Class<? extends Entity> type;

	public EntityModel(Entity entity, String property) {
		id = entity.getID();
		type = entity.getEntityType();

		this.property = property;
	}

	public void detach() {
	}

	public Entity getEntity() {
		return getEntityManager().get(type, id);
	}

	public abstract EntityManager getEntityManager();

	public Object getObject() {
		String capitolizedProperty = Character.toUpperCase(property.charAt(0)) + property.substring(1);
		Method m = null;

		try {
			m = type.getMethod("get" + capitolizedProperty);
		} catch (SecurityException e) {
		} catch (NoSuchMethodException e) {
		}

		if (m == null) {
			try {
				m = type.getMethod("is" + capitolizedProperty);
			} catch (SecurityException e) {
			} catch (NoSuchMethodException e) {
			}
		}

		if (m == null) {
			return null;
		}

		try {
			return m.invoke(getEntity());
		} catch (IllegalArgumentException e) {
		} catch (IllegalAccessException e) {
		} catch (InvocationTargetException e) {
		}

		return null;
	}

	public void setObject(Object object) {
		String capitolizedProperty = Character.toUpperCase(property.charAt(0)) + property.substring(1);
		Method m = null;

		for (Method method : type.getMethods()) {
			if (method.getName().equals("set" + capitolizedProperty)) {
				m = method;
				break;
			}
		}

		if (m == null) {
			return;
		}

		try {
			m.invoke(getEntity(), object);
		} catch (IllegalArgumentException e) {
		} catch (IllegalAccessException e) {
		} catch (InvocationTargetException e) {
		}
	}
}
