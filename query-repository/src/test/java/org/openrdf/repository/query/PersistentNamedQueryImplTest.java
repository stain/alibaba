package org.openrdf.repository.query;

import java.io.File;

import junit.framework.TestCase;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.event.base.NotifyingRepositoryWrapper;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

public class PersistentNamedQueryImplTest extends TestCase {
	private NamedQueryRepositoryWrapper repo ;
	private String NS = "http://rdf.example.org/";
	private File dataDir ;
	private URI QUERY;

	@Override
	public void setUp() throws Exception {
		dataDir = new File("/tmp/test/") ;
		deleteDir(dataDir) ;
		dataDir.mkdir() ;
	}

	private void init(File dataDir) throws RepositoryException {
		SailRepository sail = new SailRepository(new MemoryStore(dataDir)) ;
		repo = new NamedQueryRepositoryWrapper(new NotifyingRepositoryWrapper(sail));
		
		ValueFactory vf = repo.getValueFactory();
		QUERY = vf.createURI(NS, "query");
	}
	
	public void test_persistence() throws Exception {
		init(dataDir) ;
		repo.initialize() ;
		
		String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
		repo.createNamedQuery(QUERY, QueryLanguage.SPARQL, rq1, NS);
		assertEquals(repo.getNamedQuery(QUERY).getQueryString(), rq1);
		
		repo.shutDown();
		init(dataDir) ;
		repo.initialize() ;
		
		NamedQuery nq2 = repo.getNamedQuery(QUERY) ;
		assertEquals(nq2.getQueryString(), rq1);
		
		repo.shutDown();
	}
	
	public void test_nonPersistence() throws Exception {
		init(dataDir) ;
		repo.initialize() ;

		String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
		repo.createNamedQuery(QUERY, QueryLanguage.SPARQL, rq1, NS);
		assertEquals(repo.getNamedQuery(QUERY).getQueryString(), rq1);
		
		// shut-down (desist named query) then restart the persistent repository
		repo.shutDown();
		init(dataDir) ;
		repo.initialize() ;
		
		NamedQuery nq2 = repo.getNamedQuery(QUERY) ;
		assertEquals(nq2.getQueryString(), rq1);
		
		// cease persisting QUERY1
		repo.removeNamedQuery(QUERY) ;
		
		repo.shutDown();
		init(dataDir) ;
		repo.initialize() ;
		
		// the removed named query should not be persisted
		assertTrue(repo.getNamedQueryIDs().length==0) ;
		
		repo.shutDown();
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
