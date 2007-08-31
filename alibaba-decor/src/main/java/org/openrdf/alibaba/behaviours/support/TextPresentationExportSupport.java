package org.openrdf.alibaba.behaviours.support;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrdf.alibaba.behaviours.TextPresentationExportBehaviour;
import org.openrdf.alibaba.behaviours.helpers.Context;
import org.openrdf.alibaba.concepts.Decoration;
import org.openrdf.alibaba.concepts.Display;
import org.openrdf.alibaba.concepts.Intent;
import org.openrdf.alibaba.concepts.Layout;
import org.openrdf.alibaba.concepts.LiteralDisplay;
import org.openrdf.alibaba.concepts.Perspective;
import org.openrdf.alibaba.concepts.PerspectiveDisplay;
import org.openrdf.alibaba.concepts.PerspectiveOrSearchPattern;
import org.openrdf.alibaba.concepts.Representation;
import org.openrdf.alibaba.concepts.RepresentationRepository;
import org.openrdf.alibaba.concepts.SearchDisplay;
import org.openrdf.alibaba.concepts.SearchPattern;
import org.openrdf.alibaba.concepts.TextPresentation;
import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.vocabulary.DCR;
import org.openrdf.elmo.ElmoQuery;
import org.openrdf.elmo.Entity;
import org.openrdf.elmo.annotations.rdf;

@rdf(DCR.NS + "Presentation")
public class TextPresentationExportSupport implements
		TextPresentationExportBehaviour {
	private TextPresentation pres;

	private RepresentationRepository repository;

	public TextPresentationExportSupport(TextPresentation presentation) {
		this.pres = presentation;
		repository = pres.getPovRepresentations();
	}

	public void exportPresentation(Intent intent, Entity target,
			Map<String, String> filter, String orderBy, PrintWriter out)
			throws AlibabaException, IOException {
		Decoration decoration = pres.getPovDecoration();
		Context ctx = new Context(filter, orderBy);
		ctx.setWriter(out);
		ctx.bind("presentation", pres);
		decoration.before(ctx.getBindings());
		printRepresentation(intent, target, ctx);
		decoration.after(ctx.getBindings());
		ctx.remove("presentation");
		out.flush();
	}

	private void printRepresentation(Intent intent, Entity target, Context ctx)
			throws AlibabaException, IOException {
		PerspectiveOrSearchPattern psp = pres.findPerspectiveOrSearchPattern(
				intent, target);
		if (psp instanceof SearchPattern) {
			SearchPattern sp = (SearchPattern) psp;
			ElmoQuery<?> query = sp.createElmoQuery(ctx.getFilter(), ctx
					.getOrderBy());
			try {
				printResources(intent, sp, query, ctx);
			} finally {
				query.close();
			}
		} else {
			assert psp instanceof Perspective : psp;
			Perspective spec = (Perspective) psp;
			if (target == null) {
				printResources(intent, spec, Collections.EMPTY_SET, ctx);
			} else {
				printResources(intent, spec, Collections.singleton(target), ctx);
			}
		}
	}

	private void printResources(Intent intent, PerspectiveOrSearchPattern spec,
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
				printResource(intent, rep, displays, iter.next(), ctx);
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

	private void printResource(Intent intent, Representation rep,
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
					printDisplay(intent, rep, display, resources[i], ctx);
				} else {
					printDisplay(intent, rep, display, resource, ctx);
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

	private void printDisplay(Intent intent, Representation rep,
			Display display, Object resource, Context ctx)
			throws AlibabaException, IOException {
		ctx.bind("display", display);
		if (display instanceof PerspectiveDisplay) {
			printPerspectiveDisplay(intent, rep, (PerspectiveDisplay) display,
					resource, ctx);
		} else if (display instanceof SearchDisplay) {
			printSearchDisplay(intent, rep, (SearchDisplay) display, resource,
					ctx);
		} else if (display instanceof LiteralDisplay) {
			printLiteralDisplay(intent, rep, (LiteralDisplay) display,
					resource, ctx);
		} else {
			throw new AssertionError("Unknow display type for: " + display);
		}
		ctx.remove("display");
	}

	private void printPerspectiveDisplay(Intent intent, Representation rep,
			PerspectiveDisplay display, Object resource, Context ctx)
			throws AlibabaException, IOException {
		Set<?> values = display.getValuesOf(resource);
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
					printResources(spec.getPovPurpose(), spec, singleton, ctx);
					if (iter.hasNext()) {
						decor.separation(ctx.getBindings());
					}
				}
			} else {
				printResources(spec.getPovPurpose(), spec, values, ctx);
			}
			decor.after(ctx.getBindings());
		}
	}

	private void printSearchDisplay(Intent intent, Representation rep,
			SearchDisplay display, Object resource, Context ctx)
			throws AlibabaException, IOException {
		Set<?> values = display.getValuesOf(resource);
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
					printResources(intent, sp, query, ctx);
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

	private void printLiteralDisplay(Intent intent, Representation rep,
			LiteralDisplay display, Object resource, Context ctx)
			throws AlibabaException, IOException {
		Set<?> values = display.getValuesOf(resource);
		Decoration decor = rep.getPovLiteralDecoration();
		if (values.isEmpty()) {
			decor.empty(ctx.getBindings());
		} else {
			decor.before(ctx.getBindings());
			decor.values(values, ctx.getBindings());
			decor.after(ctx.getBindings());
		}
	}
}
