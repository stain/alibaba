package org.openrdf.repository.query.config;

import java.io.File;

import junit.framework.TestCase;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.DelegatingRepository;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.base.RepositoryWrapper;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.event.base.NotifyingRepositoryWrapper;
import org.openrdf.repository.query.DelegatingNamedQueryRepository;
import org.openrdf.repository.query.NamedQuery;
import org.openrdf.repository.query.NamedQueryRepository;
import org.openrdf.repository.query.NamedQueryRepositoryWrapper;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

public class NamedQueryRepositoryFactoryTest extends TestCase {
	
	private NamedQueryRepository repo ;
	private Repository notifier ;
	private String NS = "http://rdf.example.org/";
	private URI QUERY1, QUERY2;
	private NamedQueryRepositoryFactory factory ;
	private SailRepository sail;

	@Override
	public void tearDown() throws Exception {
		repo.shutDown();
	}
	
	private void init(File dataDir) throws RepositoryException, RepositoryConfigException {
		factory = new NamedQueryRepositoryFactory() ;
		MemoryStore store = dataDir==null ? new MemoryStore() : new MemoryStore(dataDir) ;
		sail = new SailRepository(store) ;
		notifier = new NotifyingRepositoryWrapper(sail) ;
				
		ValueFactory vf = sail.getValueFactory();
		QUERY1 = vf.createURI(NS, "query1");
		QUERY2 = vf.createURI(NS, "query2");
	}

	private void functionalTest() throws Exception {
		String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
		NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
		assertEquals(nq1.getQueryString(), rq1);
		
		NamedQuery nq2 = repo.createNamedQuery(QUERY2, QueryLanguage.SPARQL, rq1, NS);
		assertEquals(nq2, repo.getNamedQuery(QUERY2)) ;
		
		assertTrue(repo.getNamedQueryIDs().length==2) ;
	}
	
	/* The null case returns an unconfigured NamedQueryRepositoryWrapper */
	
	public void test_NullCase() throws Exception {
		init(null) ;
		
		repo = factory.createRepository(null) ;

		((DelegatingRepository) repo).setDelegate(notifier);
		repo.initialize() ;
		functionalTest();
	}
	
	/* The effect of the factory is void if the delegate is already a named query repository */
	
	public void test_VoidCase() throws Exception {
		init(null) ;
		NamedQueryRepository named = factory.createRepository(notifier) ;		
		repo = factory.createRepository(named) ;
		repo.initialize() ;
		
		assertTrue(repo == named) ;
		functionalTest();
	}
	
	/* Wrap the notifying repository with NamedQueryRepositoryWrapper */
	
	public void test_WrapNotifyingCase() throws Exception {
		init(null); 
		repo = factory.createRepository(notifier) ;
		repo.initialize() ;
		
		assertTrue(repo instanceof NamedQueryRepositoryWrapper) ;
		functionalTest();
	}
	
	/* Wrap the nested notifying repository with NamedQueryRepositoryWrapper */
	
	public void test_WrapNestedNotifyingCase() throws Exception {
		init(null); 
		repo = factory.createRepository(new RepositoryWrapper(notifier)) ;
		repo.initialize() ;
		
		assertTrue(repo instanceof NamedQueryRepositoryWrapper) ;
		functionalTest();
	}
	
	/* Wrap the non-notifying sail repository with 
	 * NotifyingRepositoryWrapper & NamedQueryRepositoryWrapper
	 */

	public void test_WrapNonNotifyingCase() throws Exception {
		init(null);
		repo = factory.createRepository(sail) ;
		repo.initialize() ;
		
		assertTrue(repo instanceof NamedQueryRepositoryWrapper) ;
		functionalTest();
	}
	
	/* Delegate to the NamedQueryRepository nested within the dummy RepositoryWrapper */
	
	public void test_DelegatingCase() throws Exception {
		init(null);
		NamedQueryRepository nested = factory.createRepository(sail) ;
		repo = factory.createRepository(new RepositoryWrapper(nested)) ;
		repo.initialize() ;
		
		assertTrue(repo instanceof DelegatingNamedQueryRepository) ;
		functionalTest();
	}
	
	/* getConfig() returns a Repository wrapper created using the null constructor
	 * Use setters to initialize a notifier
	 */
	
	public void test_setNotifying() throws Exception {
		init(null) ;
		repo = factory.getRepository(factory.getConfig()) ;
		((RepositoryWrapper) repo).setDelegate(notifier) ;
		repo.initialize() ;
		functionalTest() ;
	}

	
	/* getConfig() returns a Repository wrapper created using the null constructor
	 * Use setters to initialize a nested notifier
	 */
	
	public void test_setNestedNotifying() throws Exception {
		init(null) ;
		repo = factory.getRepository(factory.getConfig()) ;
		((RepositoryWrapper) repo).setDelegate(new RepositoryWrapper(notifier)) ;
		repo.initialize() ;
		functionalTest() ;
	}
	
	/* getConfig() returns a Repository wrapper created using the null constructor
	 * Use setters to initialize a non-notifier
	 */
	
	public void test_setNonNotifying() throws Exception {
		init(null) ;
		repo = factory.getRepository(factory.getConfig()) ;
		((RepositoryWrapper) repo).setDelegate(sail) ;
		repo.initialize() ;
		functionalTest() ;
	}
	
}
