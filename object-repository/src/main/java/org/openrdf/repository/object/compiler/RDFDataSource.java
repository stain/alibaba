package org.openrdf.repository.object.compiler;

import java.util.Map;

import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LinkedHashModel;

public class RDFDataSource {
	private Model model;

	public RDFDataSource(Model model) {
		this.model = model;
	}

	public boolean contains(Resource subj, URI pred, Value obj) {
		return model.contains(subj, pred, obj);
	}

	public Model match(Resource subj, URI pred, Value obj, Resource... contexts) {
		return new LinkedHashModel(model.filter(subj, pred, obj, contexts));
	}

	public Map<String, String> getNamespaces() {
		return model.getNamespaces();
	}

	public String getNamespace(String prefix) {
		return model.getNamespace(prefix);
	}

	public String setNamespace(String prefix, String name) {
		return model.setNamespace(prefix, name);
	}

	public void add(Resource subj, URI pred, Value obj) {
		model.add(subj, pred, obj);
	}

	public boolean remove(Resource subj, URI pred, Value obj) {
		return model.remove(subj, pred, obj);
	}

}
