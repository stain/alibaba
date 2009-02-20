package org.openrdf.elmo.codegen;

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.openrdf.elmo.sesame.SesameLiteralManager;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.util.RDFInserter;
import org.openrdf.sail.memory.MemoryStore;

public class OwlGeneratorTest extends TestCase {
	public static class Entity {
		private boolean b;
		private Class c;
		public boolean isB() {
			return b;
		}
		public void setB(boolean b) {
			this.b = b;
		}
		public Class getC() {
			return c;
		}
		public void setC(Class c) {
			this.c = c;
		}
	}

	public void testPrimitives() throws Exception {
		Repository repo = new SailRepository(new MemoryStore());
		repo.initialize();
		ValueFactory vf = repo.getValueFactory();
		SesameLiteralManager lm = new SesameLiteralManager(vf);
		lm.setClassLoader(getClass().getClassLoader());
		RepositoryConnection con = repo.getConnection();
		OwlGenerator og = new OwlGenerator();
		og.setLiteralManager(lm);
		List beans = Arrays.asList(Entity.class);
		og.exportOntology(beans, new RDFInserter(con));
		//con.export(new RDFXMLWriter(System.out));
		URI p = vf.createURI("java:" + Entity.class.getName() + "#b");
		assertTrue(con.hasStatement(p, RDFS.RANGE, XMLSchema.BOOLEAN, false));
	}

}
