package org.openrdf.alibaba.decor.support;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.openrdf.alibaba.decor.Decoration;
import org.openrdf.alibaba.decor.Representation;
import org.openrdf.alibaba.decor.TextPresentation;
import org.openrdf.alibaba.decor.TextPresentationExportBehaviour;
import org.openrdf.alibaba.decor.base.TextPresentationBase;
import org.openrdf.alibaba.decor.helpers.Context;
import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.pov.Display;
import org.openrdf.alibaba.pov.Intent;
import org.openrdf.alibaba.pov.Perspective;
import org.openrdf.alibaba.pov.PerspectiveOrSearchPattern;
import org.openrdf.alibaba.pov.SearchPattern;
import org.openrdf.alibaba.vocabulary.DCR;
import org.openrdf.elmo.ElmoQuery;
import org.openrdf.elmo.Entity;
import org.openrdf.elmo.annotations.rdf;

@rdf(DCR.NS + "Presentation")
public class TextPresentationExportSupport extends TextPresentationBase
		implements TextPresentationExportBehaviour {

	public TextPresentationExportSupport(TextPresentation presentation) {
		super(presentation);
	}

	public void exportPresentation(Intent intent, Entity target, Context ctx)
			throws AlibabaException, IOException {
		presentation(intent, target, ctx);
	}

	public void exportRepresentation(PerspectiveOrSearchPattern spec,
			Context context) throws AlibabaException, IOException {
		resources(spec.getPovPurpose(), spec, Collections.singleton(null), context);
	}

	@Override
	protected void display(Intent intent, Representation rep, Display display,
			Object resource, Context ctx) throws AlibabaException, IOException {
		Decoration decor = rep.findDecorationFor(display);
		Collection<?> values = display.getValuesOf(resource);
		if (values.isEmpty()) {
			decor.empty(ctx.getBindings());
		} else {
			decor.before(ctx.getBindings());
			if (display.getPovPerspective() != null) {
				perspectiveDisplay(decor, display, values, ctx);
			} else if (display.getPovSearchPattern() != null) {
				searchDisplay(intent, decor, display, values, ctx);
			} else {
				literalDisplay(decor, values, ctx);
			}
			decor.after(ctx.getBindings());
		}
	}

	protected void perspectiveDisplay(Decoration decor, Display display,
			Collection<?> values, Context ctx) throws AlibabaException,
			IOException {
		Perspective spec = display.getPovPerspective();
		if (decor.isSeparation()) {
			Iterator<?> iter = values.iterator();
			while (iter.hasNext()) {
				Set<?> singleton = Collections.singleton(iter.next());
				resources(spec.getPovPurpose(), spec, singleton, ctx);
				if (iter.hasNext()) {
					decor.separation(ctx.getBindings());
				}
			}
		} else {
			resources(spec.getPovPurpose(), spec, values, ctx);
		}
	}

	protected void searchDisplay(Intent intent, Decoration decor,
			Display display, Collection<?> values, Context ctx)
			throws AlibabaException, IOException {
		SearchPattern sp = display.getPovSearchPattern();
		Iterator<?> iter = values.iterator();
		while (iter.hasNext()) {
			// FIXME what about value?
			ElmoQuery<?> query = sp.createElmoQuery(ctx.getFilter(), ctx
					.getOrderBy());
			try {
				resources(intent, sp, query, ctx);
			} finally {
				query.close();
			}
			if (iter.hasNext()) {
				decor.separation(ctx.getBindings());
			}
		}
	}

	protected void literalDisplay(Decoration decor, Collection<?> values,
			Context ctx) throws AlibabaException, IOException {
		decor.values(values, ctx.getBindings());
	}
}
