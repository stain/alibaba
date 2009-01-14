package org.openrdf.elmo.sesame;

import org.openrdf.elmo.RdfTypeFactory;
import org.openrdf.elmo.base.RoleMapperFactoryBase;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;

public class SesameRoleMapperFactory extends RoleMapperFactoryBase<URI> {
	private ValueFactory vf;

	public SesameRoleMapperFactory(ValueFactory vf) {
		this.vf = vf;
	}

	@Override
	protected RdfTypeFactory<URI> getRdfTypeFactory() {
		return new SesameTypeFactory(vf);
	}

}
