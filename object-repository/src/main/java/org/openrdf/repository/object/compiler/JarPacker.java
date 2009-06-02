/*
 * Copyright (c) 2009, James Leigh All rights reserved.
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
package org.openrdf.repository.object.compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.openrdf.rio.RDFParseException;

public class JarPacker {

	private static final String META_INF_ANNOTATIONS = "META-INF/org.openrdf.annotations";
	private static final String META_INF_BEHAVIOURS = "META-INF/org.openrdf.behaviours";
	private static final String META_INF_CONCEPTS = "META-INF/org.openrdf.concepts";
	private static final String META_INF_DATATYPES = "META-INF/org.openrdf.datatypes";
	private static final String META_INF_ONTOLOGIES = "META-INF/org.openrdf.ontologies";

	private File dir;
	private Collection<String> annotations;
	private Collection<String> behaviours;
	private Collection<String> concepts;
	private Collection<String> datatypes;
	private Collection<URL> ontologies;

	public JarPacker(File dir) {
		this.dir = dir;
	}

	public void setAnnotations(Set<String> annotations) {
		this.annotations = annotations;
	}

	public void setBehaviours(Collection<String> behaviours) {
		this.behaviours = behaviours;
	}

	public void setConcepts(Collection<String> concepts) {
		this.concepts = concepts;
	}

	public void setDatatypes(Collection<String> datatypes) {
		this.datatypes = datatypes;
	}

	public void setOntologies(Collection<URL> ontologies) {
		this.ontologies = ontologies;
	}

	public void packageJar(File output) throws Exception {
		FileOutputStream stream = new FileOutputStream(output);
		JarOutputStream jar = new JarOutputStream(stream);
		try {
			packaFiles(dir, dir, jar);
			if (annotations != null) {
				printClasses(annotations, jar, META_INF_ANNOTATIONS);
			}
			if (behaviours != null) {
				printClasses(behaviours, jar, META_INF_BEHAVIOURS);
			}
			if (concepts != null) {
				printClasses(concepts, jar, META_INF_CONCEPTS);
			}
			if (datatypes != null) {
				printClasses(datatypes, jar, META_INF_DATATYPES);
			}
			if (ontologies != null) {
				packOntologies(ontologies, jar);
			}
		} finally {
			jar.close();
			stream.close();
		}
	}

	private void packaFiles(File base, File dir, JarOutputStream jar)
			throws IOException, FileNotFoundException {
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				packaFiles(base, file, jar);
			} else if (file.exists()) {
				String path = file.getAbsolutePath();
				path = path.substring(base.getAbsolutePath().length() + 1);
				// replace separatorChar by '/' on all platforms
				if (File.separatorChar != '/') {
					path = path.replace(File.separatorChar, '/');
				}
				jar.putNextEntry(new JarEntry(path));
				copyInto(file.toURI().toURL(), jar);
				file.delete();
			}
		}
	}

	private void copyInto(URL source, OutputStream out)
			throws FileNotFoundException, IOException {
		InputStream in = source.openStream();
		try {
			int read;
			byte[] buf = new byte[512];
			while ((read = in.read(buf)) > 0) {
				out.write(buf, 0, read);
			}
		} finally {
			in.close();
		}
	}

	private void printClasses(Collection<String> roles, JarOutputStream jar,
			String entry) throws IOException {
		PrintStream out = null;
		for (String name : roles) {
			if (out == null) {
				jar.putNextEntry(new JarEntry(entry));
				out = new PrintStream(jar);
			}
			out.println(name);
		}
		if (out != null) {
			out.flush();
		}
	}

	private void packOntologies(Collection<URL> rdfSources, JarOutputStream jar)
			throws RDFParseException, IOException {
		Map<String, URL> ontologies = new HashMap<String, URL>();
		for (URL rdf : rdfSources) {
			String path = "META-INF/ontologies/";
			path += asLocalFile(rdf).getName();
			if (rdf.toExternalForm().startsWith("file:")) {
				ontologies.put(path, null);
			} else {
				ontologies.put(path, rdf);
			}
			jar.putNextEntry(new JarEntry(path));
			copyInto(rdf, jar);
		}
		if (ontologies.isEmpty())
			return;
		jar.putNextEntry(new JarEntry(META_INF_ONTOLOGIES));
		PrintStream out = new PrintStream(jar);
		for (Map.Entry<String, URL> e : ontologies.entrySet()) {
			out.print(e.getKey());
			if (e.getValue() != null) {
				out.print("\t=\t");
				out.print(e.getValue().toExternalForm());
			}
			out.println();
		}
		out.flush();
	}

	private File asLocalFile(URL rdf) throws UnsupportedEncodingException {
		return new File(URLDecoder.decode(rdf.getFile(), "UTF-8"));
	}
}
