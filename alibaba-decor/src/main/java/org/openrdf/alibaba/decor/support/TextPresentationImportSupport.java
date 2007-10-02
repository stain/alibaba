package org.openrdf.alibaba.decor.support;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.openrdf.alibaba.decor.Decoration;
import org.openrdf.alibaba.decor.Representation;
import org.openrdf.alibaba.decor.TextPresentation;
import org.openrdf.alibaba.decor.TextPresentationImportBehaviour;
import org.openrdf.alibaba.decor.base.TextPresentationBase;
import org.openrdf.alibaba.decor.helpers.Context;
import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.pov.Display;
import org.openrdf.alibaba.pov.Intent;
import org.openrdf.alibaba.pov.Perspective;
import org.openrdf.alibaba.pov.SearchPattern;
import org.openrdf.alibaba.vocabulary.DCR;
import org.openrdf.elmo.ElmoQuery;
import org.openrdf.elmo.Entity;
import org.openrdf.elmo.annotations.rdf;

@rdf(DCR.NS + "Presentation")
public class TextPresentationImportSupport extends TextPresentationBase
		implements TextPresentationImportBehaviour {

	public TextPresentationImportSupport(TextPresentation presentation) {
		super(presentation);
	}

	public void importPresentation(Intent intent, Entity target, Context ctx)
			throws AlibabaException, IOException {
		presentation(intent, target, ctx);
	}

	@Override
	protected void display(Intent intent, Representation rep, Display display,
			Object resource, Context ctx) throws AlibabaException, IOException {
		if (display.getPovPerspective() != null) {
			perspectiveDisplay(intent, rep, display, resource, ctx);
		} else if (display.getPovSearchPattern() != null) {
			searchDisplay(intent, rep, display, resource, ctx);
		} else {
			literalDisplay(intent, rep, display, resource, ctx);
		}
	}

	protected void perspectiveDisplay(Intent intent, Representation rep,
			Display display, Object resource, Context ctx)
			throws AlibabaException, IOException {
		Collection<?> values = display.getValuesOf(resource);
		Decoration decor = rep.getPovPerspectiveDecoration();
		if (values.isEmpty()) {
			decor.empty(ctx.getBindings());
		} else {
			decor.before(ctx.getBindings());
			perspectiveDisplay(decor, display, values, ctx);
			decor.after(ctx.getBindings());
		}
	}

	private void perspectiveDisplay(Decoration decor, Display display,
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

	protected void searchDisplay(Intent intent, Representation rep,
			Display display, Object resource, Context ctx)
			throws AlibabaException, IOException {
		Collection<?> values = display.getValuesOf(resource);
		Decoration decor = rep.getPovSearchDecoration();
		if (values.isEmpty()) {
			decor.empty(ctx.getBindings());
		} else {
			decor.before(ctx.getBindings());
			searchDisplay(intent, decor, display, values, ctx);
			decor.after(ctx.getBindings());
		}
	}

	private void searchDisplay(Intent intent, Decoration decor,
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

	private void literalDisplay(Intent intent, Representation rep,
			Display display, Object resource, Context ctx)
			throws AlibabaException, IOException {
		Decoration decor = rep.getPovLiteralDecoration();
		if (decor.isEmpty(ctx.getBindings())) {
			decor.empty(ctx.getBindings());
			display.setValuesOf(resource, Collections.EMPTY_SET);
		} else {
			decor.before(ctx.getBindings());
			Set<Object> values = new LinkedHashSet<Object>();
			decor.values(values, ctx.getBindings());
			display.setValuesOf(resource, values);
			decor.after(ctx.getBindings());
		}
	}
}
