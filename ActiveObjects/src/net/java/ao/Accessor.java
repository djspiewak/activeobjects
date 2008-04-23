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
 * <p>Used to mark a particular field as an accessor.  This allows
 * two things:</p>
 * 
 * <ul>
 * <li>Non-compliance with the get/set convention</li>
 * <li>Overriding of the auto-generated field name (e.g. a method named
 * "getURI" would generate (by default) a field name of "uRI".  Most likely,
 * the desired field name was in fact "uri".  This annotation allows such
 * field names</li>
 * </ul>
 * 
 * @author Daniel Spiewak
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Accessor {
	
	/**
	 * The name of the field for which this method is an accessor.  This
	 * will override any automatically generated field name.
	 */
	String value();
}
