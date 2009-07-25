package org.openrdf.repository.object.codegen;

import java.io.File;

import org.apache.commons.codec.binary.Base64;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.base.CodeGenTestCase;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.repository.object.managers.LiteralManager;
import org.openrdf.repository.object.managers.Marshall;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

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
			throws Exception {

		ObjectRepositoryFactory ofm = new ObjectRepositoryFactory() {
			@Override
			protected LiteralManager createLiteralManager(ValueFactory uf, ValueFactory lf) {
				LiteralManager literals = super.createLiteralManager(uf, lf);
				// record additional Marshal for Base64 encoded byte arrays
				ByteArrayMarshall marshall = new ByteArrayMarshall(lf);
				literals.recordMarshall(byte[].class, marshall);
				URI type = marshall.getDatatype();
				literals.addDatatype(byte[].class, type);
				return literals;
			}
		};
		ObjectRepository repo = ofm.getRepository(converter);
		repo.setDelegate(new SailRepository(new MemoryStore()));
		repo.setDataDir(targetDir);
		repo.initialize();
		return getConceptJar(targetDir);
	};

	public static class ByteArrayMarshall implements Marshall<byte[]> {
		private ValueFactory vf;

		public ByteArrayMarshall(ValueFactory vf) {
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
