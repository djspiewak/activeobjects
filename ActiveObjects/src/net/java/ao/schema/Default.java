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
 * Specifies a default value for the database field corresponding to
 * the tagged method.  This default value will be used at INSERT,
 * usually automatically by the database.  If the database does not
 * provide a DEFAULT modifier for fields, the value will be
 * injected into any INSERT statement, ensuring it is indeed the default
 * value for the field in question.
 * 
 * @author Daniel Spiewak
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Default {
	
	/**
	 * Contains the database-agnostic representation of the default value
	 * for the corresponding field.  The conversion between this
	 * <code>String</code> and the appropriate value is handled by the
	 * {@link net.java.ao.types.DatabaseType#defaultParseValue(String)}
	 * method.  Thus, even default values for custom types are supported.
	 */
	String value();
}
