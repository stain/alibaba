package org.openrdf.alibaba.decor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.openrdf.alibaba.decor.helpers.Context;
import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.factories.DisplayFactory;
import org.openrdf.alibaba.formats.Layout;
import org.openrdf.alibaba.pov.Display;
import org.openrdf.alibaba.pov.Intent;
import org.openrdf.alibaba.pov.Perspective;
import org.openrdf.alibaba.vocabulary.ALI;
import org.openrdf.concepts.owl.DatatypeProperty;
import org.openrdf.concepts.owl.ObjectProperty;
import org.openrdf.concepts.rdf.Seq;
import org.openrdf.concepts.rdfs.Class;
import org.openrdf.elmo.ElmoManager;
import org.openrdf.elmo.ElmoManagerFactory;
import org.openrdf.elmo.ElmoModule;
import org.openrdf.elmo.Entity;
import org.openrdf.elmo.annotations.rdf;
import org.openrdf.elmo.sesame.SesameManagerFactory;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.realiser.StatementRealiserRepository;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.memory.MemoryStore;

public class TextImportTest extends TestCase {
	private static final String POVS_PROPERTIES = "META-INF/org.openrdf.alibaba.povs";

	private static final String DECORS_PROPERTIES = "META-INF/org.openrdf.alibaba.decors";

	private static final String NS = "http://www.example.com/rdf/2007/";

	@rdf(NS + "EditableResource")
	public interface EditableResource extends Entity {
		@rdf(NS + "values")
		public abstract Set<String> getValues();

		public abstract void setValues(Set<String> values);

		@rdf(NS + "value")
		public abstract String getValue();

		public abstract void setValue(String value);
	}

	@rdf(NS + "EditableAggregate")
	public interface EditableAggregate extends Entity {
		@rdf(NS + "resources")
		public abstract Set<EditableResource> getResources();

		public abstract void setResources(Set<EditableResource> resources);

		@rdf(NS + "resource")
		public abstract EditableResource getResource();

		public abstract void setResource(EditableResource resource);

		@rdf(NS + "seq")
		public abstract Seq<EditableResource> getSeq();

		public abstract void setList(Seq<EditableResource> seq);
	}

	private Repository repository;

	private ElmoManager manager;

	private EditableAggregate target;

	private TextPresentation presentation;

	private Intent intent;

	private Perspective perspective;

	public void testImportRemoveValue() throws Exception {
		EditableResource resource = manager.create(EditableResource.class);
		resource.setValue("functional value");
		target.setResource(resource);
		save("[{'resource':[{'value':[],'values':[]}],'resources':[]}]");
		assertEquals(resource, target.getResource());
		assertNull(target.getResource().getValue());
	}

	public void testImportAddValue() throws Exception {
		EditableResource resource = manager.create(EditableResource.class);
		target.setResource(resource);
		save("[{'resource':[{'value':['functional value'],'values':[]}],'resources':[]}]");
		assertEquals(resource, target.getResource());
		assertEquals("functional value", target.getResource().getValue());
	}

	public void testImportRemoveAllValues() throws Exception {
		EditableResource resource = manager.create(EditableResource.class);
		resource.getValues().add("value1");
		resource.getValues().add("value2");
		resource.getValues().add("value3");
		target.setResource(resource);
		save("[{'resource':[{'value':[],'values':[]}],'resources':[]}]");
		assertEquals(resource, target.getResource());
		assertTrue(target.getResource().getValues().isEmpty());
	}

	public void testImportRemoveFirstValues() throws Exception {
		EditableResource resource = manager.create(EditableResource.class);
		resource.getValues().add("value1");
		resource.getValues().add("value2");
		resource.getValues().add("value3");
		target.setResource(resource);
		save("[{'resource':[{'value':[],'values':['value2','value3']}],'resources':[]}]");
		assertEquals(resource, target.getResource());
		assertEquals(2, target.getResource().getValues().size());
		assertTrue(target.getResource().getValues().contains("value2"));
		assertTrue(target.getResource().getValues().contains("value3"));
	}

