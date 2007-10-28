package org.openrdf.alibaba.decor.support;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import javax.xml.namespace.QName;

import org.openrdf.alibaba.decor.Content;
import org.openrdf.alibaba.decor.Presentation;
import org.openrdf.alibaba.decor.PresentationRepository;
import org.openrdf.alibaba.decor.PresentationService;
import org.openrdf.alibaba.decor.PresentationServiceBehaviour;
import org.openrdf.alibaba.decor.Response;
import org.openrdf.alibaba.decor.TextPresentation;
import org.openrdf.alibaba.decor.UrlResolver;
import org.openrdf.alibaba.decor.helpers.Context;
import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.exceptions.NotImplementedException;
import org.openrdf.alibaba.factories.PerspectiveFactory;
import org.openrdf.alibaba.pov.Intent;
import org.openrdf.alibaba.pov.Perspective;
import org.openrdf.alibaba.pov.PerspectiveOrSearchPattern;
import org.openrdf.alibaba.pov.PerspectiveRepository;
import org.openrdf.alibaba.pov.SearchPattern;
import org.openrdf.alibaba.pov.SearchPatternRepository;
import org.openrdf.alibaba.vocabulary.DCR;
import org.openrdf.concepts.rdfs.Class;
import org.openrdf.concepts.rdfs.Resource;
import org.openrdf.elmo.Entity;
import org.openrdf.elmo.annotations.rdf;

@rdf(DCR.NS + "PresentationService")
public class PresentationServiceSupport implements PresentationServiceBehaviour {
	private PresentationService service;

	public PresentationServiceSupport(PresentationService service) {
		this.service = service;
	}

	public QName create(Entity resource, Class type,
			Content source, Intent intent) throws AlibabaException, IOException {
		String ctype = source.getContentType();
		Intent i = findIntention(intent);
		Presentation present = findPresentation(i, ctype);
		Entity target = createBean(resource, type);
		importPresentation(present, i, source, target);
		return target.getQName();
	}

	public long getLastModified(Entity target, Intent intent) {
		return System.currentTimeMillis();
	}

	public void remove(Entity target)
			throws AlibabaException {
		target.getElmoManager().remove(target);
	}

	public void retrieve(Entity target, Response resp,
			Intent intent) throws AlibabaException, IOException {
		String[] types = resp.getAcceptedTypes();
		Intent i = findIntention(intent);
		Presentation present = findPresentation(i, types);
		resp.setContentType(present.getPovContentType());
		resp.setLocale(target.getElmoManager().getLocale());
		exportPresentation(present, i, target, resp);
	}

	public void save(Entity target, Content source,
			Intent intent) throws AlibabaException, IOException {
		String ctype = source.getContentType();
		Intent i = findIntention(intent);
		Presentation present = findPresentation(i, ctype);
		importPresentation(present, i, source, target);
	}

	private void importPresentation(Presentation present,
			Intent i, Content source, Entity target) throws IOException,
			AlibabaException {
		if (present instanceof TextPresentation) {
			BufferedReader out = source.getReader();
			TextPresentation pres = (TextPresentation) present;
			Context ctx = new Context();
			ctx.setElmoManager(target.getElmoManager());
			ctx.setIntent(i);
			ctx.setReader(out);
			ctx.setLocale(source.getLocale());
			PerspectiveOrSearchPattern spec;
			spec = this.findPerspectiveOrSearchPattern(i, target);
			pres.importPresentation(spec, target, ctx);
		} else {
			throw new NotImplementedException();
		}
	}

	private void exportPresentation(Presentation present,
			Intent i, Entity target, Response resp) throws AlibabaException,
			IOException {
		if (present instanceof TextPresentation) {
			TextPresentation pres = (TextPresentation) present;
			UrlResolver link = resp.getUrlResolver();
			PrintWriter out = resp.getWriter();
			Context ctx = new Context();
			ctx.setElmoManager(target.getElmoManager());
			ctx.setIntent(i);
			ctx.setUrlResolver(link);
			ctx.setWriter(out);
			ctx.setLocale(resp.getLocale());
			PerspectiveOrSearchPattern spec;
			spec = this.findPerspectiveOrSearchPattern(i, target);
			pres.exportPresentation(spec, target, ctx);
			out.flush();
		} else {
			throw new NotImplementedException();
		}
	}

	private Intent findIntention(Intent intent) {
		if (intent == null)
			return service.getPovDefaultIntention();
		return intent;
	}

	private Presentation findPresentation(Intent intent, String... accept)
			throws AlibabaException {
		PresentationRepository repository = service.getPovPresentations();
		Presentation presentation = repository.findPresentation(intent, accept);
		if (presentation == null)
			throw new NotImplementedException("No presentation for: "
					+ Arrays.asList(accept));
		return presentation;
	}

	private PerspectiveOrSearchPattern findPerspectiveOrSearchPattern(
			Intent intent, Entity target) {
		if (target instanceof Class) {
			SearchPatternRepository spr = service.getPovSearchPatterns();
			SearchPattern sp = spr.findSearchPattern(intent, (Class) target);
			if (sp != null) {
				return sp;
			}
		}
		PerspectiveRepository repo = service.getPovPerspectives();
		Perspective spec = repo.findPerspective(intent, target);
		if (spec == null) {
			spec = createPerspective(intent, target);
		}
		return spec;
	}

	private Perspective createPerspective(Intent intent, Entity target) {
		PerspectiveFactory factory = service.getPovPerspectiveFactory();
		if (factory == null)
			return null;
		Perspective spec = factory.createPerspectiveFor(intent, target);
		PerspectiveRepository repo = service.getPovPerspectives();
		repo.getPovRegisteredPerspectives().add(spec);
		return spec;
	}

	private Entity createBean(Entity target, Class type) {
		Resource bean = (Resource) target;
		bean.getRdfTypes().add(type);
		return target.getElmoManager().find(target.getQName());
	}

}
