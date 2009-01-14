package org.openrdf.elmo.sesame;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import junit.framework.Test;

import org.openrdf.elmo.ElmoManager;
import org.openrdf.elmo.ElmoModule;
import org.openrdf.elmo.sesame.UserGuideTest.Employee;
import org.openrdf.elmo.sesame.UserGuideTest.Engineer;
import org.openrdf.elmo.sesame.UserGuideTest.EngineerBonusBehaviour;
import org.openrdf.elmo.sesame.UserGuideTest.Salesman;
import org.openrdf.elmo.sesame.UserGuideTest.SalesmanBonusBehaviour;
import org.openrdf.elmo.sesame.base.RepositoryTestCase;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.algebra.Join;
import org.openrdf.query.algebra.Projection;
import org.openrdf.query.algebra.ProjectionElem;
import org.openrdf.query.algebra.ProjectionElemList;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.algebra.StatementPattern.Scope;
import org.openrdf.query.parser.ParsedGraphQuery;
import org.openrdf.query.parser.ParsedQuery;
import org.openrdf.query.parser.QueryParserUtil;
import org.openrdf.query.parser.serqo.SeRQOFormatter;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.augur.AugurRepository;
import org.openrdf.repository.readahead.ReadAheadRepository;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.threadproxy.ThreadProxyRepository;
import org.openrdf.sail.Sail;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;
import org.openrdf.sail.memory.MemoryStore;

public class AugurTest extends RepositoryTestCase {
	private static final String NS = "http://www.example.com/rdf/2007/";

	public static Test suite() throws Exception {
		return RepositoryTestCase.suite(AugurTest.class);
	}

	private SesameManagerFactory factory;

	private ElmoManager manager;

	int count;

	public void testJoinQuery() throws Exception {
		ValueFactory vf = repository.getValueFactory();
		RepositoryConnection conn = repository.getConnection();
		try {
			// populate repository
			URI subj = vf.createURI("http://www.example.com/rdf/2007/E340076");
			URI emp = vf.createURI("http://www.example.com/rdf/2007/Employee");
			URI sales = vf.createURI("http://www.example.com/rdf/2007/Salesman");
			URI common = vf.createURI("http://www.example.com/rdf/2007/Period#common");
			URI p1 = vf.createURI("http://www.example.com/rdf/2007/Period#1");
			conn.add(subj, RDF.TYPE, emp, common);
			conn.add(subj, RDF.TYPE, sales, p1);

			// create query to use named contexts
			String queryString = "SELECT DISTINCT subj FROM CONTEXT <"
					+ common
					+ "> {subj} rdf:type {<http://www.example.com/rdf/2007/Salesman>} UNION SELECT subj FROM CONTEXT <"
					+ common
					+ "> {subj} rdf:type {<http://www.example.com/rdf/2007/Engineer>} UNION SELECT subj FROM CONTEXT <"
					+ common
					+ "> {subj} rdf:type {<http://www.example.com/rdf/2007/Employee>}";

			// join a statement pattern to select all the rdf:type statements of
			// the subject
			ParsedQuery query = QueryParserUtil.parseQuery(QueryLanguage.SERQL, queryString, "");
			Var subjVar = new Var("subj", subj);
			Var predVar = new Var("pred", RDF.TYPE);
			Var objVar = new Var("obj", null);
			Scope scope = Scope.NAMED_CONTEXTS;
			Var ctxVar = new Var("ctx", common);
			StatementPattern sp = new StatementPattern(scope, subjVar, predVar, objVar, ctxVar);
			TupleExpr tupleExpr = new Join(query.getTupleExpr(), sp);
			
			// convert the tuple query into a graph query
			ProjectionElem projSubj = new ProjectionElem("subj", "subject");
			ProjectionElem projPred = new ProjectionElem("pred", "predicate");
			ProjectionElem projObj = new ProjectionElem("obj", "object");
			ProjectionElem projCtx = new ProjectionElem("ctx", "context");
			ProjectionElemList projElemList = new ProjectionElemList(projSubj, projPred, projObj, projCtx);
			Projection proj = new Projection(tupleExpr, projElemList);
			ParsedGraphQuery newQuery = new ParsedGraphQuery(proj);
			
			// serialise the query into serqo
			SeRQOFormatter formatter = new SeRQOFormatter();
			String newQueryString = formatter.formatGraphQuery(newQuery);
			GraphQuery graphQuery = conn.prepareGraphQuery(QueryLanguage.SERQO, newQueryString, null);
			System.out.print(graphQuery.toString());
			
			// should have return one {subj} rdf:type {emp} {common} statement from the repository
			GraphQueryResult results = graphQuery.evaluate();
			assertTrue(results.hasNext()); // This works in revision 3298
		} finally {
			conn.close();
		}
	}

