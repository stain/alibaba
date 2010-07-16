/*
 * Copyright 2009-2010, James Leigh and Zepheira LLC Some rights reserved.
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
package org.openrdf.http.object.behaviours;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.GregorianCalendar;

import javax.xml.datatype.XMLGregorianCalendar;

import org.openrdf.http.object.client.RemoteConnection;
import org.openrdf.http.object.concepts.HTTPFileObject;
import org.openrdf.http.object.concepts.Transaction;
import org.openrdf.http.object.exceptions.ResponseException;
import org.openrdf.http.object.traits.ProxyObject;
import org.openrdf.repository.object.ObjectConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the FileObject interface for HTTP Objects that are local or
 * remote.
 */
public abstract class HTTPFileObjectSupport extends FileObjectImpl implements
		HTTPFileObject, ProxyObject {
	private Logger logger = LoggerFactory
			.getLogger(HTTPFileObjectSupport.class);
	private File file;
	private boolean readOnly;

	public void initLocalFileObject(File file, boolean readOnly) {
		this.file = file;
		this.readOnly = readOnly;
	}

	public long getLastModified() {
		long lastModified = super.getLastModified();
		Transaction trans = getRevision();
		if (trans != null) {
			XMLGregorianCalendar xgc = trans.getCommittedOn();
			if (xgc != null) {
				GregorianCalendar cal = xgc.toGregorianCalendar();
				long committed = cal.getTimeInMillis();
				if (lastModified < committed)
					return committed;
			}
		}
		return lastModified;
	}

	public InputStream openInputStream() throws IOException {
		if (toFile() == null) {
			String accept = "*/*";
			RemoteConnection con = openConnection("GET", null);
			con.addHeader("Accept", accept);
			int status = con.getResponseCode();
			if (status == 404) {
				con.close();
				return null;
			}
			if (status >= 400) {
				String msg = con.getResponseMessage();
				String stack = con.readErrorMessage();
				con.close();
				throw ResponseException.create(status, msg, stack);
			}
			if (status < 200 || status >= 300) {
				con.close();
				return null;
			}
			return con.readStream();
		} else {
			return super.openInputStream();
		}
	}

	public OutputStream openOutputStream() throws IOException {
		if (readOnly)
			throw new IllegalStateException(
					"Cannot modify entity within a safe methods");
		if (toFile() == null) {
			return openConnection("PUT", null).writeStream();
		} else {
			return super.openOutputStream();
		}
	}

	public boolean delete() {
		if (readOnly)
			throw new IllegalStateException(
					"Cannot modify entity within a safe methods");
		if (toFile() == null) {
			try {
				RemoteConnection con = openConnection("DELETE", null);
				try {
					int status = con.getResponseCode();
					if (status >= 400) {
						String msg = con.getResponseMessage();
						String stack = con.readErrorMessage();
						throw ResponseException.create(status, msg, stack);
					}
					return status >= 200 && status < 300;
				} finally {
					con.close();
				}
			} catch (IOException e) {
				logger.warn(e.toString(), e);
				return false;
			}
		} else {
			setRevision(null);
			return super.delete();
		}
	}

	protected File toFile() {
		return file;
	}

	private RemoteConnection openConnection(String method, String qs)
			throws IOException {
		String uri = getResource().stringValue();
		ObjectConnection oc = getObjectConnection();
		return new RemoteConnection(getProxyObjectInetAddress(), method, uri, qs, oc);
	}

}
