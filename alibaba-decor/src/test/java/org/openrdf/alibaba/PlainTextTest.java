package org.openrdf.alibaba;


import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Properties;

import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.openrdf.alibaba.concepts.Format;
import org.openrdf.alibaba.concepts.Intent;
import org.openrdf.alibaba.concepts.Layout;
import org.openrdf.alibaba.concepts.LiteralDisplay;
import org.openrdf.alibaba.concepts.MessagePatternFormat;
import org.openrdf.alibaba.concepts.Perspective;
import org.openrdf.alibaba.concepts.TextPresentation;
import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.vocabulary.ALI;
import org.openrdf.concepts.foaf.Person;
import org.openrdf.concepts.rdf.Alt;
import org.openrdf.concepts.rdf.Property;
import org.openrdf.concepts.rdf.Seq;
import org.openrdf.concepts.rdfs.Resource;
import org.openrdf.elmo.ElmoManager;
import org.openrdf.elmo.ElmoManagerFactory;
import org.openrdf.elmo.Entity;
import org.openrdf.elmo.sesame.SesameManagerFactory;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.memory.MemoryStore;

public class PlainTextTest extends TestCase {
	private static final String POVS_PROPERTIES = "META-INF/org.openrdf.alibaba.povs";

	private static final String DECORS_PROPERTIES = "META-INF/org.openrdf.alibaba.decors";

	private static final String NS = "http://www.example.com/rdf/2007/";

	private Repository repository;

	private ElmoManager manager;

	public void testUrl() throws Exception {
		LiteralDisplay link = manager.create(LiteralDisplay.class);
		link.setPovFormat((Format) manager.find(ALI.URL));
		Seq list = manager.create(Seq.class);
		list.add(link);
		Perspective spec = manager.create(Perspective.class);
		spec.setPovLayout((Layout) manager.find(ALI.INLINE));
		spec.setPovDisplays(list);
		Entity target = manager.create(Person.class, new QName(NS, "target"));
		assertEquals(NS + "target", load(target, spec));
	}

	public void testProperty() throws Exception {
		LiteralDisplay name = manager.create(LiteralDisplay.class);
		Seq seq = manager.create(Seq.class);
		QName foafName = new QName("http://xmlns.com/foaf/0.1/name");
		QName foafSurname = new QName("http://xmlns.com/foaf/0.1/surname");
		seq.add(manager.create(Property.class, foafName));
		seq.add(manager.create(Property.class, foafSurname));
		name.setPovProperties(seq);
		name.setPovFormat((MessagePatternFormat) manager.find(ALI.SECOND_FIRST));
		Seq list = manager.create(Seq.class);
		list.add(name);
		Perspective spec = manager.create(Perspective.class);
		spec.setPovLayout((Layout) manager.find(ALI.INLINE));
		spec.setPovDisplays(list);
		Person target = manager.create(Person.class, new QName(NS, "target"));
		target.getFoafNames().add("james");
		target.getFoafSurnames().add("leigh");
		assertTrue(target.getFoafNames().contains("james"));
		assertEquals("leigh, james", load(target, spec));
	}

	public void testDate() throws Exception {
		LiteralDisplay name = manager.create(LiteralDisplay.class);
		Alt alt = manager.create(Alt.class);
		QName foafBirthday = new QName("http://xmlns.com/foaf/0.1/birthday");
		alt.add(manager.create(Property.class, foafBirthday));
		name.setPovProperties(alt);
		name.setPovFormat((MessagePatternFormat) manager.find(ALI.MEDIUM_DATE));
		Seq list = manager.create(Seq.class);
		list.add(name);
		Perspective spec = manager.create(Perspective.class);
		spec.setPovLayout((Layout) manager.find(ALI.INLINE));
		spec.setPovDisplays(list);
		Person target = manager.create(Person.class, new QName(NS, "target"));
		GregorianCalendar cal = new GregorianCalendar(1970, Calendar.JANUARY, 1);
		target.setFoafBirthday(cal);
		assertEquals("Jan 1, 1970", load(target, spec));
	}

	public void testNumber() throws Exception {
		LiteralDisplay name = manager.create(LiteralDisplay.class);
		Alt alt = manager.create(Alt.class);
		QName foafIcqChatID = new QName("http://xmlns.com/foaf/0.1/icqChatID");
		alt.add(manager.create(Property.class, foafIcqChatID));
		name.setPovProperties(alt);
		name.setPovFormat((MessagePatternFormat) manager.find(ALI.INTEGER));
		Seq list = manager.create(Seq.class);
		list.add(name);
		Perspective spec = manager.create(Perspective.class);
		spec.setPovLayout((Layout) manager.find(ALI.INLINE));
		spec.setPovDisplays(list);
		Person target = manager.create(Person.class, new QName(NS, "target"));
		target.getFoafIcqChatIDs().add(new Integer(10230));
		assertEquals("10,230", load(target, spec));
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
		URL list = cl.getResource(listing);
		Properties prop = new Properties();
		prop.load(list.openStream());
		for (Object res : prop.keySet()) {
			URL url = cl.getResource(res.toString());
			RDFFormat format = RDFFormat.forFileName(url.getFile());
			conn.add(url, "", format);
		}
	}

	private String load(Object target, Perspective spec)
			throws AlibabaException, IOException {
		CharArrayWriter writer = new CharArrayWriter();
		TextPresentation present = (TextPresentation) manager
				.find(ALI.TEXT_PRESENTATION);
		Intent intention = (Intent) manager.find(ALI.GENERAL);
		spec.setPovPurpose(intention);
		spec.getPovRepresents().addAll(((Resource) target).getRdfTypes());
		present.getPovPerspectives().add(spec);
		present.exportPresentation(intention, (Entity) target, null, null, new PrintWriter(writer));
		return writer.toString().trim();
	}

}
