package org.openrdf.repository.object.codegen;

import java.io.File;

import org.apache.commons.codec.binary.Base64;
import org.openrdf.model.Literal;
import org.openrdf.model.LiteralFactory;
import org.openrdf.model.URI;
import org.openrdf.model.URIFactory;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.base.CodeGenTestCase;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.repository.object.managers.LiteralManager;
import org.openrdf.repository.object.managers.Marshall;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.store.StoreConfigException;
import org.openrdf.store.StoreException;

public class Base64BinaryTest extends CodeGenTestCase {

	public void testOneOf() throws Exception {
		addRdfSource("/ontologies/xsd-datatypes.rdf");
		addRdfSource("/ontologies/rdfs-schema.rdf");
		addRdfSource("/ontologies/owl-schema.rdf");
		addRdfSource("/ontologies/binary-ontology.owl");
		createJar("binary.jar");
	}

	@Override
	protected File createJar(String filename)
			throws StoreConfigException, StoreException {

		ObjectRepositoryFactory ofm = new ObjectRepositoryFactory() {
			@Override
			protected LiteralManager createLiteralManager(URIFactory uf, LiteralFactory lf) {
				LiteralManager literals = super.createLiteralManager(uf, lf);
				// record additional Marshal for Base64 encoded byte arrays
				ByteArrayMarshall marshall = new ByteArrayMarshall(lf);
				literals.recordMarshall(byte[].class, marshall);
				URI type = marshall.getDatatype();
				literals.addDatatype(byte[].class, type);
				return literals;
			}
		};
		File jar = new File(targetDir, "codegen.jar");
		ObjectRepository repo = ofm.createRepository(converter, new SailRepository(new MemoryStore()));
		repo.setConceptJar(jar);
		repo.initialize();
		return jar;
	};

	public static class ByteArrayMarshall implements Marshall<byte[]> {
		private LiteralFactory vf;

		public ByteArrayMarshall(LiteralFactory vf) {
			this.vf = vf;
		}

		public byte[] deserialize(Literal literal) {
			return Base64.decodeBase64(literal.stringValue().getBytes());
		}

		public URI getDatatype() {
			return XMLSchema.BASE64BINARY;
		}

		public String getJavaClassName() {
			return byte[].class.getName();
		}

		public Literal serialize(byte[] data) {
			return vf.createLiteral(new String(Base64.encodeBase64(data)));
		}

		public void setDatatype(URI datatype) {
			if (!datatype.equals(XMLSchema.BASE64BINARY)) {
				throw new IllegalArgumentException(datatype.toString());
			}
		}
	}

}
