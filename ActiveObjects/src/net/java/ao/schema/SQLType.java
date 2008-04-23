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
 * <p>Explicitly specifies the underlying database field type for the
 * corresponding field to the method in question.  This should be
 * used for use-cases where the default corresponding type for a
 * particular Java return type is inappropriate for the situation.
 * For example:</p>
 * 
 * <pre>public interface Book extends Entity {
 *     public String getTitle();
 *     public void setTitle(String title);
 *     
 *     &#064;SQLType(Types.CLOB)
 *     public String getText();
 *     &#064;SQLType(Types.CLOB)
 *     public void setText(String text);
 * }</pre>
 * 
 * <p>This annotation can also be used to specify precision and scale
 * for the underlying type.  Thus, this annotation is a single-point,
 * one-shot mechanism for controlling the type used for a specific
 * field.</p>
 * 
 * @author Daniel Spiewak
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SQLType {
	
	/**
	 * Specifies the actual SQL type integer which will be used to
	 * represent the corresponding field in the database.  The type
	 * integer is defined by the {@link java.sql.Types} constants
	 * list.  If unspecified, type will be whatever the default is
	 * for a method of the return type in question.
	 */
	int value() default -1;
	
	/**
	 * Specifies the precision of the SQL type in the underlying field.
	 * If unspecified, the default precision will be used.
	 */
	int precision() default -1;
	
	/**
	 * Specifies the scale of the SQL type in the underlying field.
	 * If unspecified, the default scale will be used.
	 */
	int scale() default -1;
}
