/*
 * Copyright (c) 2012 3 Round Stones Inc., Some rights reserved.
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
package org.openrdf.repository.auditing;

import static org.openrdf.query.QueryLanguage.SPARQL;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.Update;
import org.openrdf.query.UpdateExecutionException;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.UpdateExpr;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.impl.DatasetImpl;
import org.openrdf.query.parser.ParsedUpdate;
import org.openrdf.query.parser.QueryParser;
import org.openrdf.query.parser.QueryParserUtil;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.auditing.helpers.BasicGraphPatternVisitor;
import org.openrdf.repository.contextaware.ContextAwareConnection;
import org.openrdf.rio.turtle.TurtleUtil;

public class AuditingRepositoryConnection extends ContextAwareConnection {

	private static final int MAX_SIZE = 1024;
	private static final String RECENT_ACTIVITY = "http://www.openrdf.org/rdf/2012/auditing#RecentActivity";
	private static final String WAS_INFORMED_BY = "http://www.w3.org/ns/prov#wasInformedBy";
	private static final String USED = "http://www.w3.org/ns/prov#used";
	private static final String UPDATE_ACTIVITY = "PREFIX prov:<http://www.w3.org/ns/prov#>\n"
			+ "DELETE {\n\t"
			+ "?used prov:wasGeneratedBy ?generatedBy .\n\t"
			+ "?entity prov:wasGeneratedBy ?generatedBy\n"
			+ "} INSERT {\n\t"
			+ "GRAPH $activity { ?used prov:wasGeneratedBy $activity }\n\t"
			+ "GRAPH $activity { $activity prov:used ?entity }\n\t"
			+ "GRAPH $activity { ?entity prov:wasGeneratedBy $activity }\n"
			+ "} WHERE {\n\t"
			+ "{\n\t\t"
			+ "GRAPH $activity { $activity prov:used ?used }\n\t\t"
			+ "?used prov:wasGeneratedBy ?generatedBy\n\t"
			+ "} UNION {\n\t\t"
			+ "GRAPH $activity { ?resource ?predicate ?object }\n\t\t"
			+ "FILTER isIri(?resource)\n\t\t"
			+ "FILTER ( !sameTerm($activity,?resource) )\n\t\t"
			+ "BIND ( if( contains(str(?resource),\"#\"), iri(strbefore(str(?resource),\"#\")), ?resource ) AS ?entity)\n\t\t"
			+ "FILTER ( !sameTerm($activity,?entity) )\n\t\t"
			+ "OPTIONAL { ?entity prov:wasGeneratedBy ?generatedBy }\n\t"
			+ "}\n" + "}";
	private static final String BALANCE_ACTIVITY = UPDATE_ACTIVITY.substring(0,
			UPDATE_ACTIVITY.length() - 2)
			+ "\n\t"
			+ "FILTER (\n\t\t"
			+ "!bound(?generatedBy) ||\n\t\t"
			+ "EXISTS { $activity prov:wasInformedBy ?generatedBy } ||\n\t\t"
			+ "EXISTS { $activity prov:endedAtTime ?after . ?generatedBy prov:endedAtTime ?before FILTER (?before < ?after) }\n\t"
			+ ")\n" + "}";

	private final AuditingRepository repository;
	private final Map<URI, Set<URI>> modifiedGraphs = new HashMap<URI, Set<URI>>();
	private final Map<URI, Set<URI>> modifiedEntities = new HashMap<URI, Set<URI>>();
	private Set<URI> uncommittedActivityGraphs = new LinkedHashSet<URI>();

	public AuditingRepositoryConnection(AuditingRepository repository,
			RepositoryConnection connection) throws RepositoryException {
		super(repository, connection);
		this.repository = repository;
	}

	@Override
	public AuditingRepository getRepository() {
		return repository;
	}

	@Override
	public void commit() throws RepositoryException {
		Set<URI> recentActivities = finalizeActivityGraphs();
		super.commit();
		closeActivityGraphs(recentActivities);
	}

	@Override
	public void close() throws RepositoryException {
		super.close();
		getRepository().cleanup();
	}

	@Override
	public Update prepareUpdate(String query) throws MalformedQueryException,
			RepositoryException {
		return prepareUpdate(getQueryLanguage(), query);
	}

	@Override
	public Update prepareUpdate(QueryLanguage ql, String query)
			throws MalformedQueryException, RepositoryException {
		return prepareUpdate(ql, query, getBaseURI());
	}

	@Override
	public Update prepareUpdate(final QueryLanguage ql, final String update, final String baseURI)
			throws MalformedQueryException, RepositoryException {
		final Update prepared = super.prepareUpdate(ql, update, baseURI);
		if (prepared == null)
			return null;
		return new Update(){
			public void execute() throws UpdateExecutionException {
				try {
					BindingSet bindings = prepared.getBindings();
					Dataset dataset = prepared.getDataset();
					activity(ql, update, baseURI, bindings, dataset);
				} catch (MalformedQueryException e) {
					// ignore
				} catch (QueryEvaluationException e) {
					// ignore
				} catch (RepositoryException e) {
					throw new UpdateExecutionException(e);
				}
				prepared.execute();
			}

			public void setBinding(String name, Value value) {
				prepared.setBinding(name, value);
			}

			public void removeBinding(String name) {
				prepared.removeBinding(name);
			}

			public void clearBindings() {
				prepared.clearBindings();
			}

			public BindingSet getBindings() {
				return prepared.getBindings();
			}

			public void setDataset(Dataset dataset) {
				prepared.setDataset(dataset);
			}

			public Dataset getDataset() {
				return prepared.getDataset();
			}

			public void setIncludeInferred(boolean includeInferred) {
				prepared.setIncludeInferred(includeInferred);
			}

			public boolean getIncludeInferred() {
				return prepared.getIncludeInferred();
			}
		};
	}

	@Override
	protected boolean isDelegatingAdd() throws RepositoryException {
		return getInsertContext() == null;
	}

	@Override
	protected void addWithoutCommit(Resource subject, URI predicate,
			Value object, Resource... contexts) throws RepositoryException {
		activity(subject, contexts);
		getDelegate().add(subject, predicate, object, contexts);
	}

	@Override
	protected boolean isDelegatingRemove() {
		return getInsertContext() == null;
	}

	@Override
	protected void removeWithoutCommit(Resource subject, URI predicate,
			Value object, Resource... contexts) throws RepositoryException {
		Resource[] defRemove = getReadContexts();
		if (contexts == null || contexts.length > 0) {
			activity(subject, contexts);
			getDelegate().remove(subject, predicate, object, contexts);
		} else if (defRemove == null || defRemove.length > 0) {
			activity(subject, defRemove);
			getDelegate().remove(subject, predicate, object, defRemove);
		} else {
			activity(subject);
			executeDelete(subject, predicate, object);
		}
	}

	private void executeDelete(Resource subject, URI predicate, Value object)
			throws RepositoryException {
		StringBuilder sb = new StringBuilder();
		if (subject instanceof URI && predicate instanceof URI
				&& (object instanceof URI || object instanceof Literal)) {
			sb.append("DELETE DATA { ");
			append(subject, predicate, object, sb);
			sb.append(" }");
		} else {
			sb.append("DELETE { ");
			append(subject, predicate, object, sb);
			sb.append(" } WHERE { ");
			append(subject, predicate, object, sb);
			sb.append(" }");
		}
		String operation = sb.toString();
		try {
			Update update = prepareUpdate(QueryLanguage.SPARQL, operation);
			if (subject instanceof BNode) {
				update.setBinding("subject", subject);
			}
			if (object instanceof BNode) {
				update.setBinding("object", object);
			}
			update.execute();
		} catch (MalformedQueryException e) {
			throw new AssertionError(e);
		} catch (UpdateExecutionException e) {
			throw new RepositoryException(e);
		}
	}

	private void append(Resource subject, URI predicate, Value object,
			StringBuilder sb) {
		if (subject instanceof URI) {
			sb.append("<").append(enc(subject)).append("> ");
		} else {
			sb.append("?subject ");
		}
		if (predicate instanceof URI) {
			sb.append("<").append(enc(predicate)).append("> ");
		} else {
			sb.append("?predicate ");
		}
		if (object instanceof URI) {
			sb.append("<").append(enc(object)).append("> ");
		} else if (object instanceof Literal) {
			Literal lit = (Literal) object;
			sb.append('"');
			sb.append(TurtleUtil.encodeString(lit.stringValue()));
			sb.append('"');
			if (lit.getLanguage() != null) {
				sb.append("@");
				sb.append(lit.getLanguage());
			} else if (lit.getDatatype() != null) {
				sb.append("^^");
				sb.append("<").append(enc(lit.getDatatype())).append("> ");
			}
		} else {
			sb.append("?object ");
		}
	}

	private String enc(Value uri) {
		return TurtleUtil.encodeURIString(uri.stringValue());
	}

	private void activity(QueryLanguage ql, String update, String baseURI,
			BindingSet bindings, Dataset dataset)
			throws MalformedQueryException, RepositoryException, QueryEvaluationException {
		QueryParser parser = QueryParserUtil.createParser(ql);
		ParsedUpdate parsed = parser.parseUpdate(update, baseURI);
		for (UpdateExpr expr : parsed.getUpdateExprs()) {
			for (URI entity : findEntity(expr, bindings)) {
				activity(entity);
			}
			activity(null, findGraphs(expr, bindings, dataset));
		}
	}

	private Set<URI> findEntity(UpdateExpr expr, final BindingSet bindings) throws QueryEvaluationException {
		final Set<URI> entities = new HashSet<URI>();
		expr.visit(new BasicGraphPatternVisitor() {
			public void meet(StatementPattern node) {
				Var var = node.getSubjectVar();
				Value subj = var.getValue();
				if (subj == null) {
					subj = bindings.getValue(var.getName());
				}
				if (subj instanceof URI) {
					entities.add(entity((URI) subj));
				}
			}
		});
		return entities;
	}

	private URI[] findGraphs(UpdateExpr expr, final BindingSet bindings,
			Dataset dataset) throws QueryEvaluationException {
		final Set<URI> graphs = new LinkedHashSet<URI>();
		if (dataset != null) {
			if (dataset.getDefaultInsertGraph() != null) {
				graphs.add(dataset.getDefaultInsertGraph());
			}
			if (dataset.getDefaultRemoveGraphs() != null) {
				graphs.addAll(dataset.getDefaultRemoveGraphs());
			}
		}
		expr.visit(new BasicGraphPatternVisitor() {
			public void meet(StatementPattern node) {
				Var var = node.getContextVar();
				if (var != null) {
					Value ctx = var.getValue();
					if (ctx == null) {
						ctx = bindings.getValue(var.getName());
					}
					if (ctx instanceof URI) {
						graphs.add((URI) ctx);
					}
				}
			}
		});
		return graphs.toArray(new URI[graphs.size()]);
	}

	private URI entity(URI subject) {
		URI entity = subject;
		int hash = entity.stringValue().indexOf('#');
		if (hash > 0) {
			ValueFactory vf = getValueFactory();
			entity = vf.createURI(entity.stringValue().substring(0, hash));
		}
		return entity;
	}

	private synchronized void activity(Resource subject, Resource... contexts) throws RepositoryException {
		URI activityGraph = getInsertContext();
		if (activityGraph == null)
			return;
		uncommittedActivityGraphs.add(activityGraph);
		if (subject instanceof URI && !activityGraph.equals(subject)) {
			Set<URI> entities = modifiedEntities.get(activityGraph);
			if (entities == null) {
				modifiedEntities.put(activityGraph, entities = new LinkedHashSet<URI>());
			}
			entities.add(entity((URI) subject));
			if (entities.size() >= MAX_SIZE) {
				URI used = getValueFactory().createURI(USED);
				for (URI entity : entities) {
					add(activityGraph, used, entity, activityGraph);
				}
				entities.clear();
			}
		}
		if (contexts == null || contexts.length == 0)
			return;
		if (contexts.length == 1 && activityGraph.equals(contexts[0]))
			return;
		Set<URI> graphs = modifiedGraphs.get(activityGraph);
		if (graphs == null) {
			modifiedGraphs.put(activityGraph, graphs = new LinkedHashSet<URI>());
		}
		for (Resource ctx : contexts) {
			if (ctx instanceof URI && !activityGraph.equals(ctx)) {
				graphs.add((URI) ctx);
			}
		}
		if (graphs.size() >= MAX_SIZE) {
			URI informedBy = getValueFactory().createURI(WAS_INFORMED_BY);
			for (URI graph : graphs) {
				add(activityGraph, informedBy, graph, activityGraph);
			}
			graphs.clear();
		}
	}

	private synchronized Set<URI> finalizeActivityGraphs()
			throws RepositoryException {
		Set<URI> recentActivities = uncommittedActivityGraphs;
		int size = recentActivities.size();
		uncommittedActivityGraphs = new LinkedHashSet<URI>(size);
		for (URI activityGraph : recentActivities) {
			Set<URI> graphs = modifiedGraphs.get(activityGraph);
			Set<URI> entities = modifiedEntities.get(activityGraph);
			addMetadata(activityGraph, entities, graphs);
			if (getRepository().isTransactional()) {
				finalizeActivityGraph(activityGraph);
			}
		}
		modifiedGraphs.clear();
		modifiedEntities.clear();
		return recentActivities;
	}

	private void addMetadata(URI activityGraph, Set<URI> entities,
			Set<URI> graphs) throws RepositoryException {
		if (entities != null) {
			URI used = getValueFactory().createURI(USED);
			for (URI entity : entities) {
				add(activityGraph, used, entity, activityGraph);
			}
		}
		if (graphs != null) {
			URI informedBy = getValueFactory().createURI(WAS_INFORMED_BY);
			for (URI graph : graphs) {
				add(activityGraph, informedBy, graph, activityGraph);
			}
		}
		URI recentActivity = getValueFactory().createURI(RECENT_ACTIVITY);
		add(activityGraph, RDF.TYPE, recentActivity, activityGraph);
	}

	private void finalizeActivityGraph(URI activityGraph)
			throws RepositoryException {
		try {
			Update update = prepareUpdate(SPARQL, UPDATE_ACTIVITY);
			update.setBinding("activity", activityGraph);
			update.setDataset(new DatasetImpl());
			update.execute();
		} catch (UpdateExecutionException e) {
			throw new RepositoryException(e);
		} catch (MalformedQueryException e) {
			throw new RepositoryException(e);
		}
	}

	private void closeActivityGraphs(Set<URI> recentActivities)
			throws RepositoryException {
		getRepository().addRecentActivities(recentActivities);
		if (!getRepository().isTransactional()) {
			for (URI activityGraph : recentActivities) {
				balanceActivityGraph(activityGraph);
			}
		}
	}

	private void balanceActivityGraph(URI activityGraph)
			throws RepositoryException {
		try {
			Update update = prepareUpdate(SPARQL, BALANCE_ACTIVITY);
			update.setBinding("activity", activityGraph);
			update.setDataset(new DatasetImpl());
			update.execute();
		} catch (UpdateExecutionException e) {
			throw new RepositoryException(e);
		} catch (MalformedQueryException e) {
			throw new RepositoryException(e);
		}
	}

}
