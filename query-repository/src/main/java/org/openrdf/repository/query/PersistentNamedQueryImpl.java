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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.Properties;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.UnsupportedQueryLanguageException;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Support for named query persistence
 * 
 * @author Steve Battle
 *
 */

public class PersistentNamedQueryImpl extends NamedQueryBase {
	
	private static final Logger logger = LoggerFactory.getLogger(PersistentNamedQueryImpl.class) ;	
	public static final String NAMED_QUERIES_DATA_DIR = "named-queries";
	
	/* Constructor used on initial creation */

	public PersistentNamedQueryImpl(URI uri, QueryLanguage ql, String queryString, String baseURI) 
	throws MalformedQueryException, UnsupportedQueryLanguageException, RepositoryException {
		super(uri, ql, queryString, baseURI);		
	}
	
	/* Constructors for persistence */
		
	public PersistentNamedQueryImpl
	(long eTagPrefix, long eTagSuffix, URI uri, QueryLanguage ql, String queryString, String baseURI, long lastModified) 
	throws MalformedQueryException, UnsupportedQueryLanguageException, RepositoryException {
		super(eTagPrefix, eTagSuffix, uri, ql, queryString, baseURI, lastModified) ;
	}
	
	public PersistentNamedQueryImpl(File dataDir, Properties props, ValueFactory vf) 
	throws IOException, MalformedQueryException, UnsupportedQueryLanguageException, RepositoryException {
		super(
			Long.parseLong(props.getProperty("eTagPrefix")), 
			Long.parseLong(props.getProperty("eTagSuffix")), 
			vf.createURI(props.getProperty("uri")), 
			new QueryLanguage(props.getProperty("queryLanguage")), 
			load(new File(new File(dataDir, NAMED_QUERIES_DATA_DIR), props.getProperty("queryString"))), 
			props.getProperty("baseURI"), 
			Long.parseLong(props.getProperty("lastModified"))
		) ;
	}
	
	public static File getDataDir(File dataDir) {
		File dir = new File(dataDir, NAMED_QUERIES_DATA_DIR) ;
		if (!dir.exists()) dir.mkdir() ;
		return dir ;
	}
	
	/* cease named query persistence (delete from data-dir) */
	
	public void cease(File dataDir) {
		String name = stripSymbols(uri.getLocalName()) + Long.toString(eTagPrefix, 32) ;
		File propsFile = new File(dataDir, name+".properties") ;
		propsFile.delete() ;
		File rq = new File(dataDir, name+".rq") ;
		rq.deleteOnExit() ;
	}
	
	/* desist (dehydrate) a named query */
	
	public void desist(File dataDir) throws RepositoryException {
		try {
			String name = stripSymbols(uri.getLocalName()) + Long.toString(eTagPrefix, 32) ;
			File propsFile = new File(dataDir, name+".properties") ;
			File rq = new File(dataDir, name+".rq") ;
	
			// overwrite any existing properties file
			Properties props = new Properties() ;
			props.setProperty("uri", uri.stringValue()) ;
			props.setProperty("queryLanguage", queryLang.getName()) ;
			props.setProperty("queryString", rq.getName()) ;
			props.setProperty("baseURI", baseURI) ;
			props.setProperty("lastModified", Long.toString(lastModified)) ;
			props.setProperty("eTagPrefix", Long.toString(eTagPrefix)) ;
			props.setProperty("eTagSuffix", Long.toString(eTagSuffix)) ;
			props.store(new FileOutputStream(propsFile), baseURI) ;
	
			Writer writer = new FileWriter(rq) ;
			writer.write(queryString) ;
			writer.close() ;
		} catch (Exception e) {
			throw new RepositoryException(e) ;
		}
	}
	
	/* persist (rehydrate) named queries from data-dir
	 * Return iterator over persistent named query properties
	 */
	
	public static Iterator<Properties> persist(File dataDir) {
		final File dir = new File(dataDir, NAMED_QUERIES_DATA_DIR) ;
		final File[] files = dir.listFiles(new FilenameFilter() {
			public boolean accept(File file, String filename) {
				return filename.endsWith(".properties");
			}
		}) ;
		return new Iterator<Properties>() {
			int i=0 ;
			public boolean hasNext() {
				return dir.isDirectory() && i<files.length;
			}
			public Properties next() {
				try {
					Properties props = new Properties() ;
					props.load(new FileInputStream(files[i++])) ;
					return props ;
				} catch (Exception e) {
					logger.error(e.getMessage()) ;
					return null;
				} 
			}
			public void remove() {}
		};
	}
	
	protected static String load(File file) throws IOException {
		final FileReader reader = new FileReader(file);
		char[] block = new char[4096];
		final StringBuffer buffer = new StringBuffer();
		try {
			int len;
			while ((len = reader.read(block)) >0) {
				buffer.append(block, 0, len);
			}
		} finally {
			reader.close();
		}
		return buffer.toString();
	}
	
	private static String stripSymbols(String s) {
		return s.replaceAll("[^a-zA-Z0-9]", "") ;
	}
}
