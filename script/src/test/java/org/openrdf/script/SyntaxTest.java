package org.openrdf.script;

import junit.framework.TestCase;

import org.openrdf.script.ast.ASTBody;
import org.openrdf.script.ast.ParseException;
import org.openrdf.script.ast.SyntaxTreeBuilder;
import org.openrdf.script.ast.TokenMgrError;

public class SyntaxTest extends TestCase {

	public void test_base() throws Exception {
		parsable("base <urn:foo:>.");
	}

	public void test_prefix() throws Exception {
		parsable("prefix foo:<urn:foo:>.");
	}

	public void test_assignment() throws Exception {
		parsable("$var := <urn:root>.");
	}

	public void test_assign_bool() throws Exception {
		parsable("$var := true.");
	}

	public void test_assign_BOOL() throws Exception {
		parsable("$var := TRUE.");
	}

	public void test_assign_number() throws Exception {
		parsable("$var := 1.");
	}

	public void test_TwoStatements() throws Exception {
		parsable("$one := 1.\n$two := 2.");
	}

	public void test_select() throws Exception {
		parsable("$var := [a foo:Bar; foo:bar \"bar\"].");
	}

	public void test_selectById() throws Exception {
		parsable("$bar := \"bar\"^foo:bar.");
	}

	public void test_insert() throws Exception {
		parsable("insert { $bar a foo:Bar }.");
	}

	public void test_insert_into() throws Exception {
		parsable("insert { $bar a foo:Bar } into <urn:root>.");
	}

	public void test_insert_where() throws Exception {
		parsable("insert { ?b a foo:Bar } where { <urn:foo> foo:bar ?b}.");
	}

	public void test_findByType() throws Exception {
		parsable("$allBars := [a foo:Bar].");
	}

	public void test_insertPath() throws Exception {
		parsable("insert { $bar!foo:bar foo:bar \"bar\" }.");
	}

	public void test_assignVar() throws Exception {
		parsable("$foo := $bar.");
	}

	public void test_path() throws Exception {
		parsable("$foo := $bar ! foo:bar.");
	}

	public void test_call() throws Exception {
		parsable("$bar<-foo:call().");
	}

	public void test_callParams() throws Exception {
		parsable("$bar<-foo:call(foo:bar \"bar\" foo:foo $bar!foo:bar).");
	}

	public void test_clone() throws Exception {
		parsable("$clone := insert [].\n"
				+ "insert { $clone ?pred ?obj } where { $bar ?pred ?obj }.");
	}

	public void test_condition() throws Exception {
		parsable("$var := $bar!rdf:type != foo:Bar.");
	}

	public void test_if() throws Exception {
		parsable("if ($bar!rdf:type != foo:Bar) then do\n"
				+ "  insert { $bar a food:Bar }.\n" + "end.");
	}

	public void test_insertFilter() throws Exception {
		parsable("insert { $bar foo:foo [a foo:Bar] } \n"
				+ "where { filter (regex \"bar\" match $bar!foo:foo!foo:bar) }.");
	}

	public void test_elsif() throws Exception {
		parsable("if ($bar!foo:foo!foo:bar = \"bar\") then do\n"
				+ "end if ($bar!foo:bar = \"bar\") then do\n" + "end else do\n"
				+ "end.");
	}

	public void test_case() throws Exception {
		parsable("case $bar!foo:foo!foo:bar\n"
				+ "when \"bar\" when \"BAR\" then do\n"
				+ "end when 5 then do\n" + "end else do\n" + "end.");
	}

	public void test_forIn() throws Exception {
		parsable("$cats := [a <urn:mamal:Cat>].\n"
				+ "for $cats loop do |$cat|\n" + "end.");
	}

	public void test_forForm() throws Exception {
		parsable("for {?cat a <urn:mamal:Cat>} from <urn:graph:cats> loop do |$cat| end.");
	}

	public void test_forFormNamed() throws Exception {
		parsable("for {?cat a <urn:mamal:Cat>} from-named <urn:graph:cats> loop do |$cat| end.");
	}

	public void test_using() throws Exception {
		parsable("using {\n" + "    { ?pets a <urn:mamal:Cat> }\n"
				+ "    union { ?pets a <urn:mamal:Dog> }} begin do\n"
				+ "  for $pets distinct true loop do\n" + "  end.\n"
				+ "  for { $pets rdfs:label ?name } loop do |$pets $name|\n"
				+ "  end.\n" + "end.");
	}

	public void test_forOrderBy() throws Exception {
		parsable("for $cats asc $cats loop do end.");
	}

	public void test_while() throws Exception {
		parsable("$i := 0.\n" + "while ($i < 10) loop do\n"
				+ "  $i := $i + 1.\n" + "end.");
	}

	public void test_insertBNode() throws Exception {
		parsable("$bar := insert { [a foo:Bar] foo:foo \"bar\" }.");
	}

	public void test_insertBNodeInto() throws Exception {
		parsable("insert {[a foo:Bar] foo:foo \"bar\"} into $graph.");
	}

	public void test_insertResource() throws Exception {
		parsable("insert {<urn:foo:bar> a foo:Bar;  foo:foo \"bar\"}.");
	}

	public void test_selectGraph() throws Exception {
		parsable("using { graph ?graph { <urn:foo:bar> a foo:Bar } } begin do |$graph| end.");
	}

	public void test_beginRecue() throws Exception {
		parsable("$out := new io:FileOutputStream li \"log\".\n"
				+ "try do\n"
				+ "  $out<-output:write(li 'something').\n"
				+ "rescue |$e|\n"
				+ "  <java:java.lang.System>!<java:java.lang.System#err><-print:println(li \"$e\").\n"
				+ "  raise $e.\n"
				+ "ensure\n"
				+ " $out<-output:close().\n"
				+ "end.");
	}

	private void parsable(String code) throws TokenMgrError, ParseException {
		System.out.println();
		System.out.println(code);
		ASTBody tree = SyntaxTreeBuilder.parse(code);
		tree.dump("");
	}
}
