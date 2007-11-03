/*
 * Created on Jul 5, 2007
 */
package net.java.ao.wicket;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.java.ao.RawEntity;

import org.apache.wicket.model.IModel;

/**
 * @author Daniel Spiewak
 */
public abstract class EntityModel<T extends RawEntity<?>> implements IModel, Serializable, IManagerWrapper {
	private Object key;
	private Class<? extends RawEntity<?>> type;
	
	private String property;
	
	public EntityModel(T entity, String property) {
		key = net.java.ao.Common.getPrimaryKeyValue((RawEntity<Object>) entity);
		type = entity.getEntityType();
		
		this.property = property;
	}
	
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

	public void detach() {
		getEntity().save();
	}
	
	public T getEntity() {
		return (T) getEntityManager().get((Class<RawEntity<Object>>) type, key);
	}
}