	public void testContextSpecificData() throws Exception {
		Repository repo = new AugurRepository(repository);
		Logger logger = Logger.getLogger(AugurRepository.class.getName());
		ConsoleHandler handler = new ConsoleHandler();
		logger.addHandler(handler);
		handler.setLevel(Level.FINE);
		logger.setLevel(Level.FINE);
		QName c = new QName(NS, "Period#common");
		QName p1 = new QName(NS, "Period#1");
		QName p2 = new QName(NS, "Period#2");
		ElmoModule module = new ElmoModule();
		module.addConcept(Employee.class);
		module.addConcept(Salesman.class);
		module.addConcept(Engineer.class);
		module.addBehaviour(SalesmanBonusBehaviour.class, NS + "Salesman");
		module.addBehaviour(EngineerBonusBehaviour.class, NS + "Engineer");
		module.setGraph(c);
		ElmoModule m1 = new ElmoModule().setGraph(p1).includeModule(module);
		ElmoModule m2 = new ElmoModule().setGraph(p2).includeModule(module);
		SesameManagerFactory factory = new SesameManagerFactory(module, repo);
		SesameManagerFactory f1 = new SesameManagerFactory(m1, repo);
		SesameManagerFactory f2 = new SesameManagerFactory(m2, repo);
		ElmoManager common = factory.createElmoManager();
		ElmoManager period1 = f1.createElmoManager();
		ElmoManager period2 = f2.createElmoManager();
		try {
			Employee emp;
			QName id = new QName(NS, "E340076");
			emp = common.designate(id, Employee.class);
			emp.setName("John");
			Salesman slm = period1.designate(id, Salesman.class);
			slm.setTargetUnits(10);
			slm.setUnitsSold(15);
			slm.setSalary(90);
			Engineer eng = period2.designate(id, Engineer.class);
			eng.setBonusTargetMet(true);
			eng.setSalary(100);

			//testJoinQuery();

			for (Object obj : common.findAll(Employee.class)) {
				assertTrue(obj instanceof Employee);
				assertFalse(obj instanceof Salesman);
				assertFalse(obj instanceof Engineer);
				emp = (Employee) obj;
				assertEquals("John", emp.getName());
				assertEquals(0.0, emp.getSalary(), 0);
			}

			for (Object obj : period1.findAll(Employee.class)) {
				assertTrue(obj instanceof Employee);
				assertTrue(obj instanceof Salesman);
				assertFalse(obj instanceof Engineer);
				emp = (Employee) obj;
				assertEquals("John", emp.getName());
				assertEquals(90.0, emp.getSalary(), 0);
				assertEquals(6.75, emp.calculateExpectedBonus(0.05), 0);
			}

			for (Object obj : period2.findAll(Employee.class)) {
				assertTrue(obj instanceof Employee);
				assertFalse(obj instanceof Salesman);
				assertTrue(obj instanceof Engineer);
				emp = (Employee) obj;
				assertEquals("John", emp.getName());
				assertEquals(100.0, emp.getSalary(), 0);
				assertEquals(5, emp.calculateExpectedBonus(0.05), 0);
			}
		} finally {
			common.close();
			period1.close();
			period2.close();
		}
	}

	public void testAugurRepository() throws Exception {
		final Sail inner;
		if (repository instanceof SailRepository) {
			inner = ((SailRepository)repository).getSail();
		} else {
			inner = new MemoryStore();
			inner.initialize();
		}
		final ClassLoader cl = Thread.currentThread().getContextClassLoader();
		Class<?>[] classes = new Class<?>[] { Sail.class };
		InvocationHandler sailHandler = new InvocationHandler() {
			public Object invoke(Object proxy, Method method, Object[] args)
					throws Throwable {
				if (method.getName().equals("getConnection"))
					return getConnection();
				return method.invoke(inner, args);
			}

			public SailConnection getConnection() throws SailException {
				final SailConnection delegate = inner.getConnection();
				Class<?>[] classes = new Class<?>[] { SailConnection.class };
				InvocationHandler handler = new InvocationHandler() {
					public Object invoke(Object proxy, Method method,
							Object[] args) throws Throwable {
						count++;
						return method.invoke(delegate, args);
					}
				};
				return (SailConnection) Proxy.newProxyInstance(cl, classes,
						handler);
			}
		};
		Sail sail = (Sail) Proxy.newProxyInstance(cl, classes, sailHandler);
		repository = new SailRepository(sail); 
		repository = new AugurRepository(repository);
		repository = new ReadAheadRepository(repository);
		repository = new ThreadProxyRepository(repository);
		ElmoModule module = new ElmoModule();
		module.addConcept(Employee.class);
		factory = new SesameManagerFactory(module, repository);
		manager = factory.createElmoManager();

		manager.getTransaction().begin();
		for (int i = 0; i < 100; i++) {
			Employee emp = manager.create(Employee.class);
			emp.setName("Emp" + i);
			emp.setAddress(i + " street");
			emp.setPhoneNumber("555-" + i + i);
			emp.setEmailAddress("emp" + i + "@example.com");
		}
		manager.getTransaction().commit();

		count = 0;
		Iterable<Employee> beans = manager.findAll(Employee.class);
		for (Employee emp : beans) {
			String name = emp.getName();
			String address = emp.getAddress();
			String phone = emp.getPhoneNumber();
			String email = emp.getEmailAddress();
			String details = name + address + phone + email;
			assertNotNull(details);
		}
		assertEquals(2, count);
	}

	@Override
	protected void tearDown() throws Exception {
		if (manager != null)
			manager.close();
		if (factory != null) {
			factory.close();
		}
		super.tearDown();
	}
}
