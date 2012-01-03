package org.openrdf.repository.object.compiler;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.openrdf.rio.turtle.TurtleParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NamespacePrefixService {
	private static final String PREFIX_LOOKUP = "http://prefix.cc/reverse?format=ttl&uri=";

	public static NamespacePrefixService getInstance() {
		return new NamespacePrefixService();
	}

	private final Logger logger = LoggerFactory
			.getLogger(NamespacePrefixService.class);
	private Map<String, String> prefixes = new HashMap<String, String>();

	private NamespacePrefixService() {
		super();
	}

	public synchronized String prefix(final String ns) {
		try {
			if (prefixes.containsKey(ns))
				return prefixes.get(ns);
			URL url = new URL(PREFIX_LOOKUP + URLEncoder.encode(ns, "UTF-8"));
			logger.info("Requesting {}", url);
			URLConnection con = url.openConnection();
			con.addRequestProperty("Accept", "text/turtle");
			InputStream in = con.getInputStream();
			try {
				TurtleParser parser = new TurtleParser();
				final List<String> match = new ArrayList<String>();
				parser.setRDFHandler(new RDFHandlerBase() {
					public void handleNamespace(String prefix, String uri)
							throws RDFHandlerException {
						if (uri.equals(ns)) {
							match.add(prefix);
						}
					}
				});
				parser.parse(in, PREFIX_LOOKUP + URLEncoder.encode(ns, "UTF-8"));
				if (match.size() > 0) {
					String prefix = match.get(0);
					prefixes.put(ns, prefix);
					return prefix;
				}
				prefixes.put(ns, null);
			} finally {
				in.close();
			}
		} catch (FileNotFoundException e) {
			prefixes.put(ns, null);
			logger.trace("Unknown namespace {}", ns);
		} catch (Exception e) {
			logger.error(e.toString(), e);
		}
		return null;
	}
}
