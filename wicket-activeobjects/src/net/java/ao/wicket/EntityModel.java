/*
 * Created on Jul 5, 2007
 */
package net.java.ao.wicket;

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
	private Class<? extends Entity> type;
	
	private String property;
	
	public EntityModel(Entity entity, String property) {
		id = entity.getID();
		type = (Class<? extends Entity>) entity.getEntityType();
		
		this.property = property;
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

	public void detach() {
	}
	
	public Entity getEntity() {
		return getEntityManager().get(type, id);
	}
}
