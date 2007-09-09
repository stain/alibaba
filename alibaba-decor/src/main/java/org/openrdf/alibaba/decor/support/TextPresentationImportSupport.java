package org.openrdf.alibaba.decor.support;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrdf.alibaba.decor.Decoration;
import org.openrdf.alibaba.decor.Representation;
import org.openrdf.alibaba.decor.RepresentationRepository;
import org.openrdf.alibaba.decor.TextPresentation;
import org.openrdf.alibaba.decor.TextPresentationImportBehaviour;
import org.openrdf.alibaba.decor.UrlResolver;
import org.openrdf.alibaba.decor.helpers.Context;
import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.formats.Layout;
import org.openrdf.alibaba.pov.Display;
import org.openrdf.alibaba.pov.Intent;
import org.openrdf.alibaba.pov.LiteralDisplay;
import org.openrdf.alibaba.pov.Perspective;
import org.openrdf.alibaba.pov.PerspectiveDisplay;
import org.openrdf.alibaba.pov.PerspectiveOrSearchPattern;
import org.openrdf.alibaba.pov.SearchDisplay;
import org.openrdf.alibaba.pov.SearchPattern;
import org.openrdf.alibaba.vocabulary.DCR;
import org.openrdf.elmo.ElmoQuery;
import org.openrdf.elmo.Entity;
import org.openrdf.elmo.annotations.rdf;

@rdf(DCR.NS + "Presentation")
public class TextPresentationImportSupport implements
		TextPresentationImportBehaviour {

	private TextPresentation pres;

	private RepresentationRepository repository;

	public TextPresentationImportSupport(TextPresentation presentation) {
		this.pres = presentation;
		repository = pres.getPovRepresentations();
	}

	public void importPresentation(Intent intent, Entity target,
			Map<String, String> filter, String orderBy, UrlResolver link,
			BufferedReader reader) throws AlibabaException, IOException {
		Decoration decoration = pres.getPovDecoration();
		Context ctx = new Context(filter, orderBy);
		ctx.setUrlResolver(link);
		ctx.setReader(reader);
		ctx.bind("presentation", pres);
		decoration.before(ctx.getBindings());
		readRepresentation(intent, target, ctx);
		decoration.after(ctx.getBindings());
		ctx.remove("presentation");
	}

	private void readRepresentation(Intent intent, Entity target, Context ctx)
			throws AlibabaException, IOException {
		PerspectiveOrSearchPattern psp = pres.findPerspectiveOrSearchPattern(
				intent, target);
		if (psp instanceof SearchPattern) {
			SearchPattern sp = (SearchPattern) psp;
			ElmoQuery<?> query = sp.createElmoQuery(ctx.getFilter(), ctx
					.getOrderBy());
			try {
				readResources(intent, sp, query, ctx);
			} finally {
				query.close();
			}
		} else {
			assert psp instanceof Perspective : psp;
			Perspective spec = (Perspective) psp;
			if (target == null) {
				readResources(intent, spec, Collections.EMPTY_SET, ctx);
			} else {
				readResources(intent, spec, Collections.singleton(target), ctx);
			}
		}
	}

	private void readResources(Intent intent, PerspectiveOrSearchPattern spec,
			Iterable<?> resources, Context parent) throws AlibabaException,
			IOException {
		Layout layout = spec.getPovLayout();
		Representation rep = repository.findRepresentation(intent, layout);
		List<Display> displays = spec.getPovDisplays();
		Decoration decor = rep.getPovDecoration();
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
				readResource(intent, rep, displays, iter.next(), ctx);
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

	private void readResource(Intent intent, Representation rep,
			List<Display> displays, Object resource, Context ctx)
			throws AlibabaException, IOException {
		Decoration decor = rep.getPovDisplayDecoration();
		ctx.bind("resource", resource);
		Iterator<Display> jter = displays.iterator();
		if (jter.hasNext()) {
			decor.before(ctx.getBindings());
			for (int i = 0; jter.hasNext(); i++) {
				Display display = jter.next();
				if (resource instanceof Object[]) {
					Object[] resources = (Object[]) resource;
					assert i < resources.length : displays;
					readDisplay(intent, rep, display, resources[i], ctx);
				} else {
					readDisplay(intent, rep, display, resource, ctx);
				}
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

	private void readDisplay(Intent intent, Representation rep,
			Display display, Object resource, Context ctx)
			throws AlibabaException, IOException {
		ctx.bind("display", display);
		if (display instanceof PerspectiveDisplay) {
			readPerspectiveDisplay(intent, rep, (PerspectiveDisplay) display,
					resource, ctx);
		} else if (display instanceof SearchDisplay) {
			readSearchDisplay(intent, rep, (SearchDisplay) display, resource,
					ctx);
		} else if (display instanceof LiteralDisplay) {
			readLiteralDisplay(intent, rep, (LiteralDisplay) display, resource,
					ctx);
		} else {
			throw new AssertionError("Unknow display type for: " + display);
		}
		ctx.remove("display");
	}

	private void readPerspectiveDisplay(Intent intent, Representation rep,
			PerspectiveDisplay display, Object resource, Context ctx)
			throws AlibabaException, IOException {
		Collection<?> values = display.getValuesOf(resource);
		Decoration decor = rep.getPovPerspectiveDecoration();
		if (values.isEmpty()) {
			decor.empty(ctx.getBindings());
		} else {
			decor.before(ctx.getBindings());
			Perspective spec = display.getPovPerspective();
			if (decor.isSeparation()) {
				Iterator<?> iter = values.iterator();
				while (iter.hasNext()) {
					Set<?> singleton = Collections.singleton(iter.next());
					readResources(spec.getPovPurpose(), spec, singleton, ctx);
					if (iter.hasNext()) {
						decor.separation(ctx.getBindings());
					}
				}
			} else {
				readResources(spec.getPovPurpose(), spec, values, ctx);
			}
			decor.after(ctx.getBindings());
		}
	}

	private void readSearchDisplay(Intent intent, Representation rep,
			SearchDisplay display, Object resource, Context ctx)
			throws AlibabaException, IOException {
		Collection<?> values = display.getValuesOf(resource);
		Decoration decor = rep.getPovSearchDecoration();
		if (values.isEmpty()) {
			decor.empty(ctx.getBindings());
		} else {
			decor.before(ctx.getBindings());
			SearchPattern sp = display.getPovSearchPattern();
			Iterator<?> iter = values.iterator();
			while (iter.hasNext()) {
				// FIXME what about value?
				ElmoQuery<?> query = sp.createElmoQuery(ctx.getFilter(), ctx
						.getOrderBy());
				try {
					readResources(intent, sp, query, ctx);
				} finally {
					query.close();
				}
				if (iter.hasNext()) {
					decor.separation(ctx.getBindings());
				}
			}
			decor.after(ctx.getBindings());
		}
	}

	private void readLiteralDisplay(Intent intent, Representation rep,
			LiteralDisplay display, Object resource, Context ctx)
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
