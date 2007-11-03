/*
 * Created on Jul 17, 2007
 */
package net.java.ao.wicket;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import net.java.ao.Entity;
import net.java.ao.EntityManager;

/**
 * @author Daniel Spiewak
 */
public abstract class EntityList<T extends Entity> extends AbstractList<T> implements Serializable {
	private List<Integer> delegate;
	private Class<T> type;
	
	public EntityList(Class<T> type) {
		delegate = new ArrayList<Integer>();
		this.type = type;
	}
	
	public abstract EntityManager getEntityManager();

	public void clear() {
		delegate.clear();
	}

	public boolean contains(Object o) {
		return delegate.contains(((Entity) o).getID());
	}

	public T get(int index) {
		return getEntityManager().get(type, delegate.get(index));		// TODO	implement
	}

	public int hashCode() {
		return delegate.hashCode();
	}

	public int indexOf(Object o) {
		return delegate.indexOf(((Entity) o).getID());
	}

	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	public T remove(int index) {
		return getEntityManager().get(type, delegate.remove(index));		// TODO	implement
	}

	public boolean remove(Object o) {
		return delegate.remove((Integer) ((Entity) o).getID());
	}

	@Override
	public int size() {
		return delegate.size();
	}

	@Override
	public boolean add(T e) {
		return delegate.add(e.getID());
	}

	@Override
	public void add(int index, T element) {
		delegate.add(index, element.getID());
	}

	@Override
	public T set(int index, T element) {
		return getEntityManager().get(type, delegate.set(index, ((Entity) element).getID()));
	}
}
