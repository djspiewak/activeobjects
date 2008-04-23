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

import net.java.ao.Searchable;

/**
 * <p>Marks the corresponding database field as requiring database indexing.
 * This is different from the full-text search capabilities provided by Lucene
 * in that the indexing is actually handled within the database itself.  For
 * full-text search configuration, see the {@link Searchable} annotation.</p>
 * 
 * <p>From a design standpoint, this annotation should be used quite sparingly.
 * Any foreign key in an entity is already indexed (as enforced by AO
 * migrations).  Thus, use should be limited to fields against which queries
 * will often be run (e.g. <code>people.last_name</code> and so on).</p>
 * 
 * <p>This annotation is only relevant to migrations.  It has no effect upon
 * "runtime" entity usage.</p>
 * 
 * @author Daniel Spiewak
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Indexed {}