	public void testImportRemoveLastValues() throws Exception {
		EditableResource resource = manager.create(EditableResource.class);
		resource.getValues().add("value1");
		resource.getValues().add("value2");
		resource.getValues().add("value3");
		target.setResource(resource);
		save("[{'resource':[{'value':[],'values':['value1','value2']}],'resources':[]}]");
		assertEquals(resource, target.getResource());
		assertEquals(2, target.getResource().getValues().size());
		assertTrue(target.getResource().getValues().contains("value1"));
		assertTrue(target.getResource().getValues().contains("value2"));
	}

	public void testImportRemoveMidValues() throws Exception {
		EditableResource resource = manager.create(EditableResource.class);
		resource.getValues().add("value1");
		resource.getValues().add("value2");
		resource.getValues().add("value3");
		target.setResource(resource);
		save("[{'resource':[{'value':[],'values':['value1','value3']}],'resources':[]}]");
		assertEquals(resource, target.getResource());
		assertEquals(2, target.getResource().getValues().size());
		assertTrue(target.getResource().getValues().contains("value1"));
		assertTrue(target.getResource().getValues().contains("value3"));
	}

	public void testImportAddFirstValues() throws Exception {
		EditableResource resource = manager.create(EditableResource.class);
		resource.getValues().add("value2");
		resource.getValues().add("value3");
		target.setResource(resource);
		save("[{'resource':[{'value':[],'values':['value1','value2','value3']}],'resources':[]}]");
		assertEquals(resource, target.getResource());
		assertEquals(3, target.getResource().getValues().size());
		assertTrue(target.getResource().getValues().contains("value1"));
		assertTrue(target.getResource().getValues().contains("value2"));
		assertTrue(target.getResource().getValues().contains("value3"));
	}

	public void testImportAddLastValues() throws Exception {
		EditableResource resource = manager.create(EditableResource.class);
		resource.getValues().add("value1");
		resource.getValues().add("value2");
		target.setResource(resource);
		save("[{'resource':[{'value':[],'values':['value1','value2','value3']}],'resources':[]}]");
		assertEquals(resource, target.getResource());
		assertEquals(3, target.getResource().getValues().size());
		assertTrue(target.getResource().getValues().contains("value1"));
		assertTrue(target.getResource().getValues().contains("value2"));
		assertTrue(target.getResource().getValues().contains("value3"));
	}

	public void testImportAddMidValues() throws Exception {
		EditableResource resource = manager.create(EditableResource.class);
		resource.getValues().add("value1");
		resource.getValues().add("value3");
		target.setResource(resource);
		save("[{'resource':[{'value':[],'values':['value1','value2','value3']}],'resources':[]}]");
		assertEquals(resource, target.getResource());
		assertEquals(3, target.getResource().getValues().size());
		assertTrue(target.getResource().getValues().contains("value1"));
		assertTrue(target.getResource().getValues().contains("value2"));
		assertTrue(target.getResource().getValues().contains("value3"));
	}

	public void testImportAddAllValues() throws Exception {
		EditableResource resource = manager.create(EditableResource.class);
		target.setResource(resource);
		save("[{'resource':[{'value':[],'values':['value1','value2','value3']}],'resources':[]}]");
		assertEquals(resource, target.getResource());
		assertEquals(3, target.getResource().getValues().size());
		assertTrue(target.getResource().getValues().contains("value1"));
		assertTrue(target.getResource().getValues().contains("value2"));
		assertTrue(target.getResource().getValues().contains("value3"));
	}

	public void testImportRemoveResource() throws Exception {
		EditableResource resource = manager.create(EditableResource.class);
		target.setResource(resource);
		save("[{'resource':[],'resources':[]}]");
		assertNull(target.getResource());
	}

	public void testImportAddResource() throws Exception {
		// TODO save("[{'resource':[{'value':[],'values':[]}],'resources':[]}]");
		// TODO assertNotNull(target.getResource());
	}

	public void testImportRemoveAllResources() throws Exception {
		EditableResource resource1 = manager.create(EditableResource.class);
		EditableResource resource2 = manager.create(EditableResource.class);
		EditableResource resource3 = manager.create(EditableResource.class);
		resource1.setValue("value1");
		resource2.setValue("value2");
		resource3.setValue("value3");
		target.getResources().add(resource1);
		target.getResources().add(resource2);
		target.getResources().add(resource3);
		save("[{'resource':[],'resources':[]}]");
		assertEquals(0, target.getResources().size());
	}

