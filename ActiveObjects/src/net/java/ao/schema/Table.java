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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Used to specify a table name for an entity explicitly without resorting
 * to manual mappings in the table name converter.  This annotation could
 * be used for entities who's corresponding table names either don't follow
 * the convention, or which must correspond to some pre-existing table in
 * the database.</p>
 * 
 * <pre>@Table("t_person")
 * public interface Person extends Entity {
 *     ...
 * }</pre>
 * 
 * <p>In the above example, the <code>Person</code> entity will correspond to
 * the "t_person" table, rather than "person" or "people" (depending on the
 * table name converter).</p>
 * 
 * @author Daniel Spiewak
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Table {
	
	/**
	 * Contains the actual name name which is being specified.  This table
	 * name will be used without modification, and thus must be a valid
	 * identifier in the underlying database.
	 */
	String value();
}
