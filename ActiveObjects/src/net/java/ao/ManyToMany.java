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
 * @author Daniel Spiewak
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ManyToMany {
	
	Class<? extends Entity> value();

	/**
	 * <p>A String clause allowing developer-specified additional
	 * conditions to be imposed on the relationship.  The String 
	 * must be a proper SQL WHERE clause:</p>
	 * 
	 * <code>"deleted = FALSE"</code>
	 * 
	 * <p>One must be extremely careful with this sort of thing though
	 * because sometimes (as is the case with the above sample), the 
	 * unparameterized code may not execute as expected against every
	 * database (due to differences in typing and value handling).  Thus,
	 * in all but non-trivial cases, defined implementations should be used.</p>
	 */
	String where() default "";
}
