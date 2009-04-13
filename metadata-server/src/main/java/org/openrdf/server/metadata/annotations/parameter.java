package org.openrdf.server.metadata.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.openrdf.repository.object.annotations.rdf;

@rdf("http://www.openrdf.org/rdf/2009/meta#parameter")
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.PARAMETER })
public @interface parameter {
	String[] value() default {};
}
