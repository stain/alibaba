package org.openrdf.http.object;

import org.openrdf.http.object.base.MetadataServerTestCase;
import org.openrdf.http.object.behaviours.DescribeSupport;
import org.openrdf.model.Model;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.object.ObjectConnection;

public class DivertTest extends MetadataServerTestCase {

	private ObjectConnection con;

	public void setUp() throws Exception {
		config.addBehaviour(DescribeSupport.class, RDFS.RESOURCE);
		super.setUp();
		con = repository.getConnection();
	}

	@Override
	public void tearDown() throws Exception {
		con.close();
		super.tearDown();
	}

	public HTTPObjectServer createServer() throws Exception {
		HTTPObjectServer server = super.createServer();
		server.setIdentityPathPrefix("/absolute;");
		return server;
	}

	public void test() throws Exception {
		URIImpl subj = new URIImpl("urn:test:annotation");
		con.add(subj, RDF.TYPE, OWL.ANNOTATIONPROPERTY);
		Model model = client.path("/absolute;urn:test:annotation").queryParam("describe", "").get(Model.class);
		assertTrue(model.contains(subj, RDF.TYPE, OWL.ANNOTATIONPROPERTY));
	}
}
