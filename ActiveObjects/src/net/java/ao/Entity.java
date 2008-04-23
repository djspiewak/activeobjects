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

import net.java.ao.schema.AutoIncrement;
import net.java.ao.schema.FieldNameConverter;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.PrimaryKey;
import net.java.ao.schema.TableNameConverter;

/**
 * <p>A specific extension of {@link RawEntity} providing the {@link #getID()}
 * method.  This interface should be sufficient for most entity needs.  Only
 * in cases where custom primary keys are required should <code>RawEntity</code>
 * be directly extended.</p>
 * 
 * <p><code>Entity</code> is the superinterface for most entity interfaces.  For
 * example, the following code defines an entity which maps to the "company"
 * table (depending on what {@link TableNameConverter} is in use) with fields
 * "name", "publiclyTraded" and "id" (names depending on the {@link FieldNameConverter}
 * in use):</p>
 * 
 * <pre>public class Company extends Entity {
 *     public String getName();
 *     public void setName(String name);
 *     
 *     public boolean isPubliclyTraded();
 *     public void setPubliclyTraded(boolean publiclyTraded);
 * }</pre>
 * 
 * @author Daniel Spiewak
 * @see net.java.ao.RawEntity
 */
public interface Entity extends RawEntity<Integer> {
	
	/**
	 * <p>Retrieves the value of the "id" field in the entity; that is, the primary key.  As
	 * this is the primary key, no database query will correspond with this method
	 * invocation.</p>
	 * 
	 * <p>For the moment, primary key fields are immutable.  However, this will
	 * change before the 1.0 release as there are numerous use-cases for changing
	 * the value of a row's primary key field.</p>
	 * 
	 * @return The value of the primary key for the row, the "id" field.
	 */
	@AutoIncrement
	@NotNull
	@PrimaryKey("id")
	public int getID();
}
