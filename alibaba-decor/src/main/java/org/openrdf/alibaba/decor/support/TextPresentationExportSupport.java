package org.openrdf.alibaba.decor.support;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
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

/**
 * Presentation support for exporting text based representations.
 * 
 * @author James Leigh
 *
 */
@rdf(DCR.NS + "Presentation")
public class TextPresentationExportSupport extends TextPresentationBase
		implements TextPresentationExportBehaviour {

	public TextPresentationExportSupport(TextPresentation presentation) {
		super(presentation);
	}

	public void exportPresentation(PerspectiveOrSearchPattern spec,
			Entity target, Context ctx) throws AlibabaException, IOException {
		presentation(spec, target, ctx);
	}

	public void exportRepresentation(PerspectiveOrSearchPattern spec,
			Context context) throws AlibabaException, IOException {
		Intent intent = context.getIntent();
		context.setIntent(spec.getPovPurpose());
		resources(spec, Collections.singleton(null), context);
		context.setIntent(intent);
	}

	@Override
	protected void resources(PerspectiveOrSearchPattern spec, Representation rep,
			List<Display> displays, Collection<?> resources, Context ctx)
			throws AlibabaException, IOException {
		Decoration decor = rep.getPovDecoration();
		Iterator<?> iter = resources.iterator();
		if (iter.hasNext()) {
			decor.before(ctx.getBindings());
			while (iter.hasNext()) {
				resource(rep, displays, iter.next(), ctx);
				if (iter.hasNext()) {
					decor.separation(ctx.getBindings());
				}
			}
			decor.after(ctx.getBindings());
		} else {
			decor.empty(ctx.getBindings());
		}
	}

	@Override
	protected void display(Representation rep, Display display,
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
				searchDisplay(decor, display, values, ctx);
			} else {
				literalDisplay(decor, values, ctx);
			}
			decor.after(ctx.getBindings());
		}
	}

	private void perspectiveDisplay(Decoration decor, Display display,
			Collection<?> values, Context ctx) throws AlibabaException,
			IOException {
		Perspective spec = display.getPovPerspective();
		Intent intent = ctx.getIntent();
		if (decor.isSeparation()) {
			Iterator<?> iter = values.iterator();
			while (iter.hasNext()) {
				Set<?> singleton = Collections.singleton(iter.next());
				ctx.setIntent(spec.getPovPurpose());
				resources(spec, singleton, ctx);
				ctx.setIntent(intent);
				if (iter.hasNext()) {
					decor.separation(ctx.getBindings());
				}
			}
		} else {
			ctx.setIntent(spec.getPovPurpose());
			resources(spec, values, ctx);
			ctx.setIntent(intent);
		}
	}

	private void searchDisplay(Decoration decor, Display display,
			Collection<?> values, Context ctx) throws AlibabaException,
			IOException {
		SearchPattern sp = display.getPovSearchPattern();
		Iterator<?> iter = values.iterator();
		while (iter.hasNext()) {
			// FIXME what about value?
			ElmoQuery query = sp.createElmoQuery(ctx.getFilter(), ctx
					.getOrderBy());
			resources(sp, query.getResultList(), ctx);
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
