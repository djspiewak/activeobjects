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
package net.java.ao.schema;

import net.java.ao.EntityManager;
import net.java.ao.RawEntity;

/**
 * <p>Superinterface to all table name converters; designed to impose conventions
 * upon the auto-conversion of class names to database tables.  The idea
 * behind this is to allow user-specified table name conventions and standards
 * rather than always enforcing ActiveObjects's idea of "good naming".</p>
 * 
 * <p>Every {@link EntityManager} contains a single table name converter which
 * the entire library uses when performing operations.  Any third-party code which
 * interacts with tables directly should make use of this faculty.  By using
 * the table name converter even in ORM-external operations, the developers can
 * control table names in a single location and ensure that the conventions
 * need only be maintained in one spot.</p>
 * 
 * <p>Most new implementations of table name converters should extend 
 * {@link AbstractTableNameConverter} rather than implementing this interface
 * directly.  This allows third-party converters to take advantage of boiler-plate
 * conversion code which would otherwise have to be duplicated in every converter.</p>
 * 
 * @author Daniel Spiewak
 */
public interface TableNameConverter {
	
	/**
	 * Generates a table name to correspond with the specified class. The
	 * algorithm used must check for the existance of the {@link Table}
	 * annotation and use the appropriate override when necessary.  If this
	 * check is not made, ActiveObjects will continue to function normally,
	 * but any code assuming the proper imlementation of <code>@Table</code>
	 * will likely fail.
	 * 
	 * @param clazz	The entity type for which a corresponding field name must be
	 * 		generated.
	 * @return	A database table name which corresponds to the given entity type.
	 */
	public String getName(Class<? extends RawEntity<?>> clazz);
}
