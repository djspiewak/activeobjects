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
 * <p>Specifies a value for the field to receive in the event of an UPDATE
 * or INSERT statement in some other field of the corresponding row.  This
 * is most often used to implement a <code>modified</code> field, as it does
 * not require manual re-specification of the update value in every place
 * an UPDATE is performed.</p>
 * 
 * <p>The functionality of this annotation is modeled after the MySQL
 * <code>ON UPDATE</code> field clause.  However, because no other databases
 * implement a similar syntax, triggers are often used as the implementation
 * mechanism.  Ironically, this means that most databases have a more
 * flexible implementation of the functionality than MySQL.  ActiveObjects
 * performs no intrinsic checking to enforce limitations on the annotation's
 * usage (for example, MySQL requires only one field with DEFAULT or
 * ON UPDATE value equaling CURRENT_TIMESTAMP).  This sort of checking is
 * left entirely up to the database against which the migration is running.</p>
 * 
 * <p>Currently, this annotation is unsupported on HSQLDB due to the way it
 * handles triggers.  However, there is no hard limit actually <i>preventing</i>
 * the functionality.</p>
 * 
 * <p>This annotation is only relevant to migrations.  It has no effect upon
 * "runtime" entity usage.</p>
 * 
 * @author Daniel Spiewak
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OnUpdate {
	
	/**
	 * Contains the database-agnostic representation of the default value
	 * for the corresponding field.  The conversion between this
	 * <code>String</code> and the appropriate value is handled by the
	 * {@link net.java.ao.types.DatabaseType#defaultParseValue(String)}
	 * method.  Thus, even default values for custom types are supported.
	 */
	String value();
}
