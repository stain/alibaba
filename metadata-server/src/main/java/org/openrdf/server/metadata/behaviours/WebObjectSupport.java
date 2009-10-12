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
package org.openrdf.server.metadata.behaviours;

import static java.lang.Integer.toHexString;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.activation.MimeTypeParseException;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.codec.binary.Base64;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.annotations.parameterTypes;
import org.openrdf.repository.object.concepts.Message;
import org.openrdf.repository.object.exceptions.BehaviourException;
import org.openrdf.server.metadata.concepts.InternalWebObject;
import org.openrdf.server.metadata.concepts.Transaction;
import org.openrdf.server.metadata.concepts.WebContentListener;
import org.openrdf.server.metadata.concepts.WebRedirect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.medsea.mimeutil.MimeType;
import eu.medsea.mimeutil.MimeUtil;

public abstract class WebObjectSupport implements InternalWebObject {
	private static Logger logger = LoggerFactory
			.getLogger(WebObjectSupport.class);
	private File file;
	private boolean readOnly;
	private Object designated;

	public void initFileObject(File file, boolean readOnly) {
		this.file = file;
		this.readOnly = readOnly;
	}

	public java.net.URI toUri() {
		return java.net.URI.create(getResource().stringValue());
	}

	public String getName() {
		String uri = getResource().stringValue();
		int last = uri.length() - 1;
		int idx = uri.lastIndexOf('/', last - 1) + 1;
		if (idx > 0 && uri.charAt(last) != '/')
			return uri.substring(idx);
		if (idx > 0 && idx != last)
			return uri.substring(idx, last);
		return uri;
	}

	public String revisionTag() {
		Transaction trans = getRevision();
		if (trans == null)
			return null;
		String uri = trans.getResource().stringValue();
		String revision = toHexString(uri.hashCode());
		return "W/" + '"' + revision + '"';
	}

	public String identityTag() {
		Transaction trans = getRevision();
		String mediaType = getMediaType();
		if (trans == null || mediaType == null)
			return null;
		String uri = trans.getResource().stringValue();
		String revision = toHexString(uri.hashCode());
		String type = toHexString(mediaType.hashCode());
		String schema = toHexString(getObjectConnection().getSchemaRevision());
		return '"' + revision + '-' + type + '-' + schema + '"';
	}

	public String variantTag(String mediaType) {
		if (mediaType == null)
			return revisionTag();
		String variant = toHexString(mediaType.hashCode());
		String schema = toHexString(getObjectConnection().getSchemaRevision());
		Transaction trans = getRevision();
		if (trans == null)
			return "W/" + '"' + '-' + variant + '-' + schema + '"';
		String uri = trans.getResource().stringValue();
		String revision = toHexString(uri.hashCode());
		return "W/" + '"' + revision + '-' + variant + '-' + schema + '"';
	}

	public long getLastModified() {
		long lastModified = 0;
		if (file != null) {
			lastModified = file.lastModified() / 1000 * 1000;
		}
		Transaction trans = getRevision();
		if (trans != null) {
			XMLGregorianCalendar xgc = trans.getCommittedOn();
			if (xgc != null) {
				GregorianCalendar cal = xgc.toGregorianCalendar();
				cal.set(Calendar.MILLISECOND, 0);
				long committed = cal.getTimeInMillis();
				if (lastModified < committed)
					return committed;
			}
		}
		return lastModified;
	}

	public Charset charset() {
		String mediaType = getMediaType();
		if (mediaType == null)
			return null;
		try {
			javax.activation.MimeType m;
			m = new javax.activation.MimeType(mediaType);
			String name = m.getParameters().get("charset");
			if (name == null)
				return null;
			return Charset.forName(name);
		} catch (MimeTypeParseException e) {
			logger.info(e.toString(), e);
			return null;
		} catch (IllegalCharsetNameException e) {
			logger.info(e.toString(), e);
			return null;
		} catch (UnsupportedCharsetException e) {
			logger.info(e.toString(), e);
			return null;
		} catch (IllegalArgumentException e) {
			logger.info(e.toString(), e);
			return null;
		}
	}

	public String getAndSetMediaType() {
		String mediaType = getMediaType();
		if (mediaType == null && file != null) {
			mediaType = getMimeType(file);
			setMediaType(mediaType);
		}
		return mediaType;
	}

