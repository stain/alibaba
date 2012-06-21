package org.openrdf.repository.auditing;


import java.io.OutputStream;
import java.util.Arrays;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import junit.framework.TestCase;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.trig.TriGWriter;
import org.openrdf.sail.Sail;
import org.openrdf.sail.auditing.AuditingSail;
import org.openrdf.sail.memory.MemoryStore;

public class AuditingTest extends TestCase {
	public static final URI ACTIVITY = new URIImpl("http://www.w3.org/ns/prov#Activity");
	public static final URI RECENT = new URIImpl("http://www.openrdf.org/rdf/2012/auditing#RecentActivity");
	public static final URI OBSOLETE = new URIImpl("http://www.openrdf.org/rdf/2012/auditing#ObsoleteActivity");
	public static final URI ENDED_AT = new URIImpl("http://www.w3.org/ns/prov#endedAtTime");
	public static final URI GENERATED_BY = new URIImpl("http://www.w3.org/ns/prov#wasGeneratedBy");
	public static final URI CAHNGED = new URIImpl("http://www.openrdf.org/rdf/2012/auditing#changed");
	public static final URI INFORMED_BY = new URIImpl("http://www.w3.org/ns/prov#wasInformedBy");
	public static final URI USED = new URIImpl("http://www.w3.org/ns/prov#used");
	private ValueFactory vf = ValueFactoryImpl.getInstance();
	private String NS = "http://example.com/";
	private URI carmichael = vf.createURI(NS, "carmichael");
	private URI harris = vf.createURI(NS, "harris");
	private URI jackson = vf.createURI(NS, "jackson");
	private URI johnston = vf.createURI(NS, "johnston");
	private URI lismer = vf.createURI(NS, "lismer");
	private URI macDonald = vf.createURI(NS, "macDonald");
	private URI varley = vf.createURI(NS, "varley");
	private URI thomson = vf.createURI(NS, "thomson");
	private URI knows = vf.createURI("http://xmlns.com/foaf/0.1/knows");
	private URI lastActivity;
	private int activityNumber;
	private AuditingRepositoryConnection con;
	private AuditingRepository repo;

	private AuditingRepositoryConnection reopen(AuditingRepository repo,
			AuditingRepositoryConnection con) throws Exception {
		con = commit(repo, con);
		begin(con);
		return con;
	}

	private void begin(AuditingRepositoryConnection conn)
			throws RepositoryException {
		conn.setAutoCommit(false);
		lastActivity = vf.createURI(NS, "activity" + (++activityNumber));
		conn.setInsertContext(lastActivity);
	}

	private AuditingRepositoryConnection commit(AuditingRepository repo,AuditingRepositoryConnection conn)
			throws Exception {
		DatatypeFactory df = DatatypeFactory.newInstance();
		XMLGregorianCalendar now = df.newXMLGregorianCalendar(new GregorianCalendar());
		conn.add(lastActivity, ENDED_AT, vf.createLiteral(now), lastActivity);
		conn.add(lastActivity, RDF.TYPE, ACTIVITY, lastActivity);
		conn.setAutoCommit(true);
		conn.close();
		return repo.getConnection();
	}

	private void dump(OutputStream out) throws RepositoryException, RDFHandlerException {
		for (Resource ctx : con.getContextIDs().asList()) {
			con.export(new TriGWriter(System.out), ctx);
		}
	}

	public void setUp() throws Exception {
		Sail sail = new MemoryStore();
		sail = new AuditingSail(sail);
		Repository r = new SailRepository(sail);
		repo = new AuditingRepository(r);
		repo.initialize();
		con = repo.getConnection();
		activityNumber = 0;
	}

	public void tearDown() throws Exception {
		con.close();
		repo.shutDown();
	}

