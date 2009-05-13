package org.openrdf.server.metadata.behaviours;

import javax.interceptor.InvocationContext;
import javax.ws.rs.core.MediaType;

import org.openrdf.model.ValueFactory;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.annotations.intercepts;
import org.openrdf.server.metadata.concepts.WebResource;

public abstract class MediaTypeSupport implements WebResource {

	@intercepts
	public void setMediaType(InvocationContext ctx) throws Exception {
		ObjectConnection con = getObjectConnection();
		ValueFactory vf = con.getValueFactory();
		String previous = getMediaType();
		String next = (String) ctx.getParameters()[0];
		ctx.proceed();
		if (previous != null) {
			MediaType m = MediaType.valueOf(previous);
			String type = m.getType() + "/" + m.getSubtype();
			con.removeDesignations(this, vf.createURI("urn:mimetype:" + type));
		}
		if (next != null) {
			MediaType m = MediaType.valueOf(next);
			String type = m.getType() + "/" + m.getSubtype();
			con.addDesignations(this, vf.createURI("urn:mimetype:" + type));
		}
	}

}
