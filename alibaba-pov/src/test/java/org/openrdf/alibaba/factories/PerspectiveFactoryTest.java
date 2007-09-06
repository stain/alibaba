package org.openrdf.alibaba.factories;

import java.net.URL;
import java.util.Properties;

import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.openrdf.alibaba.pov.Intent;
import org.openrdf.alibaba.pov.PerspectiveRepository;
import org.openrdf.alibaba.vocabulary.ALI;
import org.openrdf.elmo.ElmoManager;
import org.openrdf.elmo.sesame.SesameManagerFactory;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.memory.MemoryStore;

public class PerspectiveFactoryTest extends TestCase {
	private static final String POVS_PROPERTIES = "META-INF/org.openrdf.alibaba.povs";

	private Repository repository;

	private ElmoManager manager;

	public void testCreate() throws Exception {
		PerspectiveFactory factory;
		factory = (PerspectiveFactory) manager.find(ALI.PERSPECTIVE_FACTORY);
		Intent intent = (Intent) manager.find(ALI.GENERAL);
		PerspectiveRepository repository;
		QName name = ALI.PERSPECTIVE_REPOSITORY;
		repository = (PerspectiveRepository) manager.find(name);
		factory.createPerspectiveFor(intent, repository);
	}

	@Override
	protected void setUp() throws Exception {
		repository = new SailRepository(new MemoryStore());
		repository.initialize();
		RepositoryConnection conn = repository.getConnection();
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		URL list = cl.getResource(POVS_PROPERTIES);
		Properties prop = new Properties();
		prop.load(list.openStream());
		for (Object res : prop.keySet()) {
			URL url = cl.getResource(res.toString());
			RDFFormat format = RDFFormat.forFileName(url.getFile());
			conn.add(url, "", format);
		}
		conn.close();
		manager = new SesameManagerFactory(repository).createElmoManager();
	}

	@Override
	protected void tearDown() throws Exception {
		manager.close();
		repository.shutDown();
	}
}