	public void testAdd() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = commit(repo, con);
		assertTrue(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertFalse(con.hasStatement(null, null, null, false, new Resource[]{null}));
		assertEquals(Arrays.asList(lastActivity), con.getContextIDs().asList());
		assertTrue(con.hasStatement(null, RDF.TYPE, ACTIVITY, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertFalse(con.hasStatement(null, INFORMED_BY, null, false));
		assertFalse(con.hasStatement(null, CAHNGED, null, false));
	}

	public void testAddMany() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.add(harris, knows, jackson);
		con = reopen(repo, con);
		con.add(jackson, knows, johnston);
		con = reopen(repo, con);
		con.add(johnston, knows, lismer);
		con = reopen(repo, con);
		con.add(lismer, knows, macDonald);
		con = reopen(repo, con);
		con.add(macDonald, knows, varley);
		con = reopen(repo, con);
		con.add(varley, knows, thomson);
		con = commit(repo, con);
		assertTrue(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertFalse(con.hasStatement(null, null, null, false, new Resource[]{null}));
		assertEquals(7, con.getContextIDs().asList().size());
		assertTrue(con.hasStatement(null, RDF.TYPE, ACTIVITY, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertFalse(con.hasStatement(null, INFORMED_BY, null, false));
		assertFalse(con.hasStatement(null, CAHNGED, null, false));
		assertEquals(0, con.getStatements(null, RDF.TYPE, RECENT, false).asList().size());
	}

	public void testRemove() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.remove(carmichael, knows, harris);
		con = commit(repo, con);
		assertFalse(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertEquals(2, con.getContextIDs().asList().size());
		assertTrue(con.hasStatement(null, RDF.TYPE, ACTIVITY, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertTrue(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertTrue(con.hasStatement(null, INFORMED_BY, null, false));
		assertTrue(con.hasStatement(null, CAHNGED, null, false));
	}

	public void testRemoveMany() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con.add(harris, knows, jackson);
		con.add(jackson, knows, johnston);
		con.add(johnston, knows, lismer);
		con.add(lismer, knows, macDonald);
		con.add(macDonald, knows, varley);
		con.add(varley, knows, thomson);
		con = reopen(repo, con);
		con.remove((Resource)null, knows, null);
		con = commit(repo, con);
		assertFalse(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertFalse(con.hasStatement(null, null, null, false, new Resource[]{null}));
		assertEquals(2, con.getContextIDs().asList().size());
		assertTrue(con.hasStatement(null, RDF.TYPE, ACTIVITY, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertTrue(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertTrue(con.hasStatement(null, INFORMED_BY, null, false));
		assertFalse(con.hasStatement(null, CAHNGED, null, false));
		assertEquals(0, con.getStatements(null, RDF.TYPE, RECENT, false).asList().size());
	}

	public void testRemoveAdd() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.remove(carmichael, knows, harris);
		con = reopen(repo, con);
		con.add(carmichael, knows, jackson);
		con = commit(repo, con);
		assertFalse(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, knows, jackson, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertEquals(3, con.getContextIDs().asList().size());
		assertTrue(con.hasStatement(null, RDF.TYPE, ACTIVITY, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertTrue(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertTrue(con.hasStatement(null, INFORMED_BY, null, false));
		assertTrue(con.hasStatement(null, CAHNGED, null, false));
	}

	public void testReplace() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.remove(carmichael, knows, harris);
		con.add(carmichael, knows, jackson);
		con = commit(repo, con);
		assertFalse(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, knows, jackson, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertEquals(2, con.getContextIDs().asList().size());
		assertTrue(con.hasStatement(null, RDF.TYPE, ACTIVITY, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertTrue(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertTrue(con.hasStatement(null, INFORMED_BY, null, false));
		assertTrue(con.hasStatement(null, CAHNGED, null, false));
	}

	public void testRemoveEach() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		RepositoryResult<Statement> stmts = con.getStatements(carmichael, null, null, false);
		while (stmts.hasNext()) {
			con.remove(stmts.next());
		}
		stmts.close();
		con = commit(repo, con);
		assertFalse(con.hasStatement(carmichael, null, null, false));
		assertEquals(2, con.getContextIDs().asList().size());
		assertTrue(con.hasStatement(null, RDF.TYPE, ACTIVITY, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertTrue(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertTrue(con.hasStatement(null, INFORMED_BY, null, false));
		assertFalse(con.hasStatement(null, CAHNGED, null, false));
	}

	public void testRemoveRevision() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.remove(carmichael, GENERATED_BY, null);
		con = commit(repo, con);
		assertTrue(con.hasStatement(carmichael, knows, harris, false));
		assertFalse(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertEquals(2, con.getContextIDs().asList().size());
		assertTrue(con.hasStatement(null, RDF.TYPE, ACTIVITY, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertTrue(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertTrue(con.hasStatement(null, INFORMED_BY, null, false));
		assertFalse(con.hasStatement(null, CAHNGED, null, false));
	}

	public void testRemoveLastRevision() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		RepositoryResult<Statement> stmts = con.getStatements(carmichael, GENERATED_BY, null, false);
		Value revision = stmts.next().getObject();
		stmts.close();
		con.remove(carmichael, GENERATED_BY, revision);
		con = commit(repo, con);
		assertTrue(con.hasStatement(carmichael, knows, harris, false));
		assertFalse(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertEquals(2, con.getContextIDs().asList().size());
		assertTrue(con.hasStatement(null, RDF.TYPE, ACTIVITY, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertTrue(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertTrue(con.hasStatement(null, INFORMED_BY, null, false));
		assertFalse(con.hasStatement(null, CAHNGED, null, false));
	}

	public void testTouchRevision() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.add(lastActivity, USED, carmichael);
		con = commit(repo, con);
		assertTrue(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertEquals(2, con.getContextIDs().asList().size());
		assertTrue(con.hasStatement(null, RDF.TYPE, ACTIVITY, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertFalse(con.hasStatement(null, INFORMED_BY, null, false));
		assertFalse(con.hasStatement(null, CAHNGED, null, false));
	}

	public void testDoubleTouchRevision() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.remove(carmichael, GENERATED_BY, null);
		con.add(carmichael, GENERATED_BY, lastActivity);
		con = reopen(repo, con);
		con.remove(carmichael, GENERATED_BY, null);
		con.add(carmichael, GENERATED_BY, lastActivity);
		con = commit(repo, con);
		assertTrue(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertEquals(3, con.getContextIDs().asList().size());
		assertTrue(con.hasStatement(null, RDF.TYPE, ACTIVITY, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertTrue(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertTrue(con.hasStatement(null, INFORMED_BY, null, false));
		assertFalse(con.hasStatement(null, CAHNGED, null, false));
	}

	public void testUpgrade() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.remove(carmichael, null, null);
		con.add(carmichael, knows, jackson);
		con = commit(repo, con);
		assertTrue(con.hasStatement(carmichael, knows, jackson, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertEquals(2, con.getContextIDs().asList().size());
		assertTrue(con.hasStatement(null, RDF.TYPE, ACTIVITY, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertTrue(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertTrue(con.hasStatement(null, INFORMED_BY, null, false));
		assertTrue(con.hasStatement(null, CAHNGED, null, false));
	}

	public void testClear() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.remove(carmichael, null, null);
		con = commit(repo, con);
		assertFalse(con.hasStatement(carmichael, null, null, false));
		assertEquals(2, con.getContextIDs().asList().size());
		assertTrue(con.hasStatement(null, RDF.TYPE, ACTIVITY, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertTrue(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertTrue(con.hasStatement(null, INFORMED_BY, null, false));
		assertTrue(con.hasStatement(null, CAHNGED, null, false));
	}

	public void testInsertData() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.prepareUpdate(QueryLanguage.SPARQL, "INSERT DATA { <carmichael> <http://xmlns.com/foaf/0.1/knows> <harris> } ", "http://example.com/").execute();
		con = commit(repo, con);
		assertTrue(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertFalse(con.hasStatement(null, null, null, false, new Resource[]{null}));
		assertEquals(Arrays.asList(lastActivity), con.getContextIDs().asList());
		assertTrue(con.hasStatement(null, RDF.TYPE, ACTIVITY, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertFalse(con.hasStatement(null, INFORMED_BY, null, false));
		assertFalse(con.hasStatement(null, CAHNGED, null, false));
	}

	public void testDeleteData() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.prepareUpdate(QueryLanguage.SPARQL, "DELETE DATA { <carmichael> <http://xmlns.com/foaf/0.1/knows> <harris> } ", "http://example.com/").execute();
		con = commit(repo, con);
		assertFalse(con.hasStatement(carmichael, knows, harris, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertEquals(2, con.getContextIDs().asList().size());
		assertTrue(con.hasStatement(null, RDF.TYPE, ACTIVITY, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertTrue(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertTrue(con.hasStatement(null, INFORMED_BY, null, false));
		assertTrue(con.hasStatement(null, CAHNGED, null, false));
	}

	public void testDelete() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.prepareUpdate(QueryLanguage.SPARQL, "DELETE { <carmichael> ?p ?o }\n" +
				"WHERE { <carmichael> ?p ?o } ", "http://example.com/").execute();
		con = commit(repo, con);
		assertFalse(con.hasStatement(carmichael, null, null, false));
		assertEquals(2, con.getContextIDs().asList().size());
		assertTrue(con.hasStatement(null, RDF.TYPE, ACTIVITY, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertTrue(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertTrue(con.hasStatement(null, INFORMED_BY, null, false));
		assertTrue(con.hasStatement(null, CAHNGED, null, false));
	}

	public void testModify() throws Exception {
		begin(con);
		assertTrue(con.isEmpty());
		con.add(carmichael, knows, harris);
		con = reopen(repo, con);
		con.prepareUpdate(QueryLanguage.SPARQL, "DELETE { <carmichael> ?p ?o }\n" +
				"INSERT { <carmichael> <http://xmlns.com/foaf/0.1/knows> <jackson> }\n" +
				"WHERE { <carmichael> ?p ?o } ", "http://example.com/").execute();
		con = commit(repo, con);
		assertTrue(con.hasStatement(carmichael, knows, jackson, false));
		assertTrue(con.hasStatement(carmichael, GENERATED_BY, null, false));
		assertEquals(2, con.getContextIDs().asList().size());
		assertTrue(con.hasStatement(null, RDF.TYPE, ACTIVITY, false));
		assertFalse(con.hasStatement(null, RDF.TYPE, RECENT, false));
		assertTrue(con.hasStatement(null, RDF.TYPE, OBSOLETE, false));
		assertTrue(con.hasStatement(null, ENDED_AT, null, false));
		assertTrue(con.hasStatement(null, INFORMED_BY, null, false));
		assertTrue(con.hasStatement(null, CAHNGED, null, false));
	}
}
