/*
 * Copyright (c) 2007-2009, James Leigh All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.openrdf.elmo.sesame;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.PersistenceException;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.openrdf.elmo.ElmoModule;
import org.openrdf.repository.Repository;
import org.openrdf.repository.http.HTTPRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

/**
 * Used in META-INF/persistence.xml to indicate a SesameManager should be used.
 * 
 * @author James Leigh
 * 
 */
public class SesamePersistenceProvider implements PersistenceProvider {
	private Logger logger = LoggerFactory.getLogger(SesamePersistenceProvider.class);
	/** persistence.xml */
	private final static String PERSISTENCE_XML = "META-INF/persistence.xml";

	private static final String PROVIDER_NAME = SesamePersistenceProvider.class
			.getName();

	public SesameManagerFactory createContainerEntityManagerFactory(
			PersistenceUnitInfo info, Map map) {
		String className = info.getPersistenceProviderClassName();
		if (className != null && !PROVIDER_NAME.equals(className))
			return null;
		try {
			ClassLoader cl = info.getClassLoader();
			List<URL> jarFileUrls = new ArrayList<URL>();
			if (!info.excludeUnlistedClasses()) {
				jarFileUrls.add(info.getPersistenceUnitRootUrl());
			}
			jarFileUrls.addAll(info.getJarFileUrls());
			List<String> names = info.getManagedClassNames();
			List<String> types = new ArrayList<String>(names.size());
			for (String name : names) {
				types.add(name);
			}
			Map props = new HashMap();
			props.putAll(info.getProperties());
			if (map != null) {
				props.putAll(map);
			}
			return createSesameManagerFactory(cl, props, types, jarFileUrls);
		} catch (Exception e) {
			throw new PersistenceException(e);
		}
	}

	public SesameManagerFactory createEntityManagerFactory(String emName,
			Map map) {
		try {
			URL unitUrl = findPresistenceResource(emName);
			Element unit = findPresistenceUnit(emName, unitUrl);
			NodeList providers = unit.getElementsByTagName("provider");
			if (providers.getLength() > 0) {
				Element provider = (Element) providers.item(0);
				if (!PROVIDER_NAME.equals(getTextContent(provider)))
					return null;
			}
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			boolean exclude = getBoolean(unit, "exclude-unlisted-classes");
			NodeList jarFiles = unit.getElementsByTagName("jar-file");
			List<URL> jarFileUrls = new ArrayList<URL>(jarFiles.getLength());
			if (!exclude || jarFiles.getLength() > 0) {
				URL base = new URL(unitUrl, "../");
				if (!exclude) {
					jarFileUrls.add(base);
				}
				for (int i = 0, n = jarFiles.getLength(); i < n; i++) {
					String url = getTextContent(jarFiles.item(i));
					jarFileUrls.add(new URL(base, url));
				}
			}
			NodeList names = unit.getElementsByTagName("class");
			List<String> types = new ArrayList<String>(names.getLength());
			for (int i = 0, n = names.getLength(); i < n; i++) {
				String name = getTextContent(names.item(i));
				types.add(name);
			}
			Map props = new HashMap();
			props.putAll(getProperties(unit));
			if (map != null) {
				props.putAll(map);
			}
			return createSesameManagerFactory(cl, props, types, jarFileUrls);
		} catch (Exception e) {
			throw new PersistenceException(e);
		}
	}

	private boolean getBoolean(Element unit, String tagName) {
		NodeList excludes = unit.getElementsByTagName(tagName);
		return excludes.getLength() > 0
				&& Boolean.valueOf(getTextContent(excludes.item(0)));
	}

	private String getTextContent(Node node) {
		node.normalize();
		Text text = (Text) node.getFirstChild();
		return text.getData().trim();
	}

	private SesameManagerFactory createSesameManagerFactory(ClassLoader cl,
			Map<String, String> props, List<String> types,
			List<URL> jarFileUrls) throws NamingException {
		ElmoModule module = new ElmoModule(cl);
		if (jarFileUrls != null) {
			for (URL url : jarFileUrls) {
				module.addJarFileUrl(url);
			}
		}
		if (types != null) {
			ClassLoader loader = module.getClassLoader();
			for (String type : types) {
				try {
					module.addConcept(Class.forName(type, true, loader));
				} catch (ClassNotFoundException e) {
					logger.warn(e.toString(), e);
				}
			}
		}
		return createSesameManagerFactory(module, props);
	}

	private SesameManagerFactory createSesameManagerFactory(ElmoModule module,
			Map<String, String> props) throws NamingException {
		String url = props.get("repository");
		Repository repository = findRepository(url);
		return new SesameManagerFactory(module, repository);
	}

	private Repository findRepository(String url) throws NamingException {
		if (url.startsWith("http")) {
			return new HTTPRepository(url);
		}
		return (Repository)new InitialContext().lookup(url);
	}

	private URL findPresistenceResource(String emName) throws SAXException,
			IOException, ParserConfigurationException {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder parser = dbf.newDocumentBuilder();
		Enumeration<URL> resources = cl.getResources(PERSISTENCE_XML);
		while (resources.hasMoreElements()) {
			URL url = resources.nextElement();
			InputStream stream = url.openStream();
			try {
				Document doc = parser.parse(stream);
				NodeList list = doc.getElementsByTagName("persistence-unit");
				for (int i = 0, n = list.getLength(); i < n; i++) {
					Element item = (Element) list.item(i);
					if (emName.equals(item.getAttribute("name")))
						return url;
				}
			} finally {
				stream.close();
			}
		}
		throw new IllegalArgumentException(
				"Cannot find persistence-unit with name: " + emName);
	}

	private Element findPresistenceUnit(String emName, URL url)
			throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder parser = dbf.newDocumentBuilder();
		InputStream stream = url.openStream();
		try {
			Document doc = parser.parse(stream);
			NodeList list = doc.getElementsByTagName("persistence-unit");
			for (int i = 0, n = list.getLength(); i < n; i++) {
				Element item = (Element) list.item(i);
				if (emName.equals(item.getAttribute("name")))
					return item;
			}
		} finally {
			stream.close();
		}
		throw new IllegalArgumentException(
				"Cannot find persistence-unit with name: " + emName);
	}

	private Map<String, String> getProperties(Element unit) {
		Map<String, String> properties = new HashMap<String, String>();
		NodeList list = unit.getElementsByTagName("property");
		for (int i = 0, n = list.getLength(); i < n; i++) {
			Element item = (Element) list.item(i);
			properties.put(item.getAttribute("name"), item
					.getAttribute("value"));
		}
		return properties;
	}

}
