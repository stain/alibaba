package org.openrdf.server.metadata.behaviours;

import org.openrdf.repository.object.RDFObject;
import org.openrdf.server.metadata.annotations.purpose;

public abstract class DescribeSupport implements RDFObject {

	@purpose("describe")
	public RDFObject metaDescribe() {
		return this;
	}
}
