package org.openrdf.alibaba.factories.support;

import org.openrdf.alibaba.factories.DisplayFactory;
import org.openrdf.alibaba.factories.DisplayFactoryBehaviour;
import org.openrdf.alibaba.formats.Format;
import org.openrdf.alibaba.formats.Style;
import org.openrdf.alibaba.pov.Display;
import org.openrdf.alibaba.pov.FunctionalDisplay;
import org.openrdf.alibaba.pov.Perspective;
import org.openrdf.alibaba.pov.PropertyDisplay;
import org.openrdf.alibaba.vocabulary.ALI;
import org.openrdf.alibaba.vocabulary.POV;
import org.openrdf.concepts.owl.DatatypeProperty;
import org.openrdf.concepts.owl.ObjectProperty;
import org.openrdf.elmo.ElmoManager;
import org.openrdf.elmo.annotations.rdf;

@rdf(POV.NS + "DisplayFactory")
public class DisplayFactorySupport implements DisplayFactoryBehaviour {
	private ElmoManager manager;

	public DisplayFactorySupport(DisplayFactory factory) {
		this.manager = factory.getElmoManager();
	}

	public Display createDisplay() {
		Display display = manager.create(Display.class);
		display.setPovStyle((Style) manager.find(ALI.NORMAL));
		return display;
	}

	public Display createFunctionalDisplay() {
		Display display = manager
				.create(Display.class, FunctionalDisplay.class);
		display.setPovStyle((Style) manager.find(ALI.NORMAL));
		return display;
	}

	public Display createDisplay(DatatypeProperty property) {
		PropertyDisplay display = manager.create(PropertyDisplay.class);
		String label = property.getRdfsLabel();
		if (label == null) {
			display.setRdfsLabel(property.getQName().getLocalPart());
		} else {
			display.setRdfsLabel(label);
		}
		display.setRdfsComment(property.getRdfsComment());
		display.setPovProperty(property);
		display.setPovStyle((Style) manager.find(ALI.NORMAL));
		display.setPovFormat((Format) manager.find(ALI.NONE));
		display.setPovName(property.getQName().getLocalPart());
		return display;
	}

	public Display createFunctionalDisplay(DatatypeProperty property) {
		PropertyDisplay display = manager.create(PropertyDisplay.class,
				FunctionalDisplay.class);
		String label = property.getRdfsLabel();
		if (label == null) {
			display.setRdfsLabel(property.getQName().getLocalPart());
		} else {
			display.setRdfsLabel(label);
		}
		display.setRdfsComment(property.getRdfsComment());
		display.setPovProperty(property);
		display.setPovStyle((Style) manager.find(ALI.NORMAL));
		display.setPovFormat((Format) manager.find(ALI.NONE));
		display.setPovName(property.getQName().getLocalPart());
		return display;
	}

	public Display createDisplay(ObjectProperty property) {
		PropertyDisplay display = manager.create(PropertyDisplay.class);
		String label = property.getRdfsLabel();
		if (label == null) {
			display.setRdfsLabel(property.getQName().getLocalPart());
		} else {
			display.setRdfsLabel(label);
		}
		display.setRdfsComment(property.getRdfsComment());
		display.setPovProperty(property);
		display.setPovStyle((Style) manager.find(ALI.NORMAL));
		display.setPovPerspective((Perspective) manager.find(ALI.REFERENCE));
		display.setPovName(property.getQName().getLocalPart());
		return display;
	}

	public Display createFunctionalDisplay(ObjectProperty property) {
		PropertyDisplay display = manager.create(PropertyDisplay.class,
				FunctionalDisplay.class);
		String label = property.getRdfsLabel();
		if (label == null) {
			display.setRdfsLabel(property.getQName().getLocalPart());
		} else {
			display.setRdfsLabel(label);
		}
		display.setRdfsComment(property.getRdfsComment());
		display.setPovProperty(property);
		display.setPovStyle((Style) manager.find(ALI.NORMAL));
		display.setPovPerspective((Perspective) manager.find(ALI.REFERENCE));
		display.setPovName(property.getQName().getLocalPart());
		return display;
	}
}
