package org.jrivets.util.testing;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * The Example annotation indicates that a method or class contains some example
 * or use-case. The annotation is used for declarative purposes only.
 * 
 * @author Dmitry Spasibenko
 * 
 */
@Retention(SOURCE)
@Target({ METHOD, TYPE })
public @interface Example {
    String value();
}
