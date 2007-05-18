/*
 * Created on May 3, 2007
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
	String table() default "";
	
	String relateSelf() default "";
	String relateOther() default "";
}
