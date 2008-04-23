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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Specifies a list of fields which should be appended to the
 * SELECT clause every time a row of the type in question is 
 * retrieved.  If the developer knows that every time an entity of
 * a certain type is retrieved, certain fields will be accessed, this is
 * a prime candidate for preloading.  For example:</p>
 * 
 * <pre>@Preload("name")
 * public interface Person extends Entity {
 *     public String getName();
 *     public void setName(String name);
 *     
 *     // ...
 * }
 * 
 * // ...
 * manager.find(Person.class, "age &gt; 12");</pre>
 * 
 * <p>This code will run a query something like the following:</p>
 * 
 * <code>SELECT id,name FROM people WHERE age &gt; 12</code>
 * 
 * <p>A list of fields may also be specified:</p>
 * 
 * <pre>@Preload({"firstName", "lastName"})
 * public interface Person extends Entity {
 *     public String getFirstName();
 *     public void setFirstName(String firstName);
 *     
 *     public String getLastName();
 *     public void setLastName(String lastName);
 *     
 *     // ...
 * }
 * 
 * // ...
 * manager.find(Person.class, "age &gt; 12");</pre>
 * 
 * <p>This produces a query like the following:</p>
 * 
 * <code>SELECT id,firstName,lastName FROM people WHERE age &gt; 12</code>
 * 
 * <p>* may also be specified to force queries to load <i>all</i> 
 * fields.  As such, <code>@Preload</code> is the primary mechanism
 * provided by ActiveObjects to override its lazy-loading underpinnings.</p>
 * 
 * <p><b>This flag is a hint.</b>  There are still queries (such as those
 * executed by {@link EntityManager#findWithSQL(Class, String, String, Object...)})
 * which will ignore the <code>@Preload</code> values and simply
 * execute a vanilla query.</p>
 * 
 * @author Daniel Spiewak
 * @see #value()
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Preload {
	
	/**
	 * Contains the list of fields to be preloaded.  This can be a single field (e.g. 
	 * "name", "*", etc) or an array of fields (e.g. {"firstName", "lastName"}).
	 */
	String[] value() default {"*"};
}
