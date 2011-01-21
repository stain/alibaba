/*
 * Copyright (c) 2009, James Leigh All rights reserved.
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
package org.openrdf.sail.auditing.config;

import static org.openrdf.sail.auditing.config.AuditingSchema.ARCHIVING;
import static org.openrdf.sail.auditing.config.AuditingSchema.MAX_ARCHIVE;
import static org.openrdf.sail.auditing.config.AuditingSchema.MAX_RECENT;
import static org.openrdf.sail.auditing.config.AuditingSchema.MIN_RECENT;
import static org.openrdf.sail.auditing.config.AuditingSchema.TRX_NAMESPACE;

import org.openrdf.model.Graph;
import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.sail.config.DelegatingSailImplConfigBase;
import org.openrdf.sail.config.SailConfigException;
import org.openrdf.sail.config.SailImplConfig;

/**
 * Parses and serialises the auditing SAIL configuration.
 */
public class AuditingConfig extends DelegatingSailImplConfigBase {

	public AuditingConfig() {
		super(AuditingFactory.SAIL_TYPE);
	}

	public AuditingConfig(SailImplConfig delegate) {
		super(AuditingFactory.SAIL_TYPE, delegate);
	}

	private String ns;
	private boolean archiving;
	private int maxArchive;
	private int minRecent;
	private int maxRecent;

	public String getNamespace() {
		return ns;
	}

	public void setNamespace(String ns) {
		this.ns = ns;
	}

	public boolean isArchiving() {
		return archiving;
	}

	public void setArchiving(boolean archiving) {
		this.archiving = archiving;
	}

	public int getMaxArchive() {
		return maxArchive;
	}

	public void setMaxArchive(int maxArchive) {
		this.maxArchive = maxArchive;
	}

	public int getMinRecent() {
		return minRecent;
	}

	public void setMinRecent(int minRecent) {
		this.minRecent = minRecent;
	}

	public int getMaxRecent() {
		return maxRecent;
	}

	public void setMaxRecent(int maxRecent) {
		this.maxRecent = maxRecent;
	}

	@Override
	public Resource export(Graph model) {
		ValueFactory vf = ValueFactoryImpl.getInstance();
		Resource self = super.export(model);
		if (ns != null) {
			model.add(self, TRX_NAMESPACE, vf.createLiteral(ns));
		}
		model.add(self, ARCHIVING, vf.createLiteral(archiving));
		model.add(self, MAX_ARCHIVE, vf.createLiteral(maxArchive));
		model.add(self, MIN_RECENT, vf.createLiteral(minRecent));
		model.add(self, MAX_RECENT, vf.createLiteral(maxRecent));
		return self;
	}

	@Override
	public void parse(Graph graph, Resource implNode)
			throws SailConfigException {
		super.parse(graph, implNode);
		Model model = new LinkedHashModel(graph);
		setNamespace(model.filter(implNode, TRX_NAMESPACE, null).objectString());
		Literal lit = model.filter(implNode, ARCHIVING, null).objectLiteral();
		if (lit != null) {
			setArchiving(lit.booleanValue());
		}
		lit = model.filter(implNode, MAX_ARCHIVE, null).objectLiteral();
		if (lit != null) {
			setMaxArchive(lit.intValue());
		}
		lit = model.filter(implNode, MIN_RECENT, null).objectLiteral();
		if (lit != null) {
			setMinRecent(lit.intValue());
		}
		lit = model.filter(implNode, MAX_RECENT, null).objectLiteral();
		if (lit != null) {
			setMaxRecent(lit.intValue());
		}
	}

}
