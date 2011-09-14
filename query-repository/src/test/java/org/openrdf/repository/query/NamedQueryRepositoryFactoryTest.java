package org.openrdf.repository.query;

import java.util.Iterator;

import junit.framework.TestCase;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.event.base.NotifyingRepositoryWrapper;
import org.openrdf.repository.query.NamedQueryRepository.NamedQuery;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.repository.DelegatingRepository;

public class NamedQueryRepositoryFactoryTest extends TestCase {
	
	private NamedQueryRepository repo ;
	private SailRepository sail ;
	private Repository notifier ;
	private String NS = "http://rdf.example.org/";
	private URI QUERY1, QUERY2;
	private NamedQueryRepositoryFactory factory ;

	@Override
	public void setUp() throws Exception {
		factory = new NamedQueryRepositoryFactory() ;
		sail = new SailRepository(new MemoryStore()) ;
		notifier = new NotifyingRepositoryWrapper(sail) ;
		ValueFactory vf = notifier.getValueFactory();
		QUERY1 = vf.createURI(NS, "query1");
		QUERY2 = vf.createURI(NS, "query2");
	}

	@Override
	public void tearDown() throws Exception {
		repo.shutDown();
	}

	private void functionalTest() 
	throws MalformedQueryException, RepositoryException {
		String rq1 = "SELECT ?painting WHERE { [a <Painter>] <paints> ?painting }";
		NamedQuery nq1 = repo.createNamedQuery(QUERY1, QueryLanguage.SPARQL, rq1, NS);
		assertEquals(nq1.getQueryString(), rq1);
		
		NamedQuery nq2 = repo.createNamedQuery(QUERY2, QueryLanguage.SPARQL, rq1, NS);
		assertEquals(nq2, repo.getNamedQuery(QUERY2)) ;

		Iterator<URI> i = repo.getNamedQueryURIs();
		assertEquals(QUERY1, i.next());
		assertEquals(QUERY2, i.next());
	}
	
	public void test_NullCase() throws Exception {
		// The null case returns an unconfigured NamedQueryRepositoryWrapper
		repo = factory.createRepository(null) ;
		((DelegatingRepository) repo).setDelegate(notifier);
		repo.initialize() ;
		functionalTest();
	}	
	
	public void test_VoidCase() throws Exception {
		// The effect of the factory is void if the delegate is a named query repository
		NamedQueryRepository delegate = factory.createRepository(notifier) ;		
		repo = factory.createRepository(delegate) ;
		repo.initialize() ;
		
		assertTrue(repo == delegate) ;
		functionalTest();
	}
	
	public void test_WrapNotifyingCase() throws Exception {
		// Wraps the notifying repository with NamedQueryRepositoryWrapper
		repo = factory.createRepository(notifier) ;
		repo.initialize() ;
		
		assertTrue(repo instanceof NamedQueryRepositoryWrapper) ;
		functionalTest();
	}

	public void test_WrapNonNotifyingCase() throws Exception {
		// Wraps the non-notifying sail repository with 
		// NotifyingRepositoryWrapper & NamedQueryRepositoryWrapper
		repo = factory.createRepository(sail) ;
		repo.initialize() ;
		
		assertTrue(repo instanceof NamedQueryRepositoryWrapper) ;
		functionalTest();
	}
	
	public void test_DelegatingCase() throws Exception {
		// Delegates to the nested NamedQueryRepository

		NamedQueryRepository nested = factory.createRepository(sail) ;
		Repository delegate = new NotifyingRepositoryWrapper(nested) ;
		repo = factory.createRepository(delegate) ;
		repo.initialize() ;
		
		assertTrue(repo instanceof DelegatingNamedQueryRepository) ;
		functionalTest();
	}	
	
}
