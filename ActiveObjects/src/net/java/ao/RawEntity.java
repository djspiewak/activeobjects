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

import java.beans.PropertyChangeEvent;
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

	/**
	 * Retrieves the {@link EntityManager} instance which manages this
	 * entity.  This method can be used by defined implementations to
	 * get the relevant <code>EntityManager</code> instance in order
	 * to run custom queries.
	 * 
	 * @return The instance which manages this entity.
	 */
	@Ignore public EntityManager getEntityManager();
	
	/**
	 * <p>Returns the actual {@link Class} instance which corresponds to the
	 * original entity interface.  This is necessary because calling {@link Object#getClass()}
	 * on a proxy instance doesn't return the value one would expect.  As
	 * such, <code>RawEntity</code> provides this method to give
	 * developers access to the originating entity interface.  Example:</p>
	 * 
	 * <pre>public interface Person extends Entity {
	 *     // ...
	 * }
	 * 
	 * // ...
	 * Person p = manager.get(Person.class, 2);
	 * p.getEntityType();         // returns Person.class
	 * p.getClass();           // indeterminate return value, probably something like $Proxy26.class</pre>
	 * 
	 * @return The {@link Class} which defines the entity in question.
	 */
	@Ignore public Class<? extends RawEntity<T>> getEntityType();
	
	/**
	 * <p>Adds a property change listener to the entity.  This method is included
	 * partly to emphasize compliance with the bean spec (sort of), but
	 * more to enable developers to register listeners on the setters.  This
	 * method is called when the setter is called, <i>not</i> when {@link #save()}
	 * is invoked.</p>
	 * 
	 * <p>Be aware that the {@link PropertyChangeEvent} may or
	 * may not have a valid <code>oldValue</code>.  This is because
	 * retrieving a previous value under many circumstances may result in an
	 * extra database SELECT.  If no <code>oldValue</code> is available in
	 * memory, <code>null</code> will be passed.</p>
	 * 
	 * <p>Any trivial custom code which only needs to run on mutator invocation
	 * should use property change listeners, rather than a defined implementation.</p>
	 * 
	 * @param listener	The change listener to add.
	 */
	@Ignore public void addPropertyChangeListener(PropertyChangeListener listener);
	
	/**
	 * Removes a property change listener from the entity.
	 * 
	 * @param listener	The change listener to remove.
	 * @see #addPropertyChangeListener(PropertyChangeListener)
	 */
	@Ignore public void removePropertyChangeListener(PropertyChangeListener listener);
}
