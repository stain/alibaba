package org.openrdf.alibaba.pov.support;

import javax.xml.namespace.QName;

import org.openrdf.alibaba.core.base.RepositoryBase;
import org.openrdf.alibaba.pov.Display;
import org.openrdf.alibaba.pov.DisplayRepository;
import org.openrdf.alibaba.pov.DisplayRepositoryBehaviour;
import org.openrdf.alibaba.vocabulary.POV;
import org.openrdf.elmo.ElmoManager;
import org.openrdf.elmo.annotations.rdf;

/**
 * Support for a collection of active displays.
 * 
 * @author James Leigh
 * 
 */
@rdf(POV.NS + "DisplayRepository")
public class DisplayRepositorySupport extends RepositoryBase<Display> implements
		DisplayRepositoryBehaviour {
	private ElmoManager manager;

	public DisplayRepositorySupport(DisplayRepository repository) {
		super(repository.getPovRegisteredDisplays());
		manager = repository.getElmoManager();
	}

	public Display findDisplay(QName qname) {
		return (Display) manager.find(qname);
	}
}
