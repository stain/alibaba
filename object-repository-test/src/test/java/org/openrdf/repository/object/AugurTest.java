package org.openrdf.repository.object;

import java.util.List;
import java.util.Set;

import junit.framework.Test;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.base.ElmoManagerTestCase;
import org.openrdf.result.ModelResult;

public class AugurTest extends ElmoManagerTestCase {

	private static final String NS = "urn:test:";

	public static Test suite() throws Exception {
		return ElmoManagerTestCase.suite(AugurTest.class);
	}

	@rdf("urn:test:Bean")
	public static interface Bean extends RDFObject {
		@rdf("urn:test:name")
		String getName();

		void setName(String name);

		@rdf("urn:test:nick")
		Set<String> getNicks();

		void setNicks(Set<String> nicks);

		@rdf("urn:test:parent")
		Bean getParent();

		void setParent(Bean parent);

		@rdf("urn:test:friend")
		Set<Bean> getFriends();

		void setFriends(Set<Bean> friends);
	}

	@Override
	public void setUp() throws Exception {
		module.addConcept(Bean.class);
		super.setUp();
		manager.setNamespace("test", NS);
		ValueFactory vf = manager.getValueFactory();
		manager.setAutoCommit(false);
		URI urn_root = vf.createURI(NS, "root");
		Bean root = manager.addType(manager.getObjectFactory().createRDFObject(urn_root), Bean.class);
		for (int i = 0; i < 100; i++) {
			URI uri = vf.createURI(NS, String.valueOf(i));
			Bean bean = manager.addType(manager.getObjectFactory().createRDFObject(uri), Bean.class);
			bean.setName("name" + i);
			bean.getNicks().add("nicka" + i);
			bean.getNicks().add("nickb" + i);
			bean.getNicks().add("nickc" + i);
			URI p = vf.createURI(NS, String.valueOf(i + 1000));
			Bean parent = manager.addType(manager.getObjectFactory().createRDFObject(p), Bean.class);
			parent.setName("name" + String.valueOf(i + 1000));
			bean.setParent(parent);
			for (int j = i - 10; j < i; j++) {
				if (j > 0) {
					URI f = vf.createURI(NS, String.valueOf(j + 1000));
					Bean friend = manager.addType(manager.getObjectFactory().createRDFObject(f), Bean.class);
					friend.setName("name" + String.valueOf(j + 1000));
					bean.getFriends().add(friend);
				}
			}
			root.getFriends().add(bean);
		}
		manager.setAutoCommit(true);
	}

	public void test_concept() throws Exception {
		long start = System.currentTimeMillis();
		ObjectQuery query = manager.prepareObjectQuery("SELECT ?o ?o_class ?o_name ?o_parent ?o_parent_class ?o_parent_name " +
				"WHERE {?o a ?type; a ?o_class; <urn:test:name> ?o_name; <urn:test:parent> ?o_parent ." +
				" ?o_parent a ?o_parent_class; <urn:test:name> ?o_parent_name }");
		query.setType("type", Bean.class);
		List<Bean> beans = query.evaluate(Bean.class).asList();
		for (Bean bean : beans) {
			bean.getName();
			if (bean.getParent() != null) {
				bean.getParent().getName();
			}
			for (Bean f : bean.getFriends()) {
				f.getName();
			}
		}
		long end = System.currentTimeMillis();
		System.out.println((end - start) / 1000.0);
	}

	public void test_object() throws Exception {
		long start = System.currentTimeMillis();
		ObjectQuery query = manager.prepareObjectQuery("SELECT ?o WHERE {?o a ?type}");
		query.setType("type", Bean.class);
		List<Bean> beans = (List)query.evaluate().asList();
		for (Bean bean : beans) {
			bean.getName();
			if (bean.getParent() != null) {
				bean.getParent().getName();
			}
			for (Bean f : bean.getFriends()) {
				f.getName();
			}
		}
		long end = System.currentTimeMillis();
		System.out.println((end - start) / 1000.0);
	}

	public void test_naive() throws Exception {
		ValueFactory vf = manager.getValueFactory();
		final URI Bean = vf.createURI(NS, "Bean");
		final URI name = vf.createURI(NS, "name");
		final URI parent = vf.createURI(NS, "parent");
		final URI friend = vf.createURI(NS, "friend");
		long start = System.currentTimeMillis();
		ModelResult beans = manager.match(null, RDF.TYPE, Bean);
		Statement st;
		while ((st = beans.next()) != null) {
			Resource bean = st.getSubject();
			manager.match(bean, name, null).asList();
			ModelResult match;
			Statement f;
			match = manager.match(bean, parent, null);
			while ((f = match.next())!= null) {
				manager.match((Resource)f.getObject(), name, null).asList();
			}
			match = manager.match(bean, friend, null);
			while ((f = match.next())!= null) {
				manager.match((Resource)f.getObject(), name, null).asList();
			}
		}
		long end = System.currentTimeMillis();
		System.out.println((end - start) / 1000.0);
	}
}
