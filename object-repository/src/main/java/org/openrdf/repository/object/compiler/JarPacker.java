package org.openrdf.repository.object.compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.Collection;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class JarPacker {

	private static final String META_INF_BEHAVIOURS = "META-INF/org.openrdf.behaviours";
	private static final String META_INF_CONCEPTS = "META-INF/org.openrdf.concepts";
	private static final String META_INF_DATATYPES = "META-INF/org.openrdf.datatypes";

	private File dir;
	private Collection<String> behaviours;
	private Collection<String> concepts;
	private Collection<String> datatypes;

	public JarPacker(File dir) {
		this.dir = dir;
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

	public void packageJar(File output) throws Exception {
		FileOutputStream stream = new FileOutputStream(output);
		JarOutputStream jar = new JarOutputStream(stream);
		try {
			packaFiles(dir, dir, jar);
			if (behaviours != null) {
				printClasses(behaviours, jar, META_INF_BEHAVIOURS);
			}
			if (concepts != null) {
				printClasses(concepts, jar, META_INF_CONCEPTS);
			}
			if (datatypes != null) {
				printClasses(datatypes, jar, META_INF_DATATYPES);
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
}
