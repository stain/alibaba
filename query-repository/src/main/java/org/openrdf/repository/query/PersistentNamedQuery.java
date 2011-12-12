/*
 * Copyright (c) 2011, 3 Round Stones Inc. Some rights reserved.
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

package org.openrdf.repository.query;

import static org.openrdf.query.parser.QueryParserUtil.createParser;
import info.aduna.io.IOUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.TimeZone;

import org.openrdf.model.URI;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.UnsupportedQueryLanguageException;
import org.openrdf.query.parser.ParsedOperation;
import org.openrdf.query.parser.QueryParser;
import org.openrdf.repository.RepositoryException;

/**
 * Support for named query persistence
 * 
 * @author Steve Battle
 * 
 */

public class PersistentNamedQuery implements NamedQuery {
	private static final String NAMED_QUERIES_DATA_DIR = "named-queries";
	/** Date format pattern used to generate the header in RFC 1123 format. */
	private static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";
	/** The time zone to use in the date header. */
	private static final TimeZone GMT = TimeZone.getTimeZone("GMT");
	private static final Random random = new Random();

	public static Map<URI, PersistentNamedQuery> persist(File dataDir)
			throws RepositoryException {
		Map<URI, PersistentNamedQuery> map = new HashMap<URI, PersistentNamedQuery>();
		File dir = new File(dataDir, NAMED_QUERIES_DATA_DIR);
		File[] files = dir.listFiles(new FilenameFilter() {
			public boolean accept(File file, String filename) {
				return filename.endsWith(".properties");
			}
		});
		if (files == null)
			return map;
		for (int i = 0; i < files.length; i++)
			try {
				PersistentNamedQuery nq = new PersistentNamedQuery(files[i]);
				map.put(nq.getUri(), nq);
			} catch (IOException e) {
				throw new RepositoryException(e);
			}
		return map;
	}

	public static void desist(File dataDir, Map<URI, PersistentNamedQuery> map)
			throws RepositoryException {
		for (PersistentNamedQuery nq : persist(dataDir).values()) {
			nq.cease();
		}
		File dir = new File(dataDir, NAMED_QUERIES_DATA_DIR);
		if (!dir.exists())
			dir.mkdir();
		for (PersistentNamedQuery nq : map.values()) {
			nq.desist(dir);
		}
	}

	private final QueryLanguage queryLang;
	private final String queryString;
	private final String baseURI;
	private final ParsedOperation parsedOperation;
	private final URI uri;
	private final String tagPrefix;
	private long tagSuffix;
	private String tag;
	private long lastModified;
	private String lastModifiedString;
	private File properties;
	private File rq;

	/* Constructor used on initial creation */

	public PersistentNamedQuery(URI uri, QueryLanguage ql, String queryString,
			String baseURI) throws RepositoryException {
		this.uri = uri;
		this.queryLang = ql;
		this.queryString = queryString;
		this.baseURI = baseURI;
		this.lastModified = System.currentTimeMillis();
		this.lastModifiedString = formatDate(this.lastModified);
		this.tagPrefix = Integer.toString(uri.hashCode(), 32);
		synchronized (random) {
			this.tagSuffix = random.nextLong();
		}
		this.tag = getNextTag();
		this.parsedOperation = parseOperation(ql, queryString, baseURI);
	}

	/* Constructor for persistence */

	private PersistentNamedQuery(File properties) throws RepositoryException,
			IOException {
		ValueFactoryImpl vf = ValueFactoryImpl.getInstance();
		Properties props = new Properties();
		props.load(openReader(properties));
		this.rq = new File(properties.getParentFile(), props
				.getProperty("queryString"));
		this.properties = properties;
		this.uri = vf.createURI(props.getProperty("uri"));
		this.queryLang = QueryLanguage.valueOf(props
				.getProperty("queryLanguage"));
		this.queryString = IOUtil.readString(this.rq);
		this.baseURI = props.getProperty("baseURI");
		this.lastModified = System.currentTimeMillis();
		this.lastModifiedString = formatDate(this.lastModified);
		this.tagPrefix = props.getProperty("tagPrefix");
		this.tagSuffix = Long.parseLong(props.getProperty("tagSuffix"), 32);
		this.tag = getNextTag();
		this.parsedOperation = parseOperation(queryLang, queryString, baseURI);
	}

	public QueryLanguage getQueryLanguage() {
		return queryLang;
	}

	public String getQueryString() {
		return queryString;
	}

	public String getBaseURI() {
		return baseURI;
	}

	public long getResultLastModified() {
		return lastModified;
	}

	public String getResultLastModifiedString() {
		return lastModifiedString;
	}

	public String getResultTag() {
		return tag;
	}

	public ParsedOperation getParsedOperation() {
		return parsedOperation;
	}

	public synchronized void update(long time) {
		lastModified = time;
		tag = getNextTag();
	}

	private ParsedOperation parseOperation(QueryLanguage ql,
			String queryString, String baseURI) throws RepositoryException {
		ParsedOperation parsedQuery;
		try {
			QueryParser parser = createParser(ql);
			try {
				parsedQuery = parser.parseQuery(queryString, baseURI);
			} catch (MalformedQueryException e) {
				try {
					parsedQuery = parser.parseUpdate(queryString, baseURI);
				} catch (MalformedQueryException u) {
					throw new RepositoryException(e);
				}
			}
		} catch (UnsupportedQueryLanguageException e) {
			throw new RepositoryException(e);
		}
		return parsedQuery;
	}

	/** cease named query persistence (delete from data-dir) */

	private synchronized void cease() {
		if (properties != null) {
			properties.delete();
			properties = null;
		}
		if (rq != null) {
			rq.delete();
			rq = null;
		}
	}

	/** desist (dehydrate) a named query */

	private synchronized void desist(File dir) throws RepositoryException {
		try {
			properties = new File(dir, getFileName(uri, ".properties"));
			rq = new File(dir, getFileName(uri, ".rq"));

			// overwrite any existing properties file
			Properties props = new Properties();
			props.setProperty("uri", uri.stringValue());
			props.setProperty("queryLanguage", getQueryLanguage().getName());
			props.setProperty("queryString", rq.getName());
			props.setProperty("baseURI", getBaseURI());
			props.setProperty("tagPrefix", tagPrefix);
			props.setProperty("tagSuffix", Long.toString(tagSuffix, 32));
			String comment = uri.stringValue();
			Writer writer = openWriter(rq);
			writer.write(getQueryString());
			writer.close();
			props.store(openWriter(properties), comment);
		} catch (IOException e) {
			throw new RepositoryException(e);
		}
	}

	private InputStreamReader openReader(File properties)
			throws UnsupportedEncodingException, FileNotFoundException {
		return new InputStreamReader(new FileInputStream(properties),
				"UTF8");
	}

	private OutputStreamWriter openWriter(File file)
			throws UnsupportedEncodingException, FileNotFoundException {
		return new OutputStreamWriter(new FileOutputStream(file),
				"UTF8");
	}

	private URI getUri() {
		return uri;
	}

	private String getFileName(URI uri, String suffix) {
		String safe = uri.getLocalName().replaceAll("[^a-zA-Z0-9\\-]", "_");
		String filename = safe + tagPrefix + suffix;
		return filename;
	}

	private String formatDate(long lastModified) {
		SimpleDateFormat dateformat;
		dateformat = new SimpleDateFormat(PATTERN_RFC1123, Locale.US);
		dateformat.setTimeZone(GMT);
		return dateformat.format(new Date(lastModified));
	}

	private String getNextTag() {
		return tagPrefix + Long.toString(tagSuffix++, 32);
	}
}
