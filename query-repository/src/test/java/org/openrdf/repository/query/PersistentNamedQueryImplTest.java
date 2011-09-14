package org.openrdf.repository.query;

import java.io.File;

import junit.framework.TestCase;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.event.base.NotifyingRepositoryWrapper;
import org.openrdf.repository.query.NamedQueryRepository.NamedQuery;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

public class PersistentNamedQueryImplTest extends TestCase {
	private NamedQueryRepositoryWrapper repo;
	private String NS = "http://rdf.example.org/";
	private RepositoryConnection a;
	private File dataDir ;
	private URI QUERY;

	@Override
	public void setUp() throws Exception {
		dataDir = new File("/tmp/test/") ;
		deleteDir(dataDir) ;
		dataDir.mkdir() ;
		repo = startup(dataDir) ;
		ValueFactory uf = repo.getValueFactory();
		QUERY = uf.createURI(NS, "query");
		a = repo.getConnection();		
	}
	
	@Override
	public void tearDown() throws Exception {
		shutdown();
	}

	private NamedQueryRepositoryWrapper startup(File dataDir) throws RepositoryException {
		SailRepository sail = new SailRepository(new MemoryStore(dataDir)) ;
		NamedQueryRepositoryWrapper repo ;
		repo = new NamedQueryRepositoryWrapper(new NotifyingRepositoryWrapper(sail));
		repo.initialize() ;
		return repo ;
	}
	
	private void shutdown() throws Exception {
		a.close();
		repo.shutDown();		
	}
	
	public void test_persistence() throws Exception {
		String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
		NamedQuery nq1 = repo.createNamedQuery(QUERY, QueryLanguage.SPARQL, rq1, NS);
		String eTag = nq1.getResultETag() ;
		assertEquals(repo.getNamedQuery(QUERY).getQueryString(), rq1);
		
		shutdown() ;
		repo = startup(dataDir) ;
		
		NamedQuery nq2 = repo.getNamedQuery(QUERY) ;
		assertEquals(nq2.getQueryString(), rq1);
		assertEquals(eTag, nq2.getResultETag()) ;
	}
	
	public void test_nonPersistence() throws Exception {
		String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
		NamedQuery nq1 = repo.createNamedQuery(QUERY, QueryLanguage.SPARQL, rq1, NS);
		String eTag = nq1.getResultETag() ;
		assertEquals(repo.getNamedQuery(QUERY).getQueryString(), rq1);
		
		// shut-down (desist named query) then restart the persistent repository
		shutdown() ;
		repo = startup(dataDir) ;
		
		NamedQuery nq2 = repo.getNamedQuery(QUERY) ;
		assertEquals(nq2.getQueryString(), rq1);
		assertEquals(eTag, nq2.getResultETag()) ;
		
		// cease persisting QUERY1
		repo.removeNamedQuery(QUERY) ;
		
		shutdown() ;
		repo = startup(dataDir) ;
		// the removed named query should not be persisted
		
		assertTrue(!repo.getNamedQueryURIs().hasNext()) ;
	}
	

	
	private static void deleteDir(File dir) {
		if (dir.isDirectory()) {
			File[] files = dir.listFiles() ;
			for (int i=0; i<files.length; i++) {
				if (files[i].isDirectory()) deleteDir(files[i]) ;
				files[i].delete() ;
			}
			dir.delete() ;
		}
	}
	
}
