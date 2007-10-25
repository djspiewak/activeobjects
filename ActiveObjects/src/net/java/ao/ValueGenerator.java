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

/**
 * <p>A super-interface designed to allow auto-generated values
 * to be inserted into database fields on INSERT.  Potentially,
 * this could be expanded to also include on UPDATE
 * functionality, but for the moment it only handles on entity
 * creation (INSERT).  Implementations are assumed to be
 * completely stateless and thus thread-safe.</p>
 * 
 * <p>The type parameter represents the return type of the
 * method which does the actual generation.</p>
 * 
 * @author Daniel Spiewak
 * @see net.java.ao.Generator
 */
public interface ValueGenerator<T> {
	
	/**
	 * Generate a new value for an arbitrary field.  No write
	 * database operations should be performed within this
	 * method.  The <code>EntityManager</code> instance is only
	 * intended to ensure various field constraints (such as
	 * uniqueness for primary keys).  Great care should be
	 * taken with performing such operations, as the value
	 * generation method needs to be extremely efficient.
	 * 
	 * @param manager	An instance to gain <i>read</i> access to the database.
	 * @return A new value of the relevant type for INSERT.
	 */
	public T generateValue(EntityManager manager);
}
