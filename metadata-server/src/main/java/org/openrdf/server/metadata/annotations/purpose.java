package org.openrdf.server.metadata.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.openrdf.repository.object.annotations.rdf;

@rdf("http://www.openrdf.org/rdf/2009/meta#purpose")
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.METHOD, ElementType.TYPE })
public @interface purpose {
	String[] value();
}
