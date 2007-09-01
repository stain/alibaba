package org.openrdf.alibaba;


import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.openrdf.alibaba.concepts.Display;
import org.openrdf.alibaba.concepts.DisplayFactory;
import org.openrdf.alibaba.concepts.Expression;
import org.openrdf.alibaba.concepts.Intent;
import org.openrdf.alibaba.concepts.Layout;
import org.openrdf.alibaba.concepts.PerspectiveFactory;
import org.openrdf.alibaba.concepts.SearchPattern;
import org.openrdf.alibaba.decor.TextPresentation;
import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.vocabulary.ALI;
import org.openrdf.concepts.foaf.Person;
import org.openrdf.concepts.owl.DatatypeProperty;
import org.openrdf.concepts.rdf.Seq;
import org.openrdf.concepts.rdfs.Class;
import org.openrdf.elmo.ElmoManager;
import org.openrdf.elmo.ElmoManagerFactory;
import org.openrdf.elmo.sesame.SesameManagerFactory;
import org.openrdf.model.vocabulary.FOAF;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.realiser.StatementRealiserRepository;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.memory.MemoryStore;

public class JsonTest extends TestCase {
	private static final String POVS_PROPERTIES = "META-INF/org.openrdf.alibaba.povs";

	private static final String DECORS_PROPERTIES = "META-INF/org.openrdf.alibaba.decors";

	private static final String SELECT_PERSON = "PREFIX rdf:<"
			+ RDF.NAMESPACE
			+ ">\n"
			+ "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
			+ "SELECT ?person "
			+ "WHERE {?person rdf:type foaf:Person ; foaf:name ?name ; foaf:surname ?surname}";

	private static final String NS = "http://www.example.com/rdf/2007/";

	private Repository repository;

	private ElmoManager manager;

	public void testImport() throws Exception {
		DatatypeProperty namep = manager.create(DatatypeProperty.class,
				new QName(FOAF.NAMESPACE, "name"));
		DatatypeProperty surnamep = manager.create(DatatypeProperty.class,
				new QName(FOAF.NAMESPACE, "surname"));
		Display name = createPropertyDisplay(namep);
		Display surname = createPropertyDisplay(surnamep);

		Seq list = manager.create(Seq.class);
		list.add(name);
		list.add(surname);
		Expression expression = manager.create(Expression.class);
		expression.setPovInSparql(SELECT_PERSON);
		SearchPattern query = manager.create(SearchPattern.class, new QName(NS, "test-import"));
		query.setPovSelectExpression(expression);
		query.setPovLayout((Layout) manager.find(new QName(ALI.NS, "table")));
		query.setPovDisplays(list);
		// create data
		Person megan = manager.create(Person.class, new QName(NS, "megan"));
		megan.getFoafNames().add("Megan");
		megan.getFoafSurnames().add("Smith");
		Person kelly = manager.create(Person.class, new QName(NS, "kelly"));
		kelly.getFoafNames().add("Kelly");
		kelly.getFoafSurnames().add("Smith");
		// test
		String string = "[{'name':['Megan'],'surname':['Leigh']},{'name':['Kelly'],'surname':['Smith']}]";
		save(string, query, Collections.EMPTY_MAP, null);
		megan = (Person) manager.find(new QName(NS, "megan"));
		assertEquals("Leigh", megan.getFoafSurnames().toArray()[0]);
	}

	public void testExport() throws Exception {
		DatatypeProperty namep = manager.create(DatatypeProperty.class,
				new QName(FOAF.NAMESPACE, "name"));
		DatatypeProperty surnamep = manager.create(DatatypeProperty.class,
				new QName(FOAF.NAMESPACE, "surname"));
		Display name = createPropertyDisplay(namep);
		Display surname = createPropertyDisplay(surnamep);

		Seq list = manager.create(Seq.class);
		list.add(name);
		list.add(surname);
		Expression expression = manager.create(Expression.class);
		expression.setPovInSparql(SELECT_PERSON);
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
		String string = "[{'name':['Megan'],'surname':['Smith']},{'name':['Kelly'],'surname':['Smith']}]";
		assertEquals(string, load(query, Collections.EMPTY_MAP, null));
	}

	private Display createPropertyDisplay(DatatypeProperty property) {
		TextPresentation present = (TextPresentation) manager
				.find(ALI.JSON_PRESENTATION);
		PerspectiveFactory pf = present.getPovPerspectiveFactory();
		DisplayFactory df = pf.getPovDisplayFactory();
		Display display = df.createPropertyDisplay(property);
		return display;
	}

	@Override
	protected void setUp() throws Exception {
		repository = new SailRepository(new MemoryStore());
		repository = new StatementRealiserRepository(repository);
		repository.initialize();
		RepositoryConnection conn = repository.getConnection();
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		loadPropertyKeysAsResource(conn, cl, POVS_PROPERTIES);
		loadPropertyKeysAsResource(conn, cl, DECORS_PROPERTIES);
		conn.close();
		ElmoManagerFactory factory = new SesameManagerFactory(repository);
		manager = factory.createElmoManager(Locale.US);
		manager.setAutoFlush(false);
	}

	@Override
	protected void tearDown() throws Exception {
		manager.refresh();
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
				.find(ALI.JSON_PRESENTATION);
		spec.setPovPurpose(intention);
		Class type = manager.create(Class.class);
		spec.getPovRepresents().add(type);
		present.getPovSearchPatterns().add(spec);
		manager.flush();
		present.exportPresentation(intention, type, parameters, orderBy,
				new PrintWriter(writer));
		return writer.toString().trim();
	}

	private void save(String text, SearchPattern spec,
			Map<String, String> parameters, String orderBy)
			throws AlibabaException, IOException {
		StringReader reader = new StringReader(text);
		Intent intention = (Intent) manager.find(ALI.GENERAL);
		TextPresentation present = (TextPresentation) manager
				.find(ALI.JSON_PRESENTATION);
		spec.setPovPurpose(intention);
		Class type = manager.create(Class.class);
		spec.getPovRepresents().add(type);
		present.getPovSearchPatterns().add(spec);
		manager.flush();
		present.importPresentation(intention, type, parameters, orderBy,
				new BufferedReader(reader));
	}

}
