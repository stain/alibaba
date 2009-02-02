package org.openrdf.script.helpers;

import org.openrdf.model.Model;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.store.StoreException;

public class ModelInserter implements RDFHandler {

	private RepositoryConnection con;

	private Model model = new LinkedHashModel();

	public ModelInserter(RepositoryConnection con) {
		this.con = con;
	}

	public Model getModel() {
		return model;
	}

	public void startRDF() throws RDFHandlerException {
	}

	public void endRDF() throws RDFHandlerException {
	}

	public void handleComment(String comment) throws RDFHandlerException {
	}

	public void handleNamespace(String prefix, String name)
			throws RDFHandlerException {
		model.setNamespace(prefix, name);
	}

	public void handleStatement(Statement st) throws RDFHandlerException {
		try {
			model.add(st);
			con.add(st);
		} catch (StoreException e) {
			throw new RDFHandlerException(e);
		}
	}
}