	@parameterTypes(String.class)
	public void setMediaType(Message msg) {
		String mediaType = (String) msg.getParameters()[0];
		String previous = mimeType(getMediaType());
		msg.proceed();
		ObjectConnection con = getObjectConnection();
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
				designated = con.addDesignations(this, uri);
				((InternalWebObject) designated).initFileObject(file, readOnly);
			}
		} catch (RepositoryException e) {
			throw new BehaviourException(e);
		}
	}

	public InputStream openInputStream() throws IOException {
		String encoding;
		InputStream in;
		if (file == null) {
			URL url = toUri().toURL();
			String mediaType = getMediaType();
			HttpURLConnection con = open(url);
			con.setInstanceFollowRedirects(false);
			con.addRequestProperty("Accept-Encoding", "gzip");
			if (mediaType == null) {
				con.addRequestProperty("Accept", "*/*");
			} else {
				con.addRequestProperty("Accept", mediaType + ", */*;q=0.1");
			}
			con.connect();
			int status = con.getResponseCode();
			if (status >= 400)
				throw new IOException(read(con));
			if (status < 200 || status >= 300)
				return null;
			encoding = con.getHeaderField("Content-Encoding");
			in = con.getInputStream();
			if ("gzip".equals(encoding))
				return new GZIPInputStream(in);
			return in;
		} else if (file.canRead()) {
			encoding = getContentEncoding();
			in = new FileInputStream(file);
		} else {
			return null;
		}
		if ("gzip".equals(encoding))
			return new GZIPInputStream(in);
		return in;
	}

	public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
		Charset charset = charset();
		InputStream in = openInputStream();
		if (in == null)
			return null;
		if (charset == null)
			return new InputStreamReader(in);
		return new InputStreamReader(in, charset);
	}

	public CharSequence getCharContent(boolean ignoreEncodingErrors)
			throws IOException {
		Reader reader = openReader(ignoreEncodingErrors);
		if (reader == null)
			return null;
		try {
			StringWriter writer = new StringWriter();
			int read;
			char[] cbuf = new char[1024];
			while ((read = reader.read(cbuf)) >= 0) {
				writer.write(cbuf, 0, read);
			}
			return writer.toString();
		} finally {
			reader.close();
		}
	}

	public OutputStream openOutputStream() throws IOException {
		if (readOnly)
			throw new IllegalStateException(
					"Cannot modify entity within a safe methods");
		final String mediaType = getMediaType();
		final String encoding = getDesiredEncoding(mediaType);
		OutputStream out;
		if (file == null) {
			URL url = toUri().toURL();
			final HttpURLConnection con = open(url);
			con.setDoOutput(true);
			con.setInstanceFollowRedirects(false);
			con.setRequestMethod("PUT");
			if (mediaType != null) {
				con.addRequestProperty("Content-Type", mediaType);
			}
			if ("gzip".equals(encoding)) {
				con.addRequestProperty("Content-Encoding", "gzip");
			}
			final OutputStream hout = con.getOutputStream();
			out = new FilterOutputStream(hout) {
				public void write(byte[] b, int off, int len)
						throws IOException {
					hout.write(b, off, len);
				}

				public void close() throws IOException {
					super.close();
					if (con.getResponseCode() >= 400) {
						throw new IOException(read(con));
					}
				}
			};
		} else {
			File dir = file.getParentFile();
			dir.mkdirs();
			final File tmp = new File(dir, "$partof" + file.getName());
			final OutputStream fout = new FileOutputStream(tmp);
			final MessageDigest md5 = getMessageDigest("MD5");
			out = new FilterOutputStream(fout) {
				private IOException fatal;

				public void write(int b) throws IOException {
					try {
						fout.write(b);
						md5.update((byte) b);
					} catch (IOException e) {
						fatal = e;
					}
				}

				public void write(byte[] b, int off, int len)
						throws IOException {
					try {
						fout.write(b, off, len);
						md5.update(b, off, len);
					} catch (IOException e) {
						fatal = e;
					}
				}

				public void close() throws IOException {
					fout.close();
					if (fatal != null)
						throw fatal;
					if (file.exists()) {
						file.delete();
					}
					if (!tmp.renameTo(file))
						throw new IOException("Could not save file");
					byte[] b = Base64.encodeBase64(md5.digest());
					String contentMD5 = new String(b, "UTF-8");
					ObjectConnection con = getObjectConnection();
					try {
						con.clear(getResource());
						setRedirect(null);
						con.removeDesignation(WebObjectSupport.this,
								WebRedirect.class);
						setContentMD5(contentMD5);
						setContentEncoding(encoding);
						if (mediaType == null) {
							setMediaType(getMimeType(file));
						}
						URI[] before = con.getAddContexts();
						try {
							con.setAddContexts((URI) getResource());
							if (designated instanceof WebContentListener) {
								((WebContentListener) designated)
										.contentChanged();
							} else if (designated == null) {
								contentChanged();
							}
						} finally {
							con.setAddContexts(before);
						}
					} catch (RepositoryException e) {
						throw new IOException(e);
					}
				}
			};
		}
		if ("gzip".equals(encoding))
			return new GZIPOutputStream(out);
		return out;
	}

	public Writer openWriter() throws IOException {
		Charset charset = charset();
		if (charset == null)
			return new OutputStreamWriter(openOutputStream());
		return new OutputStreamWriter(openOutputStream(), charset);
	}

	public boolean delete() {
		if (readOnly)
			throw new IllegalStateException(
					"Cannot modify entity within a safe methods");
		if (file == null) {
			try {
				URL url = toUri().toURL();
				return delete(url);
			} catch (IOException e) {
				logger.warn(e.toString(), e);
				return false;
			}
		} else {
			ObjectConnection con = getObjectConnection();
			try {
				con.clear(getResource());
				con.removeDesignation(this, WebRedirect.class);
				setRedirect(null);
				setRevision(null);
				setMediaType((String) null);
				setContentMD5(null);
				setContentEncoding(null);
				con.setAutoCommit(true); // prepare()
			} catch (RepositoryException e) {
				logger.error(e.toString(), e);
				return false;
			}
			return file.delete();
		}
	}

	private HttpURLConnection open(URL url) throws IOException {
		return (HttpURLConnection) url.openConnection();
	}

	private MessageDigest getMessageDigest(String algorithm) {
		try {
			return MessageDigest.getInstance(algorithm);
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
	}

	private boolean delete(URL url) throws IOException, ProtocolException,
			UnsupportedEncodingException {
		HttpURLConnection con = open(url);
		con.setInstanceFollowRedirects(false);
		con.setRequestMethod("DELETE");
		con.connect();
		int status = con.getResponseCode();
		if (status >= 400) {
			logger.warn(con.getResponseMessage(), read(con));
			return false;
		}
		return status >= 200 && status < 300;
	}

	private String read(URLConnection con) {
		try {
			InputStream in = con.getInputStream();
			try {
				StringWriter string = new StringWriter();
				InputStreamReader reader = new InputStreamReader(in, "UTF-8");
				int read;
				char[] cbuf = new char[1024];
				while ((read = reader.read(cbuf)) >= 0) {
					string.write(cbuf, 0, read);
				}
				return string.toString();
			} finally {
				in.close();
			}
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		} catch (IOException e) {
			logger.info(e.toString(), e);
			return null;
		}
	}

	private String getDesiredEncoding(String type) {
		if (type == null)
			return "identity";
		if (type.startsWith("text/") || type.startsWith("application/xml")
				|| type.startsWith("application/x-turtle")
				|| type.startsWith("application/trix")
				|| type.startsWith("application/x-trig")
				|| type.startsWith("application/postscript")
				|| type.startsWith("application/")
				&& (type.endsWith("+xml") || type.contains("+xml;")))
			return "gzip";
		return "identity";
	}

	private String mimeType(String media) {
		if (media == null)
			return null;
		int idx = media.indexOf(';');
		if (idx > 0)
			return media.substring(0, idx);
		return media;
	}

	private String getMimeType(File file) {
		Collection types = MimeUtil.getMimeTypes(file);
		MimeType mimeType = null;
		double specificity = 0;
		for (Iterator it = types.iterator(); it.hasNext();) {
			MimeType mt = (MimeType) it.next();
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

}
