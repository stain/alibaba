package org.openrdf.server.metadata.behaviours;

import info.aduna.net.ParsedURI;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.exceptions.BehaviourException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.server.metadata.WebObject;
import org.openrdf.server.metadata.annotations.header;
import org.openrdf.server.metadata.annotations.method;
import org.openrdf.server.metadata.annotations.operation;
import org.openrdf.server.metadata.concepts.Alias;
import org.openrdf.server.metadata.exceptions.BadRequest;
import org.openrdf.server.metadata.exceptions.MethodNotAllowed;

import eu.medsea.mimeutil.MimeType;
import eu.medsea.mimeutil.MimeUtil;

public abstract class PUTSupport implements WebObject {
	private static List<String> RDF_TYPES = Arrays.asList(new String[] {
			"application/rdf+xml", "application/x-turtle", "text/rdf+n3",
			"application/trix", "application/x-trig" });

	@operation({})
	@method("DELETE")
	public void deleteObject() throws RepositoryException {
		getObjectConnection().clear(getResource());
		setInternalMediaType(null);
		if (!delete())
			throw new MethodNotAllowed();
	}

	@operation({})
	@method("PUT")
	public void putIntputStream(@header("Content-Location") String location,
			@header("Content-Type") String mediaType, InputStream in)
			throws RepositoryException {
		ObjectConnection con = getObjectConnection();
		if (location == null) {
			try {
				OutputStream out = openOutputStream();
				try {
					int read;
					byte[] buf = new byte[1024];
					while ((read = in.read(buf)) >= 0) {
						out.write(buf, 0, read);
					}
				} finally {
					out.close();
					in.close();
				}
				if (mediaType == null) {
					setInternalMediaType(getMimeType());
				} else {
					setInternalMediaType(mediaType);
					if (RDF_TYPES.contains(mimeType(mediaType))) {
						importRDF();
					}
				}
			} catch (IOException e) {
				throw new BadRequest(e);
			}
		} else {
			Alias alias = con.addDesignation(this, Alias.class);
			ParsedURI base = new ParsedURI(getResource().stringValue());
			ParsedURI to = base.resolve(location);
			alias.setRedirectsTo((WebObject) con.getObject(to.toString()));
		}
	}

	private void importRDF() {
		ObjectConnection con = getObjectConnection();
		String mime = mimeType(getMediaType());
		RDFFormat format = RDFFormat.forMIMEType(mime);
		String iri = getResource().stringValue();
		try {
			InputStream in = openInputStream();
			try {
				con.add(in, iri, format, getResource());
			} finally {
				in.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String getMimeType() throws IOException {
		List<MimeType> types = getMimeTypes();
		MimeType mimeType = null;
		double specificity = 0;
		for (MimeType mt : types) {
			int spec = mt.getSpecificity() * 2;
			if (!mt.getSubType().startsWith("x-")) {
				spec += 1;
			}
			if (spec > specificity) {
				mimeType = mt;
				specificity = spec;
			}
		}
		if (mimeType == null)
			return "application/octet-stream";
		return mimeType.toString();
	}

	private List<MimeType> getMimeTypes() throws IOException {
		InputStream in = new BufferedInputStream(openInputStream());
		try {
			List<MimeType> types = new ArrayList<MimeType>();
			types.addAll(MimeUtil.getMimeTypes(in));
			types.addAll(MimeUtil.getMimeTypes(getResource().stringValue()));
			return types;
		} finally {
			in.close();
		}
	}

	private void setInternalMediaType(String mediaType) {
		ObjectConnection con = getObjectConnection();
		String previous = mimeType(getMediaType());
		setMediaType(mediaType);
		ValueFactory vf = con.getValueFactory();
		try {
			if (previous != null && !previous.equals(mediaType)) {
				try {
					URI uri = vf.createURI("urn:mimetype:" + previous);
					con.removeDesignations(this, uri);
				} catch (IllegalArgumentException e) {
					// invalid mimetype
				}
			}
			if (mediaType != null) {
				URI uri = vf.createURI("urn:mimetype:" + mimeType(mediaType));
				con.addDesignations(this, uri);
			}
		} catch (RepositoryException e) {
			throw new BehaviourException(e);
		}
	}

	private String mimeType(String media) {
		if (media == null)
			return null;
		int idx = media.indexOf(';');
		if (idx > 0)
			return media.substring(0, idx);
		return media;
	}
}
