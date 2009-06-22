package org.openrdf.sail.auditing.config;

import static org.openrdf.sail.auditing.config.AuditingSchema.ARCHIVING;
import static org.openrdf.sail.auditing.config.AuditingSchema.CURRENT_TRX;
import static org.openrdf.sail.auditing.config.AuditingSchema.TRX_NAMESPACE;

import org.openrdf.model.Graph;
import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.sail.config.DelegatingSailImplConfigBase;
import org.openrdf.sail.config.SailConfigException;
import org.openrdf.sail.config.SailImplConfig;

public class AuditingConfig extends DelegatingSailImplConfigBase {

	public AuditingConfig() {
		super(AuditingFactory.SAIL_TYPE);
	}

	public AuditingConfig(SailImplConfig delegate) {
		super(AuditingFactory.SAIL_TYPE, delegate);
	}

	private String ns;
	private URI currentTrx;
	private boolean archiving;

	public String getNamespace() {
		return ns;
	}

	public void setNamespace(String ns) {
		this.ns = ns;
	}

	public URI getCurrentTransaction() {
		return currentTrx;
	}

	public void setCurrentTransaction(URI currentTrx) {
		this.currentTrx = currentTrx;
	}

	public boolean isArchiving() {
		return archiving;
	}

	public void setArchiving(boolean archiving) {
		this.archiving = archiving;
	}

	@Override
	public Resource export(Graph model) {
		ValueFactory vf = ValueFactoryImpl.getInstance();
		Resource self = super.export(model);
		model.add(self, TRX_NAMESPACE, vf.createLiteral(ns));
		if (currentTrx != null) {
			model.add(self, TRX_NAMESPACE, currentTrx);
		}
		model.add(self, ARCHIVING, vf.createLiteral(archiving));
		return self;
	}

	@Override
	public void parse(Graph graph, Resource implNode)
			throws SailConfigException {
		super.parse(graph, implNode);
		Model model = new LinkedHashModel(graph);
		setNamespace(model.filter(implNode, TRX_NAMESPACE, null).objectString());
		URI uri = model.filter(implNode, CURRENT_TRX, null).objectURI();
		setCurrentTransaction(uri);
		Literal lit = model.filter(implNode, ARCHIVING, null).objectLiteral();
		if (lit != null) {
			setArchiving(lit.booleanValue());
		}
	}

}
