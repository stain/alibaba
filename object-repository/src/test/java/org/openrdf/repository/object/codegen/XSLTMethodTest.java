package org.openrdf.repository.object.codegen;

import java.lang.reflect.Method;
import java.net.URL;

import org.openrdf.model.URI;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.base.CodeGenTestCase;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.memory.MemoryStore;

public class XSLTMethodTest extends CodeGenTestCase {
	public void test() throws Exception {
		addRdfSource("/ontologies/rdfs-schema.rdf");
		addRdfSource("/ontologies/owl-schema.rdf");
		addRdfSource("/ontologies/object-ontology.owl");
		addRdfSource("/ontologies/xslt-ontology.ttl");
		ObjectRepositoryFactory ofm = new ObjectRepositoryFactory();
		ObjectRepository repo = ofm.getRepository(converter);
		repo.setDelegate(new SailRepository(new MemoryStore()));
		repo.setDataDir(targetDir);
		repo.initialize();
		ObjectConnection con = repo.getConnection();
		try {
			URL url = find("/ontologies/xslt-ontology.ttl");
			con.add(url, url.toExternalForm(), RDFFormat.TURTLE);
			URI Entity = con.getValueFactory().createURI("urn:test:xsl#Entity");
			Object entity = con.getObject("urn:test:entity");
			entity = con.addDesignations(entity, Entity);
			Method testMethod = entity.getClass().getMethod("test");
			testMethod.invoke(entity);
		} finally {
			con.close();
		}
	}
}
