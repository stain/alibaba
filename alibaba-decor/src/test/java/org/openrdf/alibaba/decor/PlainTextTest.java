package org.openrdf.alibaba.decor;

import info.aduna.concurrent.locks.Lock;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Properties;

import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.openrdf.alibaba.decor.helpers.Context;
import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.formats.Format;
import org.openrdf.alibaba.formats.Layout;
import org.openrdf.alibaba.formats.MessagePatternFormat;
import org.openrdf.alibaba.pov.Display;
import org.openrdf.alibaba.pov.Intent;
import org.openrdf.alibaba.pov.Perspective;
import org.openrdf.alibaba.pov.PropertyDisplay;
import org.openrdf.alibaba.pov.QNameDisplay;
import org.openrdf.alibaba.pov.SeqDisplay;
import org.openrdf.alibaba.vocabulary.ALI;
import org.openrdf.concepts.foaf.Person;
import org.openrdf.concepts.rdf.Property;
import org.openrdf.concepts.rdf.Seq;
import org.openrdf.elmo.ElmoManager;
import org.openrdf.elmo.ElmoManagerFactory;
import org.openrdf.elmo.Entity;
import org.openrdf.elmo.sesame.SesameManagerFactory;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.flushable.FlushableRepository;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.SailException;
import org.openrdf.sail.memory.MemoryStore;

public class PlainTextTest extends TestCase {
	private static final String POVS_PROPERTIES = "META-INF/org.openrdf.alibaba.povs";

	private static final String DECORS_PROPERTIES = "META-INF/org.openrdf.alibaba.decors";

	private static final String NS = "http://www.example.com/rdf/2007/";

	private Repository repository;

	private ElmoManager manager;

	private MemoryStore store;

	public void testUrl() throws Exception {
		Display link = manager.designate(QNameDisplay.class);
		link.setPovFormat((Format) manager.find(ALI.URL));
		Seq list = manager.designate(Seq.class);
		list.add(link);
		Perspective spec = manager.designate(Perspective.class);
		spec.setPovLayout((Layout) manager.find(ALI.INLINE));
		spec.setPovDisplays(list);
		Entity target = manager.designate(Person.class, new QName(NS, "target"));
		assertEquals(NS + "target", load(target, spec));
	}

	public void testProperty() throws Exception {
		SeqDisplay name = manager.designate(SeqDisplay.class);
		Seq seq = manager.designate(Seq.class);
		QName foafName = new QName("http://xmlns.com/foaf/0.1/name");
		QName foafSurname = new QName("http://xmlns.com/foaf/0.1/surname");
		seq.add(manager.designate(Property.class, foafName));
		seq.add(manager.designate(Property.class, foafSurname));
		name.setPovProperties(seq);
		MessagePatternFormat format;
		format = (MessagePatternFormat) manager.find(ALI.SECOND_FIRST);
		name.setPovFormat(format);
		Seq list = manager.designate(Seq.class);
		list.add(name);
		Perspective spec = manager.designate(Perspective.class);
		spec.setPovLayout((Layout) manager.find(ALI.INLINE));
		spec.setPovDisplays(list);
		Person target = manager.designate(Person.class, new QName(NS, "target"));
		target.getFoafNames().add("james");
		target.getFoafSurnames().add("leigh");
		assertTrue(target.getFoafNames().contains("james"));
		assertEquals("leigh, james", load(target, spec));
	}

	public void testDate() throws Exception {
		PropertyDisplay name = manager.designate(PropertyDisplay.class);
		QName foafBirthday = new QName("http://xmlns.com/foaf/0.1/birthday");
		name.setPovProperty(manager.designate(Property.class, foafBirthday));
		name.setPovFormat((MessagePatternFormat) manager.find(ALI.MEDIUM_DATE));
		Seq list = manager.designate(Seq.class);
		list.add(name);
		Perspective spec = manager.designate(Perspective.class);
		spec.setPovLayout((Layout) manager.find(ALI.INLINE));
		spec.setPovDisplays(list);
		Person target = manager.designate(Person.class, new QName(NS, "target"));
		GregorianCalendar cal = new GregorianCalendar(1970, Calendar.JANUARY, 1);
		target.setFoafBirthday(cal);
		assertEquals("Jan 1, 1970", load(target, spec));
	}

	public void testNumber() throws Exception {
		PropertyDisplay name = manager.designate(PropertyDisplay.class);
		QName foafIcqChatID = new QName("http://xmlns.com/foaf/0.1/icqChatID");
		name.setPovProperty(manager.designate(Property.class, foafIcqChatID));
		name.setPovFormat((MessagePatternFormat) manager.find(ALI.INTEGER));
		Seq list = manager.designate(Seq.class);
		list.add(name);
		Perspective spec = manager.designate(Perspective.class);
		spec.setPovLayout((Layout) manager.find(ALI.INLINE));
		spec.setPovDisplays(list);
		Person target = manager.designate(Person.class, new QName(NS, "target"));
		target.getFoafIcqChatIDs().add(new Integer(10230));
		assertEquals("10,230", load(target, spec));
	}

	@Override
	protected void setUp() throws Exception {
		store = new MemoryStore();
		repository = new SailRepository(store);
		repository = new FlushableRepository(repository);
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

	private void loadPropertyKeysAsResource(RepositoryConnection conn,
			ClassLoader cl, String listing) throws IOException,
			RDFParseException, RepositoryException {
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

	private String load(Object target, Perspective spec)
			throws AlibabaException, IOException {
		CharArrayWriter writer = new CharArrayWriter();
		TextPresentation present;
		present = (TextPresentation) manager.find(ALI.TEXT_PRESENTATION);
		Intent intention = (Intent) manager.find(ALI.GENERAL);
		spec.setPovPurpose(intention);
		Context ctx = new Context();
		ctx.setElmoManager(manager);
		ctx.setIntent(intention);
		ctx.setWriter(new PrintWriter(writer));
		ctx.setLocale(manager.getLocale());
		present.exportPresentation(spec, (Entity) target, ctx);
		return writer.toString().trim();
	}

}
