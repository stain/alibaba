package org.openrdf.alibaba.decor.base;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.openrdf.alibaba.decor.Decoration;
import org.openrdf.alibaba.decor.Representation;
import org.openrdf.alibaba.decor.RepresentationRepository;
import org.openrdf.alibaba.decor.TextPresentation;
import org.openrdf.alibaba.decor.helpers.Context;
import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.exceptions.NotImplementedException;
import org.openrdf.alibaba.formats.Layout;
import org.openrdf.alibaba.pov.Display;
import org.openrdf.alibaba.pov.FunctionalDisplay;
import org.openrdf.alibaba.pov.Intent;
import org.openrdf.alibaba.pov.Perspective;
import org.openrdf.alibaba.pov.PerspectiveOrSearchPattern;
import org.openrdf.alibaba.pov.SearchPattern;
import org.openrdf.elmo.ElmoQuery;
import org.openrdf.elmo.Entity;

public abstract class TextPresentationBase {
	private TextPresentation pres;

	private RepresentationRepository repository;

	public TextPresentationBase(TextPresentation presentation) {
		this.pres = presentation;
		repository = pres.getPovRepresentations();
	}

	protected void presentation(Intent intent, Entity target, Context ctx)
			throws AlibabaException, IOException {
		Decoration decoration = pres.getPovDecoration();
		ctx.bind("presentation", pres);
		decoration.before(ctx.getBindings());
		representation(intent, target, ctx);
		decoration.after(ctx.getBindings());
		ctx.remove("presentation");
	}

	private void representation(Intent intent, Entity target, Context ctx)
			throws AlibabaException, IOException {
		PerspectiveOrSearchPattern psp = pres.findPerspectiveOrSearchPattern(
				intent, target);
		if (psp instanceof SearchPattern) {
			SearchPattern sp = (SearchPattern) psp;
			ElmoQuery<?> query = sp.createElmoQuery(ctx.getFilter(), ctx
					.getOrderBy());
			try {
				resources(intent, sp, query, ctx);
			} finally {
				query.close();
			}
		} else {
			assert psp instanceof Perspective : psp;
			Perspective spec = (Perspective) psp;
			if (target == null) {
				resources(intent, spec, Collections.EMPTY_SET, ctx);
			} else {
				resources(intent, spec, Collections.singleton(target), ctx);
			}
		}
	}

	protected void resources(Intent intent, PerspectiveOrSearchPattern spec,
			Iterable<?> resources, Context parent) throws AlibabaException,
			IOException {
		Layout layout = spec.getPovLayout();
		Representation rep = repository.findRepresentation(intent, layout);
		if (rep == null)
			throw new NotImplementedException("Cannot find representation for "
					+ intent + " as a " + layout);
		Decoration decor = rep.getPovDecoration();
		List<Display> displays = spec.getPovDisplays();
		Context ctx = parent.copy();
		if (spec instanceof SearchPattern) {
			ctx.bind("searchPattern", spec);
		} else {
			assert spec instanceof Perspective : spec;
			ctx.bind("perspective", spec);
		}
		ctx.bind("representation", rep);
		ctx.bind("displays", displays);
		Iterator<?> iter = resources.iterator();
		if (iter.hasNext()) {
			decor.before(ctx.getBindings());
			while (iter.hasNext()) {
				resource(intent, rep, displays, iter.next(), ctx);
				if (iter.hasNext()) {
					decor.separation(ctx.getBindings());
				}
			}
			decor.after(ctx.getBindings());
		} else {
			decor.empty(ctx.getBindings());
		}
		ctx.remove("displays");
		ctx.remove("representation");
		if (spec instanceof SearchPattern) {
			ctx.remove("searchPattern");
		} else {
			assert spec instanceof Perspective : spec;
			ctx.remove("perspective");
		}
	}

	private void resource(Intent intent, Representation rep,
			List<Display> displays, Object resource, Context ctx)
			throws AlibabaException, IOException {
		Decoration decor = rep.getPovDisplayDecoration();
		ctx.bind("resource", resource);
		Iterator<Display> jter = displays.iterator();
		if (jter.hasNext()) {
			decor.before(ctx.getBindings());
			for (int i = 0; jter.hasNext(); i++) {
				Display display = jter.next();
				ctx.bind("display", display);
				if (resource instanceof Object[]) {
					Object[] resources = (Object[]) resource;
					assert i < resources.length : displays;
					display(intent, rep, display, resources[i], ctx);
				} else {
					display(intent, rep, display, resource, ctx);
				}
				ctx.remove("display");
				if (jter.hasNext()) {
					decor.separation(ctx.getBindings());
				}
			}
			decor.after(ctx.getBindings());
		} else {
			decor.empty(ctx.getBindings());
		}
		ctx.remove("resource");
	}

	protected Decoration findDecoration(Representation rep, Display display) {
		Perspective perspective = display.getPovPerspective();
		SearchPattern searchPattern = display.getPovSearchPattern();
		if (display instanceof FunctionalDisplay) {
			if (perspective != null) {
				Decoration decor = rep.getPovFunctionalPerspectiveDecoration();
				if (decor != null)
					return decor;
			} else if (searchPattern != null) {
				Decoration decor = rep.getPovFunctionalSearchDecoration();
				if (decor != null)
					return decor;
			} else {
				Decoration decor = rep.getPovFunctionalLiteralDecoration();
				if (decor != null)
					return decor;
			}
		}
		if (perspective != null)
			return rep.getPovPerspectiveDecoration();
		if (searchPattern != null)
			return rep.getPovSearchDecoration();
		return rep.getPovLiteralDecoration();
	}

	protected abstract void display(Intent intent, Representation rep,
			Display display, Object resource, Context ctx)
			throws AlibabaException, IOException;
}
