package org.openrdf.repository.object.managers.helpers;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import javassist.bytecode.ClassFile;

public class CheckForBehaviour extends CheckForConcept {

	public CheckForBehaviour(ClassLoader cl) {
		super(cl);
	}

	public String getClassName(String name) throws IOException {
		// NOTE package-info.class should be excluded
		if (!name.endsWith(".class") || name.contains("-"))
			return null;
		InputStream stream = cl.getResourceAsStream(name);
		assert stream != null : name;
		DataInputStream dstream = new DataInputStream(stream);
		try {
			ClassFile cf = new ClassFile(dstream);
			if (!cf.isInterface()) {
				// behaviour that implements a concept
				for (String fname : cf.getInterfaces()) {
					String cn = fname.replace('.', '/') + ".class";
					if (super.getClassName(cn) != null)
						return cf.getName();
				}
			}
		} finally {
			dstream.close();
			stream.close();
		}
		return null;
	}

}
