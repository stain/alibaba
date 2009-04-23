package org.openrdf.server.metadata.behaviours;

import org.openrdf.repository.object.RDFObject;
import org.openrdf.server.metadata.annotations.operation;
import org.openrdf.server.metadata.annotations.rel;
import org.openrdf.server.metadata.annotations.title;
import org.openrdf.server.metadata.annotations.type;

public abstract class DescribeSupport implements RDFObject {

	@title("RDF Metadata")
	@rel("describedby")
	@operation("describe")
	@type( { "application/rdf+xml", "application/x-turtle", "text/rdf+n3",
		"application/trix", "application/x-trig" })
	public RDFObject metaDescribe() {
		return this;
	}
}
