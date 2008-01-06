package org.openrdf.alibaba.factories;

import org.openrdf.alibaba.pov.Display;
import org.openrdf.concepts.owl.DatatypeProperty;
import org.openrdf.concepts.owl.ObjectProperty;

/**
 * Methods that can be used to create Displays dynamically.
 * 
 * @author James Leigh
 * 
 */
public interface DisplayFactoryBehaviour {

	public abstract Display createDisplay();

	public abstract Display createFunctionalDisplay();

	public abstract Display createDisplay(DatatypeProperty property);

	public abstract Display createFunctionalDisplay(DatatypeProperty property);

	public abstract Display createDisplay(ObjectProperty property);

	public abstract Display createFunctionalDisplay(ObjectProperty property);

}