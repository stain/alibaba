package org.openrdf.server.metadata.resources;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import org.openrdf.repository.RepositoryException;
import org.openrdf.server.metadata.concepts.WebResource;
import org.openrdf.server.metadata.http.Request;
import org.openrdf.server.metadata.http.Response;

public class OptionsResource extends MetadataResource {

	public OptionsResource(File file, WebResource target) {
		super(file, target);
	}

	public Response options(Request req) throws RepositoryException {
		String name = req.getOperation();
		StringBuilder sb = new StringBuilder();
		sb.append("OPTIONS, TRACE");
		if (name == null) {
			for (String method : getAllowedDataMethods()) {
				sb.append(", ").append(method);
			}
		} else {
			for (String method : getAllowedMetadataMethods(name)) {
				sb.append(", ").append(method);
			}
		}
		return new Response().header("Allow", sb.toString());
	}

	public Set<String> getAllowedDataMethods() throws RepositoryException {
		Set<String> set = new LinkedHashSet<String>();
		File file = getFile();
		if (file.canRead()) {
			set.add("GET");
			set.add("HEAD");
		}
		File parent = file.getParentFile();
		if (file.canWrite() || !file.exists()
				&& (!parent.exists() || parent.canWrite())) {
			set.add("PUT");
		}
		if (file.exists() && parent.canWrite()) {
			set.add("DELETE");
		}
		Set<String> allowed = getAllowedMetadataMethods("POST");
		if (allowed.contains("POST")) {
			set.add("POST");
		}
		return set;
	}

	public Set<String> getAllowedMetadataMethods(String name)
			throws RepositoryException {
		Set<String> set = new LinkedHashSet<String>();
		if (findGetterMethod(name) != null) {
			set.add("GET");
			set.add("HEAD");
		}
		if (findOperationMethod(name) != null) {
			set.add("POST");
		}
		if (findSetterMethod(name) != null) {
			set.add("PUT");
			set.add("DELETE");
		}
		return set;
	}

}
