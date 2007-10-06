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
 * <p>Marks a specific method (and thus, database field) to contain
 * a generated value on INSERT.  The precise value and generation
 * algorithm is specified by the {@link ValueGenerator} class.
 * This can be used to implement functionality like UUID keys
 * and other values which must be generated in the application
 * layer.</p>
 * 
 * <p>This only generates values for INSERT statements, not
 * UPDATE, SELECT or any such statement.  It is likely that down
 * the road, such functionality will be added.  However, at the
 * moment the designed use-case is something like UUID
 * primary keys, rather than an application-layer ON UPDATE.</p>
 * 
 * @author Daniel Spiewak
 * @see net.java.ao.schema.OnUpdate
 * @see net.java.ao.ValueGenerator
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Generator {
	
	/**
	 * Specifies the generator class to use in creating new values
	 * for the field in question.  This generator should be
	 * thread-safe and execute very quickly.
	 *
	 */
	Class<? extends ValueGenerator<?>> value();
}
