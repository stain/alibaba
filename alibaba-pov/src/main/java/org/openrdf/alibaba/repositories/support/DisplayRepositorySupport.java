package org.openrdf.alibaba.repositories.support;

import javax.xml.namespace.QName;

import org.openrdf.alibaba.concepts.Display;
import org.openrdf.alibaba.concepts.DisplayRepository;
import org.openrdf.alibaba.repositories.DisplayRepositoryBehaviour;
import org.openrdf.alibaba.repositories.base.RepositoryBase;
import org.openrdf.alibaba.vocabulary.POV;
import org.openrdf.concepts.rdf.Property;
import org.openrdf.concepts.rdfs.Container;
import org.openrdf.elmo.ElmoManager;
import org.openrdf.elmo.annotations.rdf;

@rdf(POV.NS + "DisplayRepository")
public class DisplayRepositorySupport extends RepositoryBase<Display> implements DisplayRepositoryBehaviour {
	private ElmoManager manager;

	public DisplayRepositorySupport(DisplayRepository repository) {
		super(repository.getPovRegisteredDisplays());
		manager = repository.getElmoManager();
	}

	public Display findDisplay(QName qname) {
		return (Display) manager.find(qname);
	}

	public Display findDisplayFor(Property property) {
		for (Display display : this) {
			Container<Property> prop = display.getPovProperties();
			if (prop != null) {
				if (prop.contains(property))
					return display;
			}
		}
		return null;
	}
}
