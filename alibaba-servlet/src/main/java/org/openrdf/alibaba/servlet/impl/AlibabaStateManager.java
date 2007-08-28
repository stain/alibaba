package org.openrdf.alibaba.servlet.impl;

import java.io.BufferedReader;
import java.io.IOException;

import javax.xml.namespace.QName;

import org.openrdf.alibaba.concepts.Intent;
import org.openrdf.alibaba.concepts.Presentation;
import org.openrdf.alibaba.concepts.PresentationRepository;
import org.openrdf.alibaba.concepts.TextPresentation;
import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.exceptions.NotImplementedException;
import org.openrdf.alibaba.servlet.Content;
import org.openrdf.alibaba.servlet.Response;
import org.openrdf.alibaba.servlet.StateManager;
import org.openrdf.alibaba.vocabulary.ALI;
import org.openrdf.concepts.rdfs.Class;
import org.openrdf.concepts.rdfs.Resource;
import org.openrdf.elmo.ElmoManager;
import org.openrdf.elmo.ElmoManagerFactory;
import org.openrdf.elmo.Entity;

public class AlibabaStateManager implements StateManager {
	private long lastModified = System.currentTimeMillis();

	private ElmoManagerFactory factory;

	private QName presentationRepository = ALI.PRESENTATION_REPOSITORY;

	private QName defaultIntention = ALI.GENERAL;

	public void setElmoManagerFactory(ElmoManagerFactory factory) {
		this.factory = factory;
	}

	public void setPresentationRepository(QName presentationRepository) {
		this.presentationRepository = presentationRepository;
	}

	public void setDefaultIntention(QName defaultIntention) {
		this.defaultIntention = defaultIntention;
	}

	public QName create(QName resource, QName type, Content source, QName intent)
			throws AlibabaException, IOException {
		ElmoManager manager = factory.createElmoManager(source.getLocale());
		try {
			manager.setAutoFlush(false);
			String ctype = source.getContentType();
			Presentation present = findPresentation(manager, ctype);
			Entity target = createBean(manager, resource, type);
			Intent i = findIntention(manager, intent);
			importPresentation(present, i, source, target);
			lastModified = System.currentTimeMillis();
			return target.getQName();
		} finally {
			manager.flush();
			manager.close();
		}
	}

	public void remove(QName resource) {
		ElmoManager manager = factory.createElmoManager();
		try {
			manager.setAutoFlush(false);
			Entity target = manager.find(resource);
			manager.remove(target);
			lastModified = System.currentTimeMillis();
		} finally {
			manager.flush();
			manager.close();
		}
	}

	public void retrieve(QName resource, Response resp, QName intent)
			throws AlibabaException, IOException {
		ElmoManager manager = factory.createElmoManager(resp.getLocale());
		try {
			manager.setAutoFlush(false);
			String[] types = resp.getAcceptedTypes();
			Presentation present = findPresentation(manager, types);
			resp.setContentType(present.getPovContentType());
			resp.setLocale(manager.getLocale());
			Entity target = manager.find(resource);
			Intent i = findIntention(manager, intent);
			exportPresentation(present, i, target, resp);
		} finally {
			manager.refresh(); // FIXME
			manager.close();
		}
	}

	public void save(QName resource, Content source, QName intent)
			throws AlibabaException, IOException {
		ElmoManager manager = factory.createElmoManager(source.getLocale());
		try {
			manager.setAutoFlush(false);
			String ctype = source.getContentType();
			Presentation present = findPresentation(manager, ctype);
			Entity target = manager.find(resource);
			Intent i = findIntention(manager, intent);
			importPresentation(present, i, source, target);
			lastModified = System.currentTimeMillis();
		} finally {
			manager.flush();
			manager.close();
		}
	}

	public long getLastModified(QName resource) {
		return lastModified;
	}

	private void importPresentation(Presentation present, Intent i, Content source, Entity target) throws IOException, AlibabaException {
		if (present instanceof TextPresentation) {
			BufferedReader out = source.getReader();
			TextPresentation pres = (TextPresentation) present;
			pres.importPresentation(i, target, null, null, out);
		} else {
			throw new NotImplementedException();
		}
	}

	private void exportPresentation(Presentation present, Intent i, Entity target, Response resp) throws AlibabaException, IOException {
		if (present instanceof TextPresentation) {
			TextPresentation pres = (TextPresentation) present;
			pres.exportPresentation(i, target, null, null, resp.getWriter());
		} else {
			throw new NotImplementedException();
		}
	}

	private Intent findIntention(ElmoManager manager, QName intent) {
		if (intent == null)
			return (Intent) manager.find(defaultIntention);
		return (Intent) manager.find(intent);
	}

	private Presentation findPresentation(ElmoManager manager, String... accept) {
		PresentationRepository repository = (PresentationRepository) manager
				.find(presentationRepository);
		return repository.findPresentation(accept);
	}

	private Entity createBean(ElmoManager manager, QName resource, QName type) {
		Resource bean = (Resource) manager.find(resource);
		Class c = manager.create(Class.class, type);
		bean.getRdfTypes().add(c);
		return manager.find(resource);
	}

}