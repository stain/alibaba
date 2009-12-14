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
package org.openrdf.http.object.behaviours;

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
import java.io.Writer;

import javax.tools.FileObject;

import org.openrdf.http.object.traits.VersionedObject;

/**
 * Commons methods used by both http:// and file:// objects.
 */
public abstract class FileObjectImpl implements VersionedObject, FileObject {
	private static int counter;
	private File pending;
	private boolean deleted;

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

	public long getLastModified() {
		if (getLocalFile() == null)
			return -1;
		return getLocalFile().lastModified() / 1000 * 1000;
	}

	public InputStream openInputStream() throws IOException {
		if (getLocalFile() != null && getLocalFile().canRead()) {
			// TODO return delayed for use after sync
			return new FileInputStream(getLocalFile());
		} else {
			return null;
		}
	}

	public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
		InputStream in = openInputStream();
		if (in == null)
			return null;
		return new InputStreamReader(in);
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
		File file = toFile();
		if (file == null)
			return null;
		File dir = file.getParentFile();
		dir.mkdirs();
		if (!dir.canWrite() || file.exists() && !file.canWrite())
			throw new IOException("Cannot open file for writting");
		String name = ".$partof" + file.getName() + "-" + (++counter);
		final File tmp = new File(dir, name);
		final OutputStream fout = new FileOutputStream(tmp);
		return new FilterOutputStream(fout) {
			private IOException fatal;

			public void write(int b) throws IOException {
				try {
					fout.write(b);
				} catch (IOException e) {
					fatal = e;
					throw e;
				}
			}

			public void write(byte[] b, int off, int len) throws IOException {
				try {
					fout.write(b, off, len);
				} catch (IOException e) {
					fatal = e;
					throw e;
				}
			}

			public void close() throws IOException {
				fout.close();
				if (fatal == null) {
					if (pending != null) {
						pending.delete();
					}
					deleted = false;
					pending = tmp;
					touchRevision();
				} else {
					tmp.delete();
				}
			}
		};
	}

	public Writer openWriter() throws IOException {
		OutputStream out = openOutputStream();
		if (out == null)
			return null;
		return new OutputStreamWriter(out);
	}

	public boolean delete() {
		File file = toFile();
		if (file != null && file.canWrite() && file.getParentFile().canWrite()) {
			if (pending != null) {
				pending.delete();
				pending = null;
			}
			deleted = true;
			return true;
		} else {
			return false;
		}
	}

	public void commitFile() throws IOException {
		try {
			File file = toFile();
			if (pending != null) {
				if (file.exists()) {
					file.delete();
				}
				if (!pending.renameTo(file))
					throw new IOException("Could not save file");
			} else if (deleted) {
				if (file.exists() && !file.delete())
					throw new IOException("Could not delete file");
			}
		} finally {
			pending = null;
			deleted = false;
		}
	}

	public void rollbackFile() {
		deleted = false;
		if (pending != null) {
			pending.delete();
			pending = null;
		}
	}

	protected abstract File toFile();

	private File getLocalFile() {
		if (pending != null)
			return pending;
		return toFile();
	}

}
