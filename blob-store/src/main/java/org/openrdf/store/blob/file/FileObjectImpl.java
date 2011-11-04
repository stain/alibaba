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
package org.openrdf.store.blob.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.concurrent.locks.Lock;

import org.openrdf.store.blob.BlobObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class FileObjectImpl extends BlobObject implements FileListener {
	private final Logger logger = LoggerFactory.getLogger(FileObjectImpl.class);
	private final FileTransaction disk;
	private File readFile;
	private File writeFile;
	private boolean open;
	private boolean initialized;
	private volatile boolean changed;
	private boolean deleted;
	private boolean written;

	protected FileObjectImpl(FileTransaction disk, URI uri) {
		super(uri);
		this.disk = disk;
	}

	public synchronized String[] getHistory() throws IOException {
		return disk.getHistory();
	}

	public synchronized boolean delete() {
		try {
			init(true);
		} catch (IOException e) {
			logger.error(e.toString(), e);
			return false;
		}
		Lock read = disk.readLock();
		try {
			read.lock();
			deleted = readFile != null && readFile.exists()
					&& readFile.getParentFile().canWrite();
			return deleted;
		} finally {
			read.unlock();
		}
	}

	public synchronized long getLastModified() {
		try {
			init(true);
		} catch (IOException e) {
			logger.error(e.toString(), e);
			return 0;
		}
		if (deleted)
			return 0;
		if (written)
			return writeFile.lastModified();
		if (readFile == null)
			return 0;
		Lock read = disk.readLock();
		try {
			read.lock();
			return readFile.lastModified();
		} finally {
			read.unlock();
		}
	}

	public synchronized InputStream openInputStream() throws IOException {
		init(false);
		if (deleted)
			return null;
		if (written)
			return new FileInputStream(writeFile);
		Lock read = disk.readLock();
		try {
			read.lock();
			if (readFile == null || !readFile.exists())
				return null;
			return new FileInputStream(readFile);
		} finally {
			read.unlock();
		}
	}

	public synchronized OutputStream openOutputStream() throws IOException {
		init(true);
		File dir = writeFile.getParentFile();
		dir.mkdirs();
		if (!dir.canWrite() || writeFile.exists() && !writeFile.canWrite())
			throw new IOException("Cannot open blob file for writting");
		final OutputStream fout = new FileOutputStream(writeFile);
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
				written(fatal == null);
			}
		};
	}

	public void changed(URI uri) {
		changed = true;
	}

	protected synchronized boolean hasConflict() {
		return changed;
	}

	protected synchronized boolean sync() throws IOException {
		if (!open)
			return false;
		try {
			if (deleted) {
				writeFile.delete();
				return readFile.delete();
			} else if (written) {
				File dir = getLocalDir(disk.getDirectory(), toUri());
				File target = new File(dir, getLocalName(dir, toUri(), ""));
				if (target.exists()) {
					target.delete();
				}
				writeFile.renameTo(target);
				readFile = target;
				writeFile = target;
			}
			return false;
		} finally {
			if (open) {
				disk.unwatch(toUri(), this);
				open = false;
				changed = false;
				written = false;
				deleted = false;
			}
		}
	}

	protected synchronized void abort() {
		if (open) {
			disk.unwatch(toUri(), this);
			open = false;
			changed = false;
			written = false;
			deleted = false;
			writeFile.delete();
		}
	}

	protected synchronized boolean erase() throws IOException {
		File dir = getLocalDir(disk.getDirectory(), toUri());
		writeFile.delete();
		boolean ret = readFile.delete();
		while (dir.list().length == 0 && dir.delete()) {
			dir = dir.getParentFile();
		}
		return ret;
	}

	private synchronized void written(boolean success) {
		if (success) {
			written = true;
			deleted = false;
			readFile = writeFile;
		} else {
			written = false;
			writeFile.delete();
		}
	}

	private void init(boolean write) throws IOException {
		if (disk.isClosed()) {
			if (write)
				throw new IllegalStateException(
						"Transaction has already completed");
		} else {
			if (!open) {
				open = true;
				disk.watch(toUri(), this);
			}
		}
		if (!initialized) {
			initialized = true;
			initReadWriteFile();
		}
	}

	private void initReadWriteFile() throws IOException {
		File dir = getLocalDir(disk.getDirectory(), toUri());
		readFile = new File(dir, getLocalName(dir, toUri(), ""));
		writeFile = new File(dir, getLocalName(dir, toUri(), disk.getID()));
	}

	private File getLocalDir(File dataDir, URI uri) {
		String auth = uri.getAuthority();
		if (auth == null)
			return new File(dataDir, safe(uri.getSchemeSpecificPart()));
		File base = new File(dataDir, safe(auth));
		String path = uri.getPath();
		return new File(base, safe(path));
	}

	private String safe(String path) {
		if (path == null)
			return "";
		path = path.replace('/', File.separatorChar);
		path = path.replace('\\', File.separatorChar);
		path = path.replace(':', File.separatorChar);
		path = path.replaceAll("[^a-zA-Z0-9/\\\\]", "_");
		return path.toLowerCase();
	}

	private String getLocalName(File dir, URI uri, String suffix) {
		String name = Integer.toHexString(uri.toString().hashCode());
		if (suffix.length() > 0)
			return '$' + name + '$' + suffix;
		return '$' + name;
	}

}
