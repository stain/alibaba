package org.openrdf.alibaba.decor.support;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrdf.alibaba.decor.Decoration;
import org.openrdf.alibaba.decor.Representation;
import org.openrdf.alibaba.decor.TextPresentation;
import org.openrdf.alibaba.decor.TextPresentationImportBehaviour;
import org.openrdf.alibaba.decor.base.TextPresentationBase;
import org.openrdf.alibaba.decor.helpers.Context;
import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.exceptions.NotImplementedException;
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
public class TextPresentationImportSupport extends TextPresentationBase
		implements TextPresentationImportBehaviour {

	public TextPresentationImportSupport(TextPresentation presentation) {
		super(presentation);
	}

	public void importPresentation(Intent intent,
			PerspectiveOrSearchPattern spec, Entity target, Context ctx)
			throws AlibabaException, IOException {
		presentation(intent, spec, target, ctx);
	}

	@Override
	protected void resources(Intent intent, Representation rep,
			Decoration decor, List<Display> displays, Collection<?> resources,
			Context ctx) throws AlibabaException, IOException {
		Map<String, Object> b = ctx.getBindings();
		if (decor.isBefore(b)) {
			decor.before(b);
			Decoration displayDecor = rep.getPovDisplayDecoration();
			List<Object> pool = new ArrayList<Object>(resources);
			List<Object> found = new ArrayList<Object>(pool.size());
			do {
				Object resource = findNextMatch(pool, displayDecor, ctx);
				found.add(resource);
				resource(intent, rep, displays, resource, ctx);
				if (decor.isAfter(b))
					break;
				decor.separation(b);
			} while (true);
			decor.after(b);
			resources.retainAll(found);
			if (resources.size() != found.size()) {
				found.removeAll(resources);
				Collection coll = resources;
				coll.addAll(found);
			}
		} else {
			decor.empty(b);
			resources.clear();
		}
	}

	private Object findNextMatch(List<Object> pool, Decoration decor,
			Context ctx) throws AlibabaException, IOException {
		Iterator<Object> iter = pool.iterator();
		while (iter.hasNext()) {
			Object resource = iter.next();
			ctx.bind("resource", resource);
			if (decor.isBefore(ctx.getBindings())) {
				iter.remove();
				return resource;
			}
		}
		// TODO create new
		throw new NotImplementedException("add bnode");
	}

	@Override
	protected void display(Intent intent, Representation rep, Display display,
			Object resource, Context ctx) throws AlibabaException, IOException {
		Decoration decor = rep.findDecorationFor(display);
		if (decor.isBefore(ctx.getBindings())) {
			decor.before(ctx.getBindings());
			if (display.getPovPerspective() != null) {
				perspectiveDisplay(decor, display, resource, ctx);
			} else if (display.getPovSearchPattern() != null) {
				searchDisplay(intent, decor, display, resource, ctx);
			} else {
				literalDisplay(decor, display, resource, ctx);
			}
			decor.after(ctx.getBindings());
		} else {
			decor.empty(ctx.getBindings());
			display.setValuesOf(resource, Collections.EMPTY_SET);
		}
	}

	private void perspectiveDisplay(Decoration decor, Display display,
			Object resource, Context ctx) throws AlibabaException, IOException {
		Collection<?> values = new ArrayList<Object>(display
				.getValuesOf(resource));
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
			display.setValuesOf(resource, values);
		}
	}

	private void searchDisplay(Intent intent, Decoration decor,
			Display display, Object resource, Context ctx)
			throws AlibabaException, IOException {
		Collection<?> values = display.getValuesOf(resource);
		SearchPattern sp = display.getPovSearchPattern();
		Iterator<?> iter = values.iterator();
		while (iter.hasNext()) {
			// FIXME what about value?
			ElmoQuery<?> query = sp.createElmoQuery(ctx.getFilter(), ctx
					.getOrderBy());
			resources(intent, sp, query.getResultList(), ctx);
			if (iter.hasNext()) {
				decor.separation(ctx.getBindings());
			}
		}
	}

	private void literalDisplay(Decoration decor, Display display,
			Object resource, Context ctx) throws AlibabaException, IOException {
		Set<Object> values = new LinkedHashSet<Object>();
		decor.values(values, ctx.getBindings());
		display.setValuesOf(resource, values);
	}
}
