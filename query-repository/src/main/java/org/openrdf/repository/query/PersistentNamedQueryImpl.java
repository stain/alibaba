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
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.query.config.NamedQueryFactory;

/**
 * Support for named query persistence
 * 
 * @author Steve Battle
 *
 */

public class PersistentNamedQueryImpl extends NamedQueryBase {
	
	private static final String NAMED_QUERIES_DATA_DIR = "named-queries";
	
	/* Constructor used on initial creation */

	public PersistentNamedQueryImpl(QueryLanguage ql, String queryString, String baseURI) 
	throws RepositoryException {
		super(ql, queryString, baseURI);		
	}
	
	/* Constructors for persistence */
	
	public PersistentNamedQueryImpl() {
		super() ;
	}
	
	public void initialize(File dataDir, Properties props) throws Exception {
		initialize(
			Long.parseLong(props.getProperty("eTagPrefix")), 
			Long.parseLong(props.getProperty("eTagSuffix")), 
			props.getProperty("eTag"),
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
	
	public void cease(File dataDir, URI uri) {
		String name = getLocalName(uri) + Long.toString(getResultETagPrefix(), 32) ;
		new File(getDataDir(dataDir), name+".properties").delete() ;
		new File(getDataDir(dataDir), name+".rq").delete() ;
	}
	
	/* desist (dehydrate) a named query */
	
	public void desist(File dataDir, URI uri) throws RepositoryException {
		try {
			String name = getLocalName(uri) + Long.toString(getResultETagPrefix(), 32) ;
			File propsFile = new File(getDataDir(dataDir), name+".properties") ;
			File rq = new File(getDataDir(dataDir), name+".rq") ;
	
			// overwrite any existing properties file
			Properties props = new Properties() ;
			props.setProperty("uri", uri.stringValue()) ;
			props.setProperty("queryLanguage", getQueryLanguage().getName()) ;
			props.setProperty("queryString", rq.getName()) ;
			props.setProperty("baseURI", getBaseURI()) ;
			props.setProperty("lastModified", Long.toString(getResultLastModified())) ;
			props.setProperty("eTagPrefix", Long.toString(getResultETagPrefix())) ;
			props.setProperty("eTagSuffix", Long.toString(getResultETagSuffix())) ;
			props.setProperty("eTag", getResultETag()) ;
			String comment = uri.stringValue() ;
			props.store(new OutputStreamWriter(new FileOutputStream(propsFile),"UTF8"), comment) ;
			Writer writer = new OutputStreamWriter(new FileOutputStream(rq),"UTF8") ;
			writer.write(getQueryString()) ;
			writer.close() ;
		} catch (Exception e) {
			throw new RepositoryException(e) ;
		}
	}
	
	public static Map<URI,NamedQueryRepository.NamedQuery> persist(File dataDir, ValueFactory vf, NamedQueryFactory factory) 
	throws RepositoryException {
		Map<URI, NamedQueryRepository.NamedQuery> map = new HashMap<URI,NamedQueryRepository.NamedQuery>() ;
		File dir = getDataDir(dataDir) ;
		File[] files = dir.listFiles(new FilenameFilter() {
			public boolean accept(File file, String filename) {
				return filename.endsWith(".properties");
			}
		}) ;
		for (int i=0; i<files.length; i++) try {
			Properties props = new Properties() ;
			props.load(new InputStreamReader(new FileInputStream(files[i]),"UTF8")) ;
			NamedQueryRepository.NamedQuery nq = factory.createNamedQuery(dataDir, props) ;
			map.put(vf.createURI(props.getProperty("uri")), nq) ;
		}
		catch (Exception e) {
			throw new RepositoryException(e) ;
		}
		return map ;
	}
	
	private static String load(File file) throws IOException {
		final Reader reader = new InputStreamReader(new FileInputStream(file),"UTF8");
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
	
	private String getLocalName(URI uri) {
		return substituteSymbols(uri.getLocalName());
	}

	private static String substituteSymbols(String s) {
		return s.replaceAll("[^a-zA-Z0-9\\-]", "_") ;
	}
}
