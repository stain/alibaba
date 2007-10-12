package org.openrdf.alibaba.servlet.impl;

import javax.xml.namespace.QName;

import org.openrdf.alibaba.decor.PresentationService;
import org.openrdf.alibaba.pov.Intent;
import org.openrdf.alibaba.servlet.PresentationManager;
import org.openrdf.alibaba.vocabulary.ALI;
import org.openrdf.concepts.rdfs.Class;
import org.openrdf.elmo.ElmoManager;

public class PresentationManagerImpl implements PresentationManager {
	private ElmoManager manager;

	public PresentationManagerImpl(ElmoManager manager) {
		this.manager = manager;
	}

	public boolean isOpen() {
		return manager.isOpen();
	}

	public void close() {
		manager.close();
	}

	public PresentationService getPresentationService() {
		return manager.designate(PresentationService.class,
				ALI.PRESENTATION_SERVICE);
	}

	public Class findClass(QName type) {
		if (type == null)
			return null;
		return manager.designate(Class.class, type);
	}

	public Intent findIntent(QName intent) {
		if (intent == null)
			return null;
		return manager.designate(Intent.class, intent);
	}

}
