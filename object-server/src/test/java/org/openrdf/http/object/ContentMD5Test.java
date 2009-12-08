package org.openrdf.http.object;

import org.openrdf.http.object.annotations.operation;
import org.openrdf.http.object.annotations.type;
import org.openrdf.http.object.base.MetadataServerTestCase;
import org.openrdf.http.object.behaviours.PUTSupport;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.object.annotations.iri;

import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

public class ContentMD5Test extends MetadataServerTestCase {

	@iri(RDFS.NAMESPACE + "Resource")
	public interface Resource {
		@operation("property")
		@type("text/plain")
		@iri("urn:test:property")
		String getProperty();

		@operation("property")
		void setProperty(String property);
	}

	public void setUp() throws Exception {
		config.addConcept(Resource.class);
		config.addBehaviour(PUTSupport.class);
		super.setUp();
	}

	protected void addContentEncoding(WebResource client) {
		// no encoding
	}

	public void testGoodMD5() throws Exception {
		WebResource web = client.path("/");
		web.header("Content-MD5", "nwqq6b6ua/tTDk7B5M184w==").put(
				"Check Integrity!");
	}

	public void testBadMD5() throws Exception {
		try {
			WebResource web = client.path("/");
			web.header("Content-MD5", "WRH4uNnoR4EfM9mheXUtIA==").put(
					"Check Integrity!");
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(400, e.getResponse().getStatus());
		}
	}

	public void testGoodMD5Property() throws Exception {
		WebResource web = client.path("/").queryParam("property", "");
		web.header("Content-MD5", "nwqq6b6ua/tTDk7B5M184w==").put(
				"Check Integrity!");
	}

	public void testBadMD5Property() throws Exception {
		try {
			WebResource web = client.path("/").queryParam("property", "");
			web.header("Content-MD5", "WRH4uNnoR4EfM9mheXUtIA==").put(
					"Check Integrity!");
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(400, e.getResponse().getStatus());
		}
	}
}