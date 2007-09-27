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
package net.java.ao;

import java.beans.PropertyChangeListener;

import net.java.ao.schema.Ignore;

/**
 * <p>The superinterface for all entities, regardless of primary key.  Developers
 * may choose to extend this interface rather than {@link Entity} if they
 * need to specify a custom primary key field.  As this interface is the most
 * generic superinterface in the entity hierarchy, it defines most of the 
 * common entity methods, such as {@link #save()} and 
 * {@link #addPropertyChangeListener(PropertyChangeListener)}.</p>
 * 
 * <p>This interface is parameterized for the sake of typechecking in the
 * {@link EntityManager#find(Class, String, Object...)} method.  The generic
 * type specified when inheriting this interface should be the same as the
 * return type of the primary key method.  Unfortunately, this is unenforcable,
 * so it is left up to the developers to ensure the spec is followed.</p>
 * 
 * @author Daniel Spiewak
 */
public interface RawEntity<T> {
	
	/**
	 * Called when the entity instance is created.  This method is defined
	 * here in order to provide a common interface through an existing
	 * mechanism.  It is designed to be implemented within an entity
	 * defined implementation.  Any calls to this method will be ignored
	 * by AO unless a defined implementation is present.
	 */
	@Ignore public void init();
	
	/**
	 * Saves all changed (dirty) fields within the entity to the database.  This
	 * method should almost never be overridden within a defined
	 * implementation.  However, it is possible to do so if absolutely
	 * necessary.
	 */
	@Ignore public void save();

	@Ignore public String getTableName();
	@Ignore public EntityManager getEntityManager();
	@Ignore public Class<? extends RawEntity<T>> getEntityType();
	
	@Ignore public void addPropertyChangeListener(PropertyChangeListener listener);
	@Ignore public void removePropertyChangeListener(PropertyChangeListener listener);
}
