package org.openrdf.alibaba.servlet.impl;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.openrdf.alibaba.servlet.helpers.MockRequest;
import org.openrdf.alibaba.servlet.helpers.MockResponse;
import org.openrdf.alibaba.vocabulary.ALI;
import org.openrdf.elmo.sesame.SesameManagerFactory;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.realiser.StatementRealiserRepository;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.memory.MemoryStore;

public class AlibabaStateManagerTest extends TestCase {

	private static final String POVS_PROPERTIES = "META-INF/org.openrdf.alibaba.povs";

	private static final String DECORS_PROPERTIES = "META-INF/org.openrdf.alibaba.decors";

	private Repository repository;

	private AlibabaStateManager manager;

	public void testRetrive() throws Exception {
		MockRequest req = new MockRequest();
		MockResponse resp = new MockResponse();
		QName repo = ALI.PRESENTATION_REPOSITORY;
		String uri = repo.getNamespaceURI() + repo.getLocalPart();
		req.setRequestURL("http://localhost/?uri=" + uri);
		req.putParameter("uri", uri);
		req.setPathInfo("/");
		req.setMethod("GET");
		req.setHeader("Accept", "application/vnd.mozilla.xul+xml");
		HttpResponse response = new HttpResponse(req, resp);
		response.setUrlResolver(new HttpUrlResolver(true, "", null));
		manager.retrieve(repo, response, ALI.GENERAL);
	}

	@Override
	protected void setUp() throws Exception {
		MemoryStore store = new MemoryStore();
		repository = new SailRepository(store);
		repository = new StatementRealiserRepository(repository);
		repository.initialize();
		RepositoryConnection conn = repository.getConnection();
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		loadPropertyKeysAsResource(conn, cl, POVS_PROPERTIES);
		loadPropertyKeysAsResource(conn, cl, DECORS_PROPERTIES);
		conn.close();
		manager = new AlibabaStateManager();
		manager.setElmoManagerFactory(new SesameManagerFactory(repository));
	}

	@Override
	protected void tearDown() throws Exception {
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
}
