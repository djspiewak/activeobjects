
package net.java.ao;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the field to use for version based optimistic locking.
 * If this annotation is present on an entity class, version based optimistic
 * locking will be enforced.
 * The field must be of type {@code int}.
 * @author Ian McDonagh
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Version {
  /** Field name */
	String value() default "version";
  /** Initial value */
  int initial() default -1;
  /** Find objects with initial version */
  boolean findInitial() default false;
  /** Increment value */
  int increment() default 1;
}
