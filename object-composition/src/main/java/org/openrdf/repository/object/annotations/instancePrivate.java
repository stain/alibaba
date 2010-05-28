package org.openrdf.repository.object.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates this method or methods defined in this class should not be proxied
 * from other objects.
 * 
 * @author James Leigh
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.TYPE, ElementType.METHOD })
public @interface instancePrivate {

}
