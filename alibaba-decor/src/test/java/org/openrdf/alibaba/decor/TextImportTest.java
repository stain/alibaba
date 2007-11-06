package org.openrdf.alibaba.decor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
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
import org.openrdf.alibaba.pov.Expression;
import org.openrdf.alibaba.pov.Intent;
import org.openrdf.alibaba.pov.Perspective;
import org.openrdf.alibaba.pov.ReferencePerspective;
import org.openrdf.alibaba.pov.SearchPattern;
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
import org.openrdf.repository.flushable.FlushableRepository;
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
		@rdf(NS + "value")
		public abstract String getValue();

		public abstract void setValue(String value);

		@rdf(NS + "values")
		public abstract Set<String> getValues();

		public abstract void setValues(Set<String> values);
	}

	@rdf(NS + "EditableAggregate")
	public interface EditableAggregate extends Entity {
		@rdf(NS + "resource")
		public abstract EditableResource getResource();

		public abstract void setResource(EditableResource resource);

		@rdf(NS + "resources")
		public abstract Set<EditableResource> getResources();

		public abstract void setResources(Set<EditableResource> resources);

		@rdf(NS + "list")
		public abstract List<EditableResource> getList();

		public abstract void setList(List<EditableResource> list);

		@rdf(NS + "lookup")
		public abstract EditableResource getLookup();

		public abstract void setLookup(EditableResource resource);
	}

	private Repository repository;

	private ElmoManager manager;

	private EditableAggregate target;

	private TextPresentation presentation;

	private Intent intent;

	private Perspective perspective;

	public void testImportRemoveValue() throws Exception {
		EditableResource resource = manager.designate(EditableResource.class);
		resource.setValue("functional value");
		target.setResource(resource);
		save("[{'resource':[{'value':[],'values':[]}],'resources':[],'list':[],'lookup':[]}]");
		assertEquals(resource, target.getResource());
		assertNull(target.getResource().getValue());
	}

	public void testImportAddValue() throws Exception {
		EditableResource resource = manager.designate(EditableResource.class);
		target.setResource(resource);
		save("[{'resource':[{'value':['functional value'],'values':[]}],'resources':[],'list':[],'lookup':[]}]");
		assertEquals(resource, target.getResource());
		assertEquals("functional value", target.getResource().getValue());
	}

	public void testImportRemoveAllValues() throws Exception {
		EditableResource resource = manager.designate(EditableResource.class);
		resource.getValues().add("value1");
		resource.getValues().add("value2");
		resource.getValues().add("value3");
		target.setResource(resource);
		save("[{'resource':[{'value':[],'values':[]}],'resources':[],'list':[],'lookup':[]}]");
		assertEquals(resource, target.getResource());
		assertTrue(target.getResource().getValues().isEmpty());
	}

	public void testImportRemoveFirstValues() throws Exception {
		EditableResource resource = manager.designate(EditableResource.class);
		resource.getValues().add("value1");
		resource.getValues().add("value2");
		resource.getValues().add("value3");
		target.setResource(resource);
		save("[{'resource':[{'value':[],'values':['value2','value3']}],'resources':[],'list':[],'lookup':[]}]");
		assertEquals(resource, target.getResource());
		assertEquals(2, target.getResource().getValues().size());
		assertTrue(target.getResource().getValues().contains("value2"));
		assertTrue(target.getResource().getValues().contains("value3"));
	}

	public void testImportRemoveLastValues() throws Exception {
		EditableResource resource = manager.designate(EditableResource.class);
		resource.getValues().add("value1");
		resource.getValues().add("value2");
		resource.getValues().add("value3");
		target.setResource(resource);
		save("[{'resource':[{'value':[],'values':['value1','value2']}],'resources':[],'list':[],'lookup':[]}]");
		assertEquals(resource, target.getResource());
		assertEquals(2, target.getResource().getValues().size());
		assertTrue(target.getResource().getValues().contains("value1"));
		assertTrue(target.getResource().getValues().contains("value2"));
	}

	public void testImportRemoveMidValues() throws Exception {
		EditableResource resource = manager.designate(EditableResource.class);
		resource.getValues().add("value1");
		resource.getValues().add("value2");
		resource.getValues().add("value3");
		target.setResource(resource);
		save("[{'resource':[{'value':[],'values':['value1','value3']}],'resources':[],'list':[],'lookup':[]}]");
		assertEquals(resource, target.getResource());
		assertEquals(2, target.getResource().getValues().size());
		assertTrue(target.getResource().getValues().contains("value1"));
		assertTrue(target.getResource().getValues().contains("value3"));
	}

	public void testImportAddFirstValues() throws Exception {
		EditableResource resource = manager.designate(EditableResource.class);
		resource.getValues().add("value2");
		resource.getValues().add("value3");
		target.setResource(resource);
		save("[{'resource':[{'value':[],'values':['value1','value2','value3']}],'resources':[],'list':[],'lookup':[]}]");
		assertEquals(resource, target.getResource());
		assertEquals(3, target.getResource().getValues().size());
		assertTrue(target.getResource().getValues().contains("value1"));
		assertTrue(target.getResource().getValues().contains("value2"));
		assertTrue(target.getResource().getValues().contains("value3"));
	}

	public void testImportAddLastValues() throws Exception {
		EditableResource resource = manager.designate(EditableResource.class);
		resource.getValues().add("value1");
		resource.getValues().add("value2");
		target.setResource(resource);
		save("[{'resource':[{'value':[],'values':['value1','value2','value3']}],'resources':[],'list':[],'lookup':[]}]");
		assertEquals(resource, target.getResource());
		assertEquals(3, target.getResource().getValues().size());
		assertTrue(target.getResource().getValues().contains("value1"));
		assertTrue(target.getResource().getValues().contains("value2"));
		assertTrue(target.getResource().getValues().contains("value3"));
	}

	public void testImportAddMidValues() throws Exception {
		EditableResource resource = manager.designate(EditableResource.class);
		resource.getValues().add("value1");
		resource.getValues().add("value3");
		target.setResource(resource);
		save("[{'resource':[{'value':[],'values':['value1','value2','value3']}],'resources':[],'list':[],'lookup':[]}]");
		assertEquals(resource, target.getResource());
		assertEquals(3, target.getResource().getValues().size());
		assertTrue(target.getResource().getValues().contains("value1"));
		assertTrue(target.getResource().getValues().contains("value2"));
		assertTrue(target.getResource().getValues().contains("value3"));
	}

	public void testImportAddAllValues() throws Exception {
		EditableResource resource = manager.designate(EditableResource.class);
		target.setResource(resource);
		save("[{'resource':[{'value':[],'values':['value1','value2','value3']}],'resources':[],'list':[],'lookup':[]}]");
		assertEquals(resource, target.getResource());
		assertEquals(3, target.getResource().getValues().size());
		assertTrue(target.getResource().getValues().contains("value1"));
		assertTrue(target.getResource().getValues().contains("value2"));
		assertTrue(target.getResource().getValues().contains("value3"));
	}

	public void testImportRemoveResource() throws Exception {
		EditableResource resource = manager.designate(EditableResource.class);
		target.setResource(resource);
		save("[{'resource':[],'resources':[],'list':[],'lookup':[]}]");
		assertNull(target.getResource());
	}

	public void testImportAddResource() throws Exception {
		save("[{'resource':[{'value':[],'values':[]}],'resources':[],'list':[],'lookup':[]}]");
		assertNotNull(target.getResource());
	}

	public void testImportRemoveAllResources() throws Exception {
		EditableResource resource1 = manager.designate(EditableResource.class);
		EditableResource resource2 = manager.designate(EditableResource.class);
		EditableResource resource3 = manager.designate(EditableResource.class);
		resource1.setValue("value1");
		resource2.setValue("value2");
		resource3.setValue("value3");
		target.getResources().add(resource1);
		target.getResources().add(resource2);
		target.getResources().add(resource3);
		save("[{'resource':[],'resources':[],'list':[],'lookup':[]}]");
		assertEquals(0, target.getResources().size());
	}

	public void testImportRemoveFirstResources() throws Exception {
		EditableResource resource1 = manager.designate(EditableResource.class);
		EditableResource resource2 = manager.designate(EditableResource.class);
		EditableResource resource3 = manager.designate(EditableResource.class);
		resource1.setValue("value1");
		resource2.setValue("value2");
		resource3.setValue("value3");
		target.getResources().add(resource1);
		target.getResources().add(resource2);
		target.getResources().add(resource3);
		save("[{'resource':[],'resources':[{'value':['value2'],'values':[]},{'value':['value3'],'values':[]}],'list':[],'lookup':[]}]");
		assertEquals(2, target.getResources().size());
	}

	public void testImportAddFirstResources() throws Exception {
		EditableResource resource2 = manager.designate(EditableResource.class);
		EditableResource resource3 = manager.designate(EditableResource.class);
		resource2.setValue("value2");
		resource3.setValue("value3");
		target.getResources().add(resource2);
		target.getResources().add(resource3);
		save("[{'resource':[],'resources':[{'value':['value1'],'values':[]},{'value':['value2'],'values':[]},{'value':['value3'],'values':[]}],'list':[],'lookup':[]}]");
		assertEquals(3, target.getResources().size());
	}

	public void testImportAddMidResources() throws Exception {
		EditableResource resource1 = manager.designate(EditableResource.class);
		EditableResource resource3 = manager.designate(EditableResource.class);
		resource1.setValue("value1");
		resource3.setValue("value3");
		target.getResources().add(resource1);
		target.getResources().add(resource3);
		save("[{'resource':[],'resources':[{'value':['value1'],'values':[]},{'value':['value2'],'values':[]},{'value':['value3'],'values':[]}],'list':[],'lookup':[]}]");
		assertEquals(3, target.getResources().size());
	}

	public void testImportAddLastResources() throws Exception {
		EditableResource resource1 = manager.designate(EditableResource.class);
		EditableResource resource2 = manager.designate(EditableResource.class);
		resource1.setValue("value1");
		resource2.setValue("value2");
		target.getResources().add(resource1);
		target.getResources().add(resource2);
		save("[{'resource':[],'resources':[{'value':['value1'],'values':[]},{'value':['value2'],'values':[]},{'value':['value3'],'values':[]}],'list':[],'lookup':[]}]");
		assertEquals(3, target.getResources().size());
	}

	public void testImportRemoveAllList() throws Exception {
		EditableResource resource1 = manager.designate(EditableResource.class);
		EditableResource resource2 = manager.designate(EditableResource.class);
		EditableResource resource3 = manager.designate(EditableResource.class);
		resource1.setValue("value1");
		resource2.setValue("value2");
		resource3.setValue("value3");
		target.setList(manager.designate(Seq.class));
		target.getList().add(resource1);
		target.getList().add(resource2);
		target.getList().add(resource3);
		save("[{'resource':[],'resources':[],'list':[],'lookup':[]}]");
		assertEquals(0, target.getList().size());
	}

	public void testImportRemoveFirstList() throws Exception {
		EditableResource resource1 = manager.designate(EditableResource.class);
		EditableResource resource2 = manager.designate(EditableResource.class);
		EditableResource resource3 = manager.designate(EditableResource.class);
		resource1.setValue("value1");
		resource2.setValue("value2");
		resource3.setValue("value3");
		target.setList(manager.designate(Seq.class));
		target.getList().add(resource1);
		target.getList().add(resource2);
		target.getList().add(resource3);
		save("[{'resource':[],'resources':[],'list':[{'value':['value2'],'values':[]},{'value':['value3'],'values':[]}],'lookup':[]}]");
		assertEquals(2, target.getList().size());
		assertEquals("value2", target.getList().get(0).getValue());
		assertEquals("value3", target.getList().get(1).getValue());
	}

	public void testImportRemoveMidList() throws Exception {
		EditableResource resource1 = manager.designate(EditableResource.class);
		EditableResource resource2 = manager.designate(EditableResource.class);
		EditableResource resource3 = manager.designate(EditableResource.class);
		resource1.setValue("value1");
		resource2.setValue("value2");
		resource3.setValue("value3");
		target.setList(manager.designate(Seq.class));
		target.getList().add(resource1);
		target.getList().add(resource2);
		target.getList().add(resource3);
		save("[{'resource':[],'resources':[],'list':[{'value':['value1'],'values':[]},{'value':['value3'],'values':[]}],'lookup':[]}]");
		assertEquals(2, target.getList().size());
		assertEquals("value1", target.getList().get(0).getValue());
		assertEquals("value3", target.getList().get(1).getValue());
	}

	public void testImportRemoveLastList() throws Exception {
		EditableResource resource1 = manager.designate(EditableResource.class);
		EditableResource resource2 = manager.designate(EditableResource.class);
		EditableResource resource3 = manager.designate(EditableResource.class);
		resource1.setValue("value1");
		resource2.setValue("value2");
		resource3.setValue("value3");
		target.setList(manager.designate(Seq.class));
		target.getList().add(resource1);
		target.getList().add(resource2);
		target.getList().add(resource3);
		save("[{'resource':[],'resources':[],'list':[{'value':['value1'],'values':[]},{'value':['value2'],'values':[]}],'lookup':[]}]");
		assertEquals(2, target.getList().size());
		assertEquals("value1", target.getList().get(0).getValue());
		assertEquals("value2", target.getList().get(1).getValue());
	}

	public void testImportAddAllNullList() throws Exception {
		save("[{'resource':[],'resources':[],'list':[{'value':['value1'],'values':[]},{'value':['value2'],'values':[]},{'value':['value3'],'values':[]}],'lookup':[]}]");
		assertEquals(3, target.getList().size());
		assertEquals("value1", target.getList().get(0).getValue());
		assertEquals("value2", target.getList().get(1).getValue());
		assertEquals("value3", target.getList().get(2).getValue());
	}

	public void testImportAddAllEmptyList() throws Exception {
		target.setList(manager.designate(Seq.class));
		List<Seq> list = manager.findAll(Seq.class).getResultList();
		int size = list.size();
		save("[{'resource':[],'resources':[],'list':[{'value':['value1'],'values':[]},{'value':['value2'],'values':[]},{'value':['value3'],'values':[]}],'lookup':[]}]");
		assertEquals(3, target.getList().size());
		assertEquals("value1", target.getList().get(0).getValue());
		assertEquals("value2", target.getList().get(1).getValue());
		assertEquals("value3", target.getList().get(2).getValue());
		list = manager.findAll(Seq.class).getResultList();
		assertEquals(size, list.size());
	}

	public void testImportAddFirstList() throws Exception {
		EditableResource resource2 = manager.designate(EditableResource.class);
		EditableResource resource3 = manager.designate(EditableResource.class);
		resource2.setValue("value2");
		resource3.setValue("value3");
		target.setList(manager.designate(Seq.class));
		target.getList().add(resource2);
		target.getList().add(resource3);
		save("[{'resource':[],'resources':[],'list':[{'value':['value1'],'values':[]},{'value':['value2'],'values':[]},{'value':['value3'],'values':[]}],'lookup':[]}]");
		assertEquals(3, target.getList().size());
		assertEquals("value1", target.getList().get(0).getValue());
		assertEquals("value2", target.getList().get(1).getValue());
		assertEquals("value3", target.getList().get(2).getValue());
	}

	public void testImportAddMidList() throws Exception {
		EditableResource resource1 = manager.designate(EditableResource.class);
		EditableResource resource3 = manager.designate(EditableResource.class);
		resource1.setValue("value1");
		resource3.setValue("value3");
		target.setList(manager.designate(Seq.class));
		target.getList().add(resource1);
		target.getList().add(resource3);
		save("[{'resource':[],'resources':[],'list':[{'value':['value1'],'values':[]},{'value':['value2'],'values':[]},{'value':['value3'],'values':[]}],'lookup':[]}]");
		assertEquals(3, target.getList().size());
		assertEquals("value1", target.getList().get(0).getValue());
		assertEquals("value2", target.getList().get(1).getValue());
		assertEquals("value3", target.getList().get(2).getValue());
	}

	public void testImportAddLastList() throws Exception {
		EditableResource resource1 = manager.designate(EditableResource.class);
		EditableResource resource2 = manager.designate(EditableResource.class);
		resource1.setValue("value1");
		resource2.setValue("value2");
		target.setList(manager.designate(Seq.class));
		target.getList().add(resource1);
		target.getList().add(resource2);
		save("[{'resource':[],'resources':[],'list':[{'value':['value1'],'values':[]},{'value':['value2'],'values':[]},{'value':['value3'],'values':[]}],'lookup':[]}]");
		assertEquals(3, target.getList().size());
		assertEquals("value1", target.getList().get(0).getValue());
		assertEquals("value2", target.getList().get(1).getValue());
		assertEquals("value3", target.getList().get(2).getValue());
	}

	public void testImportRemoveLookup() throws Exception {
		EditableResource resource1 = manager.designate(EditableResource.class, new QName(NS, "resource1"));
		resource1.setValue("value1");
		target.setLookup(resource1);
		save("[{'resource':[],'resources':[],'list':[],'lookup':[]}]");
		assertEquals(null, target.getLookup());
	}

	public void testImportAddLookup() throws Exception {
		EditableResource resource1 = manager.designate(EditableResource.class, new QName(NS, "resource1"));
		resource1.setValue("value1");
		save("[{'resource':[],'resources':[],'list':[],'lookup':[{'value':['value1']}]}]");
		assertEquals(resource1, target.getLookup());
	}

	public void testImportModifyLookup() throws Exception {
		EditableResource resource1 = manager.designate(EditableResource.class, new QName(NS, "resource1"));
		EditableResource resource2 = manager.designate(EditableResource.class, new QName(NS, "resource2"));
		resource1.setValue("value1");
		resource2.setValue("value2");
		target.setLookup(resource1);
		save("[{'resource':[],'resources':[],'list':[],'lookup':[{'value':['value2']}]}]");
		assertEquals("value1", resource1.getValue());
		assertEquals("value2", resource2.getValue());
		assertEquals(resource2, target.getLookup());
	}

	@Override
	protected void setUp() throws Exception {
		repository = new SailRepository(new MemoryStore());
		repository = new FlushableRepository(repository);
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
		target = manager.designate(EditableAggregate.class, new QName(NS, "target"));
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
		Perspective perspective = manager.designate(Perspective.class, new QName(NS, "perspective"));
		perspective.setPovPurpose(intent);
		perspective.setPovLayout(layout);
		QName typeName = new QName(NS, "EditableAggregate");
		Class type = manager.designate(Class.class, typeName);
		perspective.getPovRepresents().add(type);

		Seq<Display> displays = manager.designate(Seq.class);
		Perspective subperspective = createSubperspective(intent, layout, factory);

		ObjectProperty resource = createObjectProperty("resource");
		Display fDisplay = factory.createFunctionalDisplay(resource);
		fDisplay.setPovPerspective(subperspective);
		fDisplay = manager.rename(fDisplay, new QName(NS, "resource"));
		displays.add(fDisplay);

		ObjectProperty resources = createObjectProperty("resources");
		Display display = factory.createDisplay(resources);
		display.setPovPerspective(subperspective);
		display = manager.rename(display, new QName(NS, "resources"));
		displays.add(display);

		ObjectProperty list = createObjectProperty("list");
		Display lDisplay = factory.createDisplay(list);
		lDisplay.setPovPerspective(subperspective);
		lDisplay = manager.rename(lDisplay, new QName(NS, "list"));
		displays.add(lDisplay);

		ObjectProperty lookup = createObjectProperty("lookup");
		Display kDisplay = factory.createDisplay(lookup);
		Perspective ref = createReferencePerspective(factory);
		kDisplay.setPovPerspective(ref);
		kDisplay = manager.rename(kDisplay, new QName(NS, "lookup"));
		displays.add(kDisplay);

		perspective.setPovDisplays(displays);
		return perspective;
	}

	private ObjectProperty createObjectProperty(String name) {
		return manager.designate(ObjectProperty.class, new QName(NS, name));
	}

	private Perspective createSubperspective(Intent intent,
			Layout layout, DisplayFactory factory) {
		Perspective perspective = manager.designate(Perspective.class, new QName(NS, "subperspective"));
		perspective.setPovPurpose(intent);
		perspective.setPovLayout(layout);
		QName typeName = new QName(NS, "EditableResource");
		Class type = manager.designate(Class.class, typeName);
		perspective.getPovRepresents().add(type);
		Seq<Display> displays = manager.designate(Seq.class, new QName(NS, "displays"));
		DatatypeProperty resource = createDatatypeProperty("value");
		Display value = manager.rename(factory.createFunctionalDisplay(resource), new QName(NS, "value"));
		displays.add(value);
		DatatypeProperty resources = createDatatypeProperty("values");
		Display values = manager.rename(factory.createDisplay(resources), new QName(NS, "values"));
		displays.add(values);
		perspective.setPovDisplays(displays);
		return perspective;
	}

	private Perspective createReferencePerspective(DisplayFactory factory) {
		ReferencePerspective perspective = manager.designate(ReferencePerspective.class, new QName(NS, "reference-perspective"));
		perspective.setPovPurpose((Intent) manager.find(ALI.LOOKUP));
		perspective.setPovLayout((Layout) manager.find(ALI.INLINE));
		QName typeName = new QName(NS, "EditableResource");
		Class type = manager.designate(Class.class, typeName);
		perspective.getPovRepresents().add(type);
		Seq<Display> displays = manager.designate(Seq.class, new QName(NS, "references"));
		DatatypeProperty resource = createDatatypeProperty("value");
		Display value = manager.rename(factory.createFunctionalDisplay(resource), new QName(NS, "key"));
		displays.add(value);
		perspective.setPovDisplays(displays);
		SearchPattern search = manager.designate(SearchPattern.class, new QName(NS, "search-pattern"));
		Expression select = manager.designate(Expression.class, new QName(NS, "select-expression"));
		select.setPovInSparql("SELECT ?resource WHERE {?resource <" + NS + "value> ?value}");
		select.getPovBindings().add(value);
		search.setPovSelectExpression(select);
		perspective.setPovLookup(search);
		return perspective;
	}

	private DatatypeProperty createDatatypeProperty(String name) {
		return manager.designate(DatatypeProperty.class, new QName(NS, name));
	}

	@Override
	protected void tearDown() throws Exception {
		manager.close();
		repository.shutDown();
	}

	private void save(String text) throws AlibabaException, IOException {
		StringReader reader = new StringReader(text);
		Context ctx = new Context();
		ctx.setElmoManager(manager);
		ctx.setIntent(intent);
		ctx.setReader(new BufferedReader(reader));
		ctx.setLocale(manager.getLocale());
		presentation.importPresentation(perspective, target, ctx);
	}

}
