/*
 * Copyright (c) 2011 Talis Inc., Some rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.openrdf.repository.object.managers.helpers;

import static org.openrdf.query.QueryLanguage.SPARQL;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.transform.TransformerException;

import org.openrdf.OpenRDFException;
import org.openrdf.model.Model;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.query.BindingSet;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.parser.ParsedBooleanQuery;
import org.openrdf.query.parser.ParsedGraphQuery;
import org.openrdf.query.parser.ParsedQuery;
import org.openrdf.query.parser.ParsedTupleQuery;
import org.openrdf.query.parser.sparql.SPARQLParser;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectQuery;
import org.openrdf.repository.object.exceptions.ObjectCompositionException;
import org.openrdf.repository.object.managers.PropertyMapper;
import org.openrdf.repository.object.util.ObjectResolver;
import org.openrdf.repository.object.util.ObjectResolver.ObjectFactory;
import org.openrdf.repository.object.xslt.TransformBuilder;
import org.openrdf.repository.object.xslt.XSLTransformer;
import org.openrdf.result.MultipleResultException;
import org.openrdf.result.NoResultException;
import org.openrdf.result.Result;
import org.openrdf.rio.helpers.StatementCollector;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class SparqlEvaluator {
	public class SparqlBuilder {
		private ObjectConnection con;
		private SPARQLQuery query;
		private Map<String, Value> bindings = new HashMap<String, Value>();

		public SparqlBuilder(ObjectConnection con, SPARQLQuery query) {
			this.con = con;
			this.query = query;
		}

		public SparqlBuilder with(String name, Object value) {
			if (value instanceof Value) {
				bindings.put(name, (Value) value);
			} else if (value != null) {
				bindings.put(name, con.getObjectFactory().createValue(value));
			}
			return this;
		}

		public SparqlBuilder with(String name, boolean value) {
			bindings.put(name, con.getValueFactory().createLiteral(value));
			return this;
		}

		public SparqlBuilder with(String name, char value) {
			bindings.put(name, con.getValueFactory().createLiteral(value));
			return this;
		}

		public SparqlBuilder with(String name, byte value) {
			bindings.put(name, con.getValueFactory().createLiteral(value));
			return this;
		}

		public SparqlBuilder with(String name, short value) {
			bindings.put(name, con.getValueFactory().createLiteral(value));
			return this;
		}

		public SparqlBuilder with(String name, int value) {
			bindings.put(name, con.getValueFactory().createLiteral(value));
			return this;
		}

		public SparqlBuilder with(String name, long value) {
			bindings.put(name, con.getValueFactory().createLiteral(value));
			return this;
		}

		public SparqlBuilder with(String name, float value) {
			bindings.put(name, con.getValueFactory().createLiteral(value));
			return this;
		}

		public SparqlBuilder with(String name, double value) {
			bindings.put(name, con.getValueFactory().createLiteral(value));
			return this;
		}

		public Model asModel() throws OpenRDFException {
			GraphQuery qry = prepareGraphQuery();
			Model model = new LinkedHashModel();
			qry.evaluate(new StatementCollector(model));
			return model;
		}

		public Statement asStatement() throws OpenRDFException {
			GraphQueryResult result = asGraphQueryResult();
			try {
				if (result.hasNext()) {
					Statement stmt = result.next();
					if (result.hasNext())
						throw new MultipleResultException();
					return stmt;
				}
				return null;
			} finally {
				result.close();
			}
		}

		public BindingSet asBindingSet() throws OpenRDFException {
			TupleQueryResult result = asTupleQueryResult();
			try {
				if (result.hasNext()) {
					BindingSet bindings = result.next();
					if (result.hasNext())
						throw new MultipleResultException();
					return bindings;
				}
				return null;
			} finally {
				result.close();
			}
		}

		public TupleQueryResult asTupleQueryResult() throws OpenRDFException {
			return prepareTupleQuery().evaluate();
		}

		public GraphQueryResult asGraphQueryResult() throws OpenRDFException {
			return prepareGraphQuery().evaluate();
		}

		public boolean asBoolean() throws OpenRDFException {
			if (query.isBooleanQuery())
				return prepareBooleanQuery().evaluate();
			return asResult(Boolean.class).singleResult().booleanValue();
		}

		public char asChar() throws OpenRDFException {
			return asResult(Character.class).singleResult().charValue();
		}

		public byte asByte() throws OpenRDFException {
			return asResult(Byte.class).singleResult().byteValue();
		}

		public short asShort() throws OpenRDFException {
			return asResult(Short.class).singleResult().shortValue();
		}

		public int asInt() throws OpenRDFException {
			return asResult(Integer.class).singleResult().intValue();
		}

		public long asLong() throws OpenRDFException {
			return asResult(Long.class).singleResult().longValue();
		}

		public float asFloat() throws OpenRDFException {
			return asResult(Float.class).singleResult().floatValue();
		}

		public double asDouble() throws OpenRDFException {
			return asResult(Double.class).singleResult().doubleValue();
		}

		public Set asSet() throws OpenRDFException {
			return asResult().asSet();
		}

		public Result asResult() throws OpenRDFException {
			ObjectQuery qry = prepareObjectQuery(Object.class);
			return qry.evaluate();
		}

		public <T> Result<T> asResult(Class<T> of) throws OpenRDFException {
			ObjectQuery qry = prepareObjectQuery(of);
			return qry.evaluate(of);
		}

		public <T> Set<T> asSet(Class<T> of) throws OpenRDFException {
			if (BindingSet.class.equals(of)) {
				TupleQueryResult result = asTupleQueryResult();
				try {
					List<BindingSet> list = new ArrayList<BindingSet>();
					while (result.hasNext()) {
						list.add(result.next());
					}
					return (Set<T>) list;
				} finally {
					result.close();
				}
			}
			if (Statement.class.equals(of))
				return (Set<T>) asModel();
			return asResult(of).asSet();
		}

		public <T> List<T> asList(Class<T> of) throws OpenRDFException {
			return asResult(of).asList();
		}

		public <T> T as(Class<T> of) throws OpenRDFException {
			try {
				return asResult(of).singleResult();
			} catch (NoResultException e) {
				return null;
			}
		}

		public List asList() throws OpenRDFException {
			return asResult().asList();
		}

		public Document asDocument() throws OpenRDFException,
				TransformerException, IOException, ParserConfigurationException {
			return asTransformBuilder().asDocument();
		}

		public DocumentFragment asDocumentFragment() throws OpenRDFException,
				TransformerException, IOException, ParserConfigurationException {
			return asTransformBuilder().asDocumentFragment();
		}

		public Element asElement() throws OpenRDFException,
				TransformerException, IOException, ParserConfigurationException {
			return asTransformBuilder().asElement();
		}

		public Node asNode() throws OpenRDFException, TransformerException,
				IOException, ParserConfigurationException {
			return asTransformBuilder().asDocument();
		}

		public XMLEventReader asXMLEventReader() throws OpenRDFException,
				TransformerException, IOException, ParserConfigurationException {
			return asTransformBuilder().asXMLEventReader();
		}

		public ReadableByteChannel asReadableByteChannel()
				throws OpenRDFException, TransformerException, IOException,
				ParserConfigurationException {
			return asTransformBuilder().asReadableByteChannel();
		}

		public ByteArrayOutputStream asByteArrayOutputStream()
				throws OpenRDFException, TransformerException, IOException,
				ParserConfigurationException {
			return asTransformBuilder().asByteArrayOutputStream();
		}

		public InputStream asInputStream() throws OpenRDFException,
				TransformerException, IOException, ParserConfigurationException {
			return asTransformBuilder().asInputStream();
		}

		public Reader asReader() throws OpenRDFException, TransformerException,
				IOException {
			return asTransformBuilder().asReader();
		}

		public Object asObject() throws OpenRDFException, TransformerException,
				IOException, ParserConfigurationException {
			return asDocumentFragment();
		}

		private TransformBuilder asTransformBuilder() throws OpenRDFException,
				TransformerException, IOException {
			XSLTransformer xslt = new XSLTransformer();
			if (query.isGraphQuery()) {
				return xslt.transform(asGraphQueryResult(), systemId);
			} else if (query.isTupleQuery()) {
				return xslt.transform(asTupleQueryResult(), systemId);
			} else if (query.isBooleanQuery()) {
				return xslt.transform(asBoolean(), systemId);
			}
			throw new AssertionError("Unknown query type");
		}

		private GraphQuery prepareGraphQuery() throws MalformedQueryException,
				RepositoryException {
			String sparql = query.toString();
			String base = query.getBaseURI();
			GraphQuery qry = con.prepareGraphQuery(SPARQL, sparql, base);
			for (Map.Entry<String, Value> binding : bindings.entrySet()) {
				qry.setBinding(binding.getKey(), binding.getValue());
			}
			return qry;
		}

		private TupleQuery prepareTupleQuery() throws MalformedQueryException,
				RepositoryException {
			String sparql = query.toString();
			String base = query.getBaseURI();
			TupleQuery qry = con.prepareTupleQuery(SPARQL, sparql, base);
			for (Map.Entry<String, Value> binding : bindings.entrySet()) {
				qry.setBinding(binding.getKey(), binding.getValue());
			}
			return qry;
		}

		private BooleanQuery prepareBooleanQuery()
				throws MalformedQueryException, RepositoryException {
			String sparql = query.toString();
			String base = query.getBaseURI();
			BooleanQuery qry = con.prepareBooleanQuery(SPARQL, sparql, base);
			for (Map.Entry<String, Value> binding : bindings.entrySet()) {
				qry.setBinding(binding.getKey(), binding.getValue());
			}
			return qry;
		}

		private ObjectQuery prepareObjectQuery(Class<?> concept)
				throws MalformedQueryException, RepositoryException {
			String sparql = query.toObjectString(concept);
			String base = query.getBaseURI();
			ObjectQuery qry = con.prepareObjectQuery(SPARQL, sparql, base);
			for (Map.Entry<String, Value> binding : bindings.entrySet()) {
				qry.setBinding(binding.getKey(), binding.getValue());
			}
			return qry;
		}
	}

	private static final Pattern selectWhere = Pattern.compile(
			"\\sSELECT\\s+([\\?\\$]\\w+)\\s+WHERE\\s*\\{",
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	private static final Pattern limitOffset = Pattern.compile(
			"\\bLIMIT\\b|\\bOFFSET\\b", Pattern.CASE_INSENSITIVE);

	private class SPARQLQuery {
		private String sparql;
		private String base;
		private Class<?> concept;
		private String object;
		private ParsedQuery query;

		public SPARQLQuery(Reader in, String base) throws IOException,
				MalformedQueryException {
			try {
				StringWriter sw = new StringWriter();
				int read;
				char[] cbuf = new char[1024];
				while ((read = in.read(cbuf)) >= 0) {
					sw.write(cbuf, 0, read);
				}
				sparql = sw.toString();
				this.base = base;
				query = new SPARQLParser().parseQuery(sparql, base);
			} finally {
				in.close();
			}
		}

		public String getBaseURI() {
			return base;
		}

		public boolean isBooleanQuery() {
			return query instanceof ParsedBooleanQuery;
		}

		public boolean isGraphQuery() {
			return query instanceof ParsedGraphQuery;
		}

		public boolean isTupleQuery() {
			return query instanceof ParsedTupleQuery;
		}

		public synchronized String toObjectString(Class<?> concept) {
			if (concept.equals(this.concept))
				return object;
			if (isTupleQuery()) {
				this.concept = concept;
				ClassLoader cl = concept.getClassLoader();
				if (cl == null) {
					cl = SparqlEvaluator.class.getClassLoader();
				}
				PropertyMapper pm = new PropertyMapper(cl, readTypes);
				Map<String, String> eager = pm.findEagerProperties(concept);
				object = optimizeQueryString(sparql, eager);
			} else {
				this.concept = concept;
				object = sparql;
			}
			return object;
		}

		public String toString() {
			return sparql;
		}

		/**
		 * @param map
		 *            property name to predicate uri or null for datatype
		 */
		private String optimizeQueryString(String sparql,
				Map<String, String> map) {
			Matcher matcher = selectWhere.matcher(sparql);
			if (map != null && matcher.find()
					&& !limitOffset.matcher(sparql).find()) {
				String var = matcher.group(1);
				int idx = sparql.lastIndexOf('}');
				StringBuilder sb = new StringBuilder(256 + sparql.length());
				sb.append(sparql, 0, matcher.start(1));
				sb.append(var).append(" ");
				sb.append(var).append("_class").append(" ");
				for (Map.Entry<String, String> e : map.entrySet()) {
					String name = e.getKey();
					if (name.equals("class"))
						continue;
					sb.append(var).append("_").append(name).append(" ");
				}
				sb.append(sparql, matcher.end(1), idx);
				sb.append(" OPTIONAL { ").append(var);
				sb.append(" a ").append(var).append("_class}");
				for (Map.Entry<String, String> e : map.entrySet()) {
					String pred = e.getValue();
					String name = e.getKey();
					if (name.equals("class"))
						continue;
					sb.append(" OPTIONAL { ").append(var).append(" <");
					sb.append(pred).append("> ");
					sb.append(var).append("_").append(name).append("}");
				}
				sb.append(sparql, idx, sparql.length());
				return sb.toString();
			}
			return sparql;
		}
	}

	private SPARQLQuery sparql;
	private final String systemId;
	private final boolean readTypes;
	private final ObjectResolver<SPARQLQuery> resolver;

	public SparqlEvaluator(String systemId, boolean readTypes) {
		this.systemId = systemId;
		this.readTypes = readTypes;
		ClassLoader cl = getClass().getClassLoader();
		resolver = ObjectResolver.newInstance(cl,
				new ObjectFactory<SPARQLQuery>() {

					public SPARQLQuery create(String systemId, InputStream in)
							throws Exception {
						return create(systemId, new InputStreamReader(in,
								"UTF-8"));
					}

					public SPARQLQuery create(String systemId, Reader in)
							throws Exception {
						return new SPARQLQuery(in, systemId);
					}

					public String[] getContentTypes() {
						return new String[] { "application/sparql-query" };
					}

					public boolean isReusable() {
						return true;
					}
				});
	}

	public SparqlEvaluator(Reader reader, String systemId, boolean readTypes) {
		this(systemId, readTypes);
		try {
			sparql = resolver.getObjectFactory().create(systemId, reader);
		} catch (Exception e) {
			throw new ObjectCompositionException(e);
		}
	}

	public SparqlBuilder prepare(ObjectConnection con) {
		try {
			return new SparqlBuilder(con, getSparqlQuery());
		} catch (Exception e) {
			throw new ObjectCompositionException(e);
		}
	}

	private SPARQLQuery getSparqlQuery() throws Exception {
		if (sparql != null)
			return sparql;
		return resolver.resolve(systemId);
	}

}
