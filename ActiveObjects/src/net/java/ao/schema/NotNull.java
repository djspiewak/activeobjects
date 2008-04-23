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
 * <p>Marks a specific field as to be constrained to non-NULL values within
 * the database.  This functionality is handled through the schema indicating
 * to the database that the constraint should be enforced.  ActiveObjects
 * itself does no checking to ensure that the value of the field can only
 * be non-NULL.  For most databases, this means that the field corresponding
 * to the method in question will be defined with the <code>NOT NULL</code>
 * clause.</p>
 * 
 * <p>This annotation is only relevant to migrations.  It has no effect upon
 * "runtime" entity usage.</p>
 * 
 * @author Daniel Spiewak
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface NotNull {}
