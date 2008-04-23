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
 * <p>Marks a method as to be ignored by the schema generation engine.  This
 * annotation has no effect on the normal "runtime" behavior of the method
 * or its containing entity.</p>
 * 
 * <p>As a general rule, it's a good idea to use this annotation on any method
 * which does not have a one-to-one correspondance with a database field.  For
 * example, utility methods within the entity which perform high-level operations
 * upon the data rather than acting as a simple bean interface to a field.  Not
 * using this annotation in such a case is perfectly acceptable and will not
 * lead to any problems in the actual invocation.  However, if a migration is run
 * against an entity containing such a method, extraneous fields may be generated
 * and the migration may even fail completely.</p>
 * 
 * <p>This annotation is only relevant to migrations.  It has no effect upon
 * "runtime" entity usage.</p>
 * 
 * @author Daniel Spiewak
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Ignore {}
