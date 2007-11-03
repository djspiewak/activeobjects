/*
 * Created on Jul 17, 2007
 */
package net.java.ao.wicket;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import net.java.ao.RawEntity;

/**
 * @author Daniel Spiewak
 */
public abstract class EntityList<T extends RawEntity<?>> extends AbstractList<T> implements 
		Serializable, IManagerWrapper {
	private List<Object> delegate;
	private Class<T> type;
	
	public EntityList(Class<T> type) {
		delegate = new ArrayList<Object>();
		this.type = type;
	}
	
	public void clear() {
		delegate.clear();
	}

	public boolean contains(Object o) {
		return delegate.contains(net.java.ao.Common.getPrimaryKeyValue((RawEntity<Object>) o));
	}

	public T get(int index) {
		return (T) getEntityManager().get((Class<RawEntity<Object>>) type, delegate.get(index));
	}

	public int hashCode() {
		return delegate.hashCode();
	}

	public int indexOf(Object o) {
		return delegate.indexOf(net.java.ao.Common.getPrimaryKeyValue((RawEntity<Object>) o));
	}

	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	public T remove(int index) {
		return (T) getEntityManager().get((Class<RawEntity<Object>>) type, delegate.remove(index));
	}

	public boolean remove(Object o) {
		return delegate.remove(net.java.ao.Common.getPrimaryKeyValue((RawEntity<Object>) o));
	}

	@Override
	public int size() {
		return delegate.size();
	}

	@Override
	public boolean add(T e) {
		return delegate.add(net.java.ao.Common.getPrimaryKeyValue((RawEntity<Object>) e));
	}

	@Override
	public void add(int index, T element) {
		delegate.add(index, net.java.ao.Common.getPrimaryKeyValue((RawEntity<Object>) element));
	}

	@Override
	public T set(int index, T element) {
		return (T) getEntityManager().get((Class<RawEntity<Object>>) type, delegate.set(index, 
				net.java.ao.Common.getPrimaryKeyValue((RawEntity<Object>) element)));
	}
}
