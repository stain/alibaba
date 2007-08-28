package org.openrdf.alibaba.concepts;

import java.lang.String;
import java.util.Set;
import org.openrdf.concepts.owl.Thing;
import org.openrdf.elmo.annotations.rdf;

/** Describes part of a query. */
@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#Expression")
public interface Expression extends Thing, DisplayOrExpression {


	/** Binding display parameters to be used when evaluating this expression. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#binding")
	public abstract Set<LiteralDisplay> getPovBindings();

	/** Binding display parameters to be used when evaluating this expression. */
	public abstract void setPovBindings(Set<LiteralDisplay> value);


	/** The query represented in Java Persistence Query Language. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#inJpql")
	public abstract String getPovInJpql();

	/** The query represented in Java Persistence Query Language. */
	public abstract void setPovInJpql(String value);


	/** The query represented in Sesame RDF Query Language. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#inSerql")
	public abstract String getPovInSerql();

	/** The query represented in Sesame RDF Query Language. */
	public abstract void setPovInSerql(String value);


	/** The query represented in SPARQL RDF Query Language. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#inSparql")
	public abstract String getPovInSparql();

	/** The query represented in SPARQL RDF Query Language. */
	public abstract void setPovInSparql(String value);


	/** The query represented in Structured Query Language. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#inSql")
	public abstract String getPovInSql();

	/** The query represented in Structured Query Language. */
	public abstract void setPovInSql(String value);

}
