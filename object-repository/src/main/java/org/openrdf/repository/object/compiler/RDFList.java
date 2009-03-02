package org.openrdf.repository.object.compiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;


public class RDFList {

	private ValueFactory vf = ValueFactoryImpl.getInstance();

	private RDFDataSource triples;

	private Resource start;

	public RDFList(Model model, Resource start) {
		this(new RDFDataSource(model), start);
	}

	public RDFList(RDFDataSource triples, Resource start) {
		this.triples = triples;
		this.start = start;
	}

	public List<? extends Value> asList() {
		if (start == null)
			return Collections.emptyList();
		List<Value> list = new ArrayList<Value>();
		return copyTo(start, list);
	}

	public void addAllOthers(RDFList list) {
		List<? extends Value> l = list.asList();
		l.removeAll(asList());
		addAll(l);
	}

	private void addAll(List<? extends Value> list) {
		if (start == null) {
			start = vf.createBNode();
		}
		for (Value element : list) {
			addTo(start, element);
		}
	}

	private List<Value> copyTo(Resource node, List<Value> list) {
		Value first = triples.match(node, RDF.FIRST, null).objectValue();
		Resource rest = triples.match(node, RDF.REST, null).objectResource();
		if (first == null)
			return list;
		list.add(first);
		return copyTo(rest, list);
	}

	private void addTo(Resource node, Value element) {
		if (triples.contains(node, RDF.FIRST, null)) {
			Resource rest = triples.match(node, RDF.REST, null).objectResource();
			if (rest == null || rest.equals(RDF.NIL)) {
				rest = vf.createBNode();
				triples.remove(node, RDF.REST, null);
				triples.add(node, RDF.REST, rest);
			}
			addTo(rest, element);
		} else {
			triples.add(node, RDF.FIRST, element);
			triples.add(node, RDF.REST, RDF.NIL);
		}
	}

}