	public void testImportRemoveFirstResources() throws Exception {
		EditableResource resource1 = manager.create(EditableResource.class);
		EditableResource resource2 = manager.create(EditableResource.class);
		EditableResource resource3 = manager.create(EditableResource.class);
		resource1.setValue("value1");
		resource2.setValue("value2");
		resource3.setValue("value3");
		target.getResources().add(resource1);
		target.getResources().add(resource2);
		target.getResources().add(resource3);
		save("[{'resource':[],'resources':[{'value':['value2'],'values':[]},{'value':['value3'],'values':[]}]}]");
		assertEquals(2, target.getResources().size());
	}

	public void testImportAddResources() throws Exception {

	}

	public void testImportRemoveSeq() throws Exception {

	}

	public void testImportAddSeq() throws Exception {

	}

	public void testImportRemoveMember() throws Exception {

	}

	public void testImportAddMember() throws Exception {

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
		ElmoModule module = new ElmoModule();
		module.recordRole(EditableResource.class);
		module.recordRole(EditableAggregate.class);
		ElmoManagerFactory factory = new SesameManagerFactory(module,
				repository);
		manager = factory.createElmoManager(Locale.US);
		presentation = (TextPresentation) manager.find(ALI.JSON_PRESENTATION);
		intent = (Intent) manager.find(ALI.MODIFY);
		Layout layout = (Layout) manager.find(ALI.INLINE);
		DisplayFactory dfactory = (DisplayFactory) manager.find(ALI.DISPLAY_FACTORY);
		perspective = createPerspective(intent, layout, dfactory);
		target = manager.create(EditableAggregate.class);
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

	private Perspective createPerspective(Intent intent, Layout layout, DisplayFactory factory) {
		Perspective perspective = manager.create(Perspective.class);
		perspective.setPovPurpose(intent);
		perspective.setPovLayout(layout);
		QName typeName = new QName(NS, "EditableAggregate");
		Class type = manager.create(Class.class, typeName);
		perspective.getPovRepresents().add(type);

		Seq<Display> displays = manager.create(Seq.class);
		Perspective subperspective = createSubperspective(intent, layout, factory);

		ObjectProperty resource = createObjectProperty("resource");
		Display fDisplay = factory.createFunctionalDisplay(resource);
		fDisplay.setPovPerspective(subperspective);
		displays.add(fDisplay);

		ObjectProperty resources = createObjectProperty("resources");
		Display display = factory.createDisplay(resources);
		display.setPovPerspective(subperspective);
		displays.add(display);

		perspective.setPovDisplays(displays);
		return perspective;
	}

	private ObjectProperty createObjectProperty(String name) {
		return manager.create(ObjectProperty.class, new QName(NS, name));
	}

	private Perspective createSubperspective(Intent intent,
			Layout layout, DisplayFactory factory) {
		Perspective perspective = manager.create(Perspective.class);
		perspective.setPovPurpose(intent);
		perspective.setPovLayout(layout);
		QName typeName = new QName(NS, "EditableResource");
		Class type = manager.create(Class.class, typeName);
		perspective.getPovRepresents().add(type);
		Seq<Display> displays = manager.create(Seq.class);
		DatatypeProperty resource = createDatatypeProperty("value");
		displays.add(factory.createFunctionalDisplay(resource));
		DatatypeProperty resources = createDatatypeProperty("values");
		displays.add(factory.createDisplay(resources));
		perspective.setPovDisplays(displays);
		return perspective;
	}

	private DatatypeProperty createDatatypeProperty(String name) {
		return manager.create(DatatypeProperty.class, new QName(NS, name));
	}

	@Override
	protected void tearDown() throws Exception {
		manager.close();
		repository.shutDown();
	}

	private void save(String text) throws AlibabaException, IOException {
		StringReader reader = new StringReader(text);
		Context ctx = new Context();
		ctx.setReader(new BufferedReader(reader));
		ctx.setLocale(manager.getLocale());
		presentation.importPresentation(intent, perspective, target, ctx);
	}

}
