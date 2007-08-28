package org.openrdf.alibaba.factories.support;

import org.openrdf.alibaba.concepts.Display;
import org.openrdf.alibaba.concepts.DisplayFactory;
import org.openrdf.alibaba.concepts.Format;
import org.openrdf.alibaba.concepts.LiteralDisplay;
import org.openrdf.alibaba.concepts.Perspective;
import org.openrdf.alibaba.concepts.PerspectiveDisplay;
import org.openrdf.alibaba.concepts.Style;
import org.openrdf.alibaba.factories.DisplayFactoryBehaviour;
import org.openrdf.alibaba.vocabulary.ALI;
import org.openrdf.alibaba.vocabulary.POV;
import org.openrdf.concepts.owl.DatatypeProperty;
import org.openrdf.concepts.owl.ObjectProperty;
import org.openrdf.concepts.rdf.Alt;
import org.openrdf.concepts.rdf.Property;
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

	public Display createDisplay(Format format) {
		LiteralDisplay display = manager.create(LiteralDisplay.class);
		display.setPovFormat(format);
		display.setPovStyle((Style) manager.find(ALI.NORMAL));
		return display;
	}

	public Display createDisplay(Format format, Style style) {
		LiteralDisplay display = manager.create(LiteralDisplay.class);
		display.setPovFormat(format);
		display.setPovStyle(style);
		return display;
	}

	public LiteralDisplay createBindingDisplay(String name) {
		LiteralDisplay display = manager.create(LiteralDisplay.class);
		display.setPovName(name);
		display.setPovStyle((Style) manager.find(ALI.NORMAL));
		return display;
	}

	public LiteralDisplay createBindingDisplay(String name, Format format) {
		LiteralDisplay display = manager.create(LiteralDisplay.class);
		display.setPovName(name);
		display.setPovFormat(format);
		display.setPovStyle((Style) manager.find(ALI.NORMAL));
		return display;
	}

	public Display createPropertyDisplay(DatatypeProperty property) {
		Alt<Property> alt = manager.create(Alt.class);
		alt.add(property);
		LiteralDisplay display = manager.create(LiteralDisplay.class);
		String label = property.getRdfsLabel();
		if (label == null) {
			display.setRdfsLabel(property.getQName().getLocalPart());
		} else {
			display.setRdfsLabel(label);
		}
		display.setRdfsComment(property.getRdfsComment());
		display.setPovProperties(alt);
		display.setPovStyle((Style) manager.find(ALI.NORMAL));
		display.setPovFormat((Format) manager.find(ALI.NONE));
		display.setPovName(property.getQName().getLocalPart());
		return display;
	}

	public Display createPropertyDisplay(ObjectProperty property) {
		Alt<Property> alt = manager.create(Alt.class);
		alt.add(property);
		PerspectiveDisplay display = manager.create(PerspectiveDisplay.class);
		String label = property.getRdfsLabel();
		if (label == null) {
			display.setRdfsLabel(property.getQName().getLocalPart());
		} else {
			display.setRdfsLabel(label);
		}
		display.setRdfsComment(property.getRdfsComment());
		display.setPovProperties(alt);
		display.setPovStyle((Style) manager.find(ALI.NORMAL));
		display.setPovPerspective((Perspective) manager.find(ALI.REFERENCE));
		display.setPovName(property.getQName().getLocalPart());
		return display;
	}

	public Display createPropertyDisplay(Property property, Format format) {
		Alt<Property> alt = manager.create(Alt.class);
		alt.add(property);
		LiteralDisplay display = manager.create(LiteralDisplay.class);
		display.setRdfsLabel(property.getRdfsLabel());
		display.setRdfsComment(property.getRdfsComment());
		display.setPovProperties(alt);
		display.setPovFormat(format);
		display.setPovStyle((Style) manager.find(ALI.NORMAL));
		display.setPovName(property.getQName().getLocalPart());
		return display;
	}
}
