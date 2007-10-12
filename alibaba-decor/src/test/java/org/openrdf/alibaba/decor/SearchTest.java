package org.openrdf.alibaba.decor;


import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.openrdf.alibaba.decor.TextPresentation;
import org.openrdf.alibaba.decor.helpers.Context;
import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.formats.Format;
import org.openrdf.alibaba.formats.Layout;
import org.openrdf.alibaba.pov.Display;
import org.openrdf.alibaba.pov.Expression;
import org.openrdf.alibaba.pov.Intent;
import org.openrdf.alibaba.pov.SearchPattern;
import org.openrdf.alibaba.vocabulary.ALI;
import org.openrdf.concepts.dc.DcResource;
import org.openrdf.concepts.foaf.Person;
import org.openrdf.concepts.rdf.Seq;
import org.openrdf.concepts.rdfs.Class;
import org.openrdf.elmo.ElmoManager;
import org.openrdf.elmo.ElmoManagerFactory;
import org.openrdf.elmo.sesame.SesameManagerFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.memory.MemoryStore;

public class SearchTest extends TestCase {
	private static final String POVS_PROPERTIES = "META-INF/org.openrdf.alibaba.povs";

	private static final String DECORS_PROPERTIES = "META-INF/org.openrdf.alibaba.decors";

	private static final String SELECT_NAME_SURNAME = "PREFIX rdf:<"
			+ RDF.NAMESPACE
			+ ">\n"
			+ "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
			+ "SELECT ?name ?surname "
			+ "WHERE {?person rdf:type foaf:Person ; foaf:name ?name ; foaf:surname ?surname}";

	private static final String NS = "http://www.example.com/rdf/2007/";

	private Repository repository;

	private ElmoManager manager;

	@SuppressWarnings("unchecked")
	public void testTable() throws Exception {
		Display name = createBindingDisplay("name");
		Display surname = createBindingDisplay("surname");

		Seq list = manager.create(Seq.class);
		list.add(name);
		list.add(surname);
		Expression expression = manager.create(Expression.class);
		expression.setPovInSparql(SELECT_NAME_SURNAME);
		SearchPattern query = manager.create(SearchPattern.class);
		query.setPovSelectExpression(expression);
		query.setPovLayout((Layout) manager.find(new QName(ALI.NS, "table")));
		query.setPovDisplays(list);
		Person megan = manager.create(Person.class, new QName(NS, "megan"));
		megan.getFoafNames().add("Megan");
		megan.getFoafSurnames().add("Smith");
		Person kelly = manager.create(Person.class, new QName(NS, "kelly"));
		kelly.getFoafNames().add("Kelly");
		kelly.getFoafSurnames().add("Smith");
		assertEquals("name\tsurname\nMegan\tSmith\nKelly\tSmith", load(query,
				Collections.EMPTY_MAP, null));
	}

	@SuppressWarnings("unchecked")
	public void testParameters() throws Exception {
		Display name = createBindingDisplay("name");
		Display surname = createBindingDisplay("surname");

		Expression expression = manager.create(Expression.class);
		expression.setPovInSparql(SELECT_NAME_SURNAME);
		expression.getPovBindings().add(name);
		SearchPattern query = manager.create(SearchPattern.class);
		query.setPovSelectExpression(expression);
		query.setPovLayout((Layout) manager.find(new QName(ALI.NS, "table")));
		Seq list = manager.create(Seq.class);
		list.add(name);
		list.add(surname);
		query.setPovDisplays(list);
		Person megan = manager.create(Person.class, new QName(NS, "megan"));
		megan.getFoafNames().add("Megan");
		megan.getFoafSurnames().add("Smith");
		Person kelly = manager.create(Person.class, new QName(NS, "kelly"));
		kelly.setFoafTitle("\t");
		kelly.getFoafNames().add("Kelly");
		kelly.getFoafSurnames().add("Smith");
		Map parameters = Collections.singletonMap("name", "Megan");
		assertEquals("name\tsurname\nMegan\tSmith", load(query, parameters,
				null));
	}

	private Display createBindingDisplay(String label) {
		Display name = manager.create(Display.class);
		name.setPovFormat((Format) manager.find(new QName(ALI.NS, "none")));
		name.setPovName(label);
		((DcResource) name).setRdfsLabel(label);
		return name;
	}

	@Override
	protected void setUp() throws Exception {
		repository = new SailRepository(new MemoryStore());
		repository.initialize();
		RepositoryConnection conn = repository.getConnection();
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		loadPropertyKeysAsResource(conn, cl, POVS_PROPERTIES);
		loadPropertyKeysAsResource(conn, cl, DECORS_PROPERTIES);
		conn.close();
		ElmoManagerFactory factory = new SesameManagerFactory(repository);
		manager = factory.createElmoManager(Locale.US);
	}

	@Override
	protected void tearDown() throws Exception {
		manager.close();
		repository.shutDown();
	}

	private void loadPropertyKeysAsResource(RepositoryConnection conn, ClassLoader cl, String listing) throws IOException, RDFParseException, RepositoryException {
		Enumeration<URL> list = cl.getResources(listing);
		while (list.hasMoreElements()) {
			Properties prop = new Properties();
			prop.load(list.nextElement().openStream());
			for (Object res : prop.keySet()) {
				URL url = cl.getResource(res.toString());
				RDFFormat format = RDFFormat.forFileName(url.getFile());
				conn.add(url, "", format);
			}
		}
	}

	private String load(SearchPattern spec, Map<String, String> parameters,
			String orderBy) throws AlibabaException, IOException {
		CharArrayWriter writer = new CharArrayWriter();
		Intent intention = (Intent) manager.find(ALI.GENERAL);
		TextPresentation present = (TextPresentation) manager
				.find(ALI.TEXT_PRESENTATION);
		spec.setPovPurpose(intention);
		Class type = manager.create(Class.class);
		spec.getPovRepresents().add(type);
		Context ctx = new Context(parameters, orderBy);
		ctx.setWriter(new PrintWriter(writer));
		ctx.setLocale(manager.getLocale());
		present.exportPresentation(intention, spec, type, ctx);
		return writer.toString().trim();
	}

}
