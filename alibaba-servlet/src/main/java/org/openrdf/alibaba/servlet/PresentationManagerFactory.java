package org.openrdf.alibaba.servlet;

import java.util.Locale;

/**
 * A interface to create and initialize a {@link PresentationManager}.
 * 
 * @author James Leigh
 * 
 */
public interface PresentationManagerFactory {
	public boolean isOpen();

	public void close();

	public PresentationManager createManager(Locale locale);
}
