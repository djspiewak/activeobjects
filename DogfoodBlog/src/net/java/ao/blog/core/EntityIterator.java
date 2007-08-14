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

import java.sql.SQLException;
import java.util.Iterator;

import net.java.ao.Entity;
import net.java.ao.EntityManager;
import net.java.ao.Query;

public abstract class EntityIterator<T extends Entity> implements Iterator<T> {

	private static class EntityAllIterator<T extends Entity> extends EntityIterator<T> {

		public EntityAllIterator(EntityManager entityManager, Class<T> type) {
			super(entityManager, type);
		}

		@Override
		protected T[] getResults() {
			try {
				return entityManager.find(type);
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static class EntityCriteriaIterator<T extends Entity> extends EntityIterator<T> {

		private final String criteria;

		private final Object[] parameters;

		public EntityCriteriaIterator(EntityManager entityManager, Class<T> type, String criteria, Object... parameters) {
			super(entityManager, type);
			this.criteria = criteria;
			this.parameters = parameters;
		}

		@Override
		protected T[] getResults() {
			try {
				return entityManager.find(type, criteria, parameters);
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static class EntityQueryIterator<T extends Entity> extends EntityIterator<T> {

		private final Query query;

		public EntityQueryIterator(EntityManager entityManager, Class<T> type, Query query) {
			super(entityManager, type);
			this.query = query;
		}

		@Override
		protected T[] getResults() {
			try {
				return entityManager.find(type, query);
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static <T extends Entity> EntityIterator<T> forAll(EntityManager manager, Class<T> type) {
		return new EntityAllIterator<T>(manager, type);
	}

	public static <T extends Entity> EntityIterator<T> forCriteria(EntityManager manager, Class<T> type, String criteria, Object... parameters) {
		return new EntityCriteriaIterator<T>(manager, type, criteria, parameters);
	}

	public static <T extends Entity> EntityIterator<T> forQuery(EntityManager manager, Class<T> type, Query query) {
		return new EntityQueryIterator<T>(manager, type, query);
	}

	private int counter = 0;

	private T[] results = null;

	protected final EntityManager entityManager;

	protected final Class<T> type;

	public EntityIterator(EntityManager entityManager, Class<T> type) {
		this.entityManager = entityManager;
		this.type = type;
	}

	public boolean hasNext() {
		return counter < results().length;
	}

	public T next() {
		return results()[counter++];
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	protected abstract T[] getResults();

	protected T[] results() {
		if (results == null) {
			results = getResults();
		}
		if (results == null) {
			throw new IllegalStateException();
		}
		return results;
	}
}
