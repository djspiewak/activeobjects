/*
 * Copyright Teachscape
 */
package net.java.ao.blog.core;

import java.io.Serializable;
import java.util.Iterator;

import org.apache.wicket.markup.repeater.util.ModelIteratorAdapter;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * Convenience implementation of {@link ModelIteratorAdapter} that wraps items
 * in a simple {@link Model}.
 */
public class DefaultModelIteratorAdaptor extends ModelIteratorAdapter {

	@SuppressWarnings("unchecked")
	public DefaultModelIteratorAdaptor(Iterator delegate) {
		super(delegate);
	}

	@Override
	protected IModel model(Object object) {
		return new Model((Serializable) object);
	}
}
