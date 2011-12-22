package org.openrdf.sail.keyword;

import junit.framework.TestCase;

import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.Sail;
import org.openrdf.sail.memory.MemoryStore;

public class KeywordSailTest extends TestCase {
	private static final String PREFIX = "PREFIX rdfs:<" + RDFS.NAMESPACE + ">\n"
			+ "PREFIX keyword:<http://www.openrdf.org/rdf/2011/keyword#>\n";
	private RepositoryConnection con;
	private Repository repo;
	private ValueFactory vf;

	public void setUp() throws Exception {
		Sail sail = new KeywordSail(new MemoryStore());
		repo = new SailRepository(sail);
		repo.initialize();
		vf = repo.getValueFactory();
		con = repo.getConnection();
	}

	public void tearDown() throws Exception {
		con.close();
		repo.shutDown();
	}

	public void testFirstWord() throws Exception {
		con.add(vf.createURI("urn:test:ball"), RDFS.LABEL,
				vf.createLiteral("base ball"));
		BooleanQuery qry = con.prepareBooleanQuery(QueryLanguage.SPARQL, PREFIX
			+ "ASK { ?resource rdfs:label ?label; keyword:phone ?soundex\n"
			+ "FILTER sameTerm(?soundex, keyword:soundex($keyword))\n"
			+ "FILTER EXISTS { ?resource ?index ?term FILTER regex(?term, keyword:regex($keyword)) } }");
		qry.setBinding("keyword", vf.createLiteral("base"));
		assertTrue(qry.evaluate());
	}

	public void testLastWord() throws Exception {
		con.add(vf.createURI("urn:test:ball"), RDFS.LABEL,
				vf.createLiteral("base ball"));
		BooleanQuery qry = con.prepareBooleanQuery(QueryLanguage.SPARQL, PREFIX
			+ "ASK { ?resource rdfs:label ?label; keyword:phone ?soundex\n"
			+ "FILTER sameTerm(?soundex, keyword:soundex($keyword))\n"
			+ "FILTER EXISTS { ?resource ?index ?term FILTER regex(?term, keyword:regex($keyword)) } }");
		qry.setBinding("keyword", vf.createLiteral("ball"));
		assertTrue(qry.evaluate());
	}

	public void testFullLabel() throws Exception {
		con.add(vf.createURI("urn:test:ball"), RDFS.LABEL,
				vf.createLiteral("base ball"));
		BooleanQuery qry = con.prepareBooleanQuery(QueryLanguage.SPARQL, PREFIX
			+ "ASK { ?resource rdfs:label ?label; keyword:phone ?soundex\n"
			+ "FILTER sameTerm(?soundex, keyword:soundex($keyword))\n"
			+ "FILTER EXISTS { ?resource ?index ?term FILTER regex(?term, keyword:regex($keyword)) } }");
		qry.setBinding("keyword", vf.createLiteral("base ball"));
		assertTrue(qry.evaluate());
	}

	public void testTooLong() throws Exception {
		con.add(vf.createURI("urn:test:ball"), RDFS.LABEL,
				vf.createLiteral("base ball"));
		BooleanQuery qry = con.prepareBooleanQuery(QueryLanguage.SPARQL, PREFIX
			+ "ASK { ?resource rdfs:label ?label; keyword:phone ?soundex\n"
			+ "FILTER sameTerm(?soundex, keyword:soundex($keyword))\n"
			+ "FILTER EXISTS { ?resource ?index ?term FILTER regex(?term, keyword:regex($keyword)) } }");
		qry.setBinding("keyword", vf.createLiteral("base ball bat"));
		assertFalse(qry.evaluate());
	}
}
