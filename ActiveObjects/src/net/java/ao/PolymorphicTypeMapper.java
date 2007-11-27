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

import net.java.ao.schema.FieldNameConverter;
import net.java.ao.schema.TableNameConverter;

/**
 * <p>Represents a strategy for bi-directional mapping between entity types and
 * their corresponding polymorphic type flag value (if any).  The mappings
 * represented by this strategy will usually be similar, though not
 * necessarily identical to the values returned by the mono-directional
 * mapping of entity type to table names ({@link TableNameConverter}).  A
 * passable description of polymorphic type flags and relationships is
 * available in the javadoc for the {@link FieldNameConverter} interface.</p>
 * 
 * <p>An example set of mappings follows (assuming these are all polymorphic
 * types):</p>
 * 
 * <table border="1">
 * 		<tr>
 * 			<td><b>Classname</b></td>
 * 			<td><b>Polymorphic Type Flag</b></td>
 * 		</tr>
 * 
 * 		<tr>
 * 			<td>com.company.db.Employee</td>
 * 			<td>employee</td>
 * 		</tr>
 * 
 * 		<tr>
 * 			<td>com.company.db.Manager</td>
 * 			<td>manager</td>
 * 		</tr>
 * 
 * 		<tr>
 * 			<td>com.company.db.CEO</td>
 * 			<td>ceo</td>
 * 		</tr>
 * </table>
 * 
 * <p>Mappings need not be entirely unique, as long as the mappings for all
 * subtypes of a specific supertype are internally unique.  For example, the
 * above example implies that all three entities are subtypes of a single
 * supertype (probably <code>Person</code>).  Thus, all of the mapping values
 * must be uniquely paired.  However, a fourth entity could just as easily be
 * represented which extends an entirely separate supertype.  Such an entity
 * would not be constrained to uniqueness with the other, unrelated entities.
 * However, if the hierarchy for a single supertype does not have fully-defined
 * and unique mappings, unexpected behavior may result (such as retrieving
 * references to invalid entities).</p>
 * 
 * <p>A sane implementation (using manually-specified mappings) is implemented
 * within the {@link DefaultPolymorphicTypeMapper} class.  Very few use-cases
 * call for a custom implementation of this interface directly.</p>
 * 
 * @author Daniel Spiewak
 */
public interface PolymorphicTypeMapper {
	
	/**
	 * Retrieves the polymorphic type flag value which corresponds to the
	 * specified type.  Return value must be repeatable given the same type
	 * as well as uniquely defined within the hierarchy of the given type.
	 * 
	 * @param type	The type for which a polymorphic flag must be generated.
	 * @return	The polymorphic flag type value which corresponds to the given type.
	 * @see #invert(Class, String)
	 */
	public String convert(Class<? extends RawEntity<?>> type);
	
	/**
	 * Retrieves the entity type which corresponds to the given polymorphic type
	 * flag value as a subtype of the specified parent entity type.  Logically
	 * the inverse of the {@link #convert(Class)} method.
	 * 
	 * @param parent	The parent interface of the type which must be retrieved.
	 * @param type	The polymorphic type flag value which corresponds to the type
	 * 		which must be retrieved.
	 * @return	The entity type corresponding uniquely to the supertype-flag pair.
	 */
	public Class<? extends RawEntity<?>> invert(Class<? extends RawEntity<?>> parent, String type);
}
