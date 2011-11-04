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
package org.openrdf.store.blob.disk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import org.openrdf.store.blob.BlobObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlobFile extends BlobObject implements BlobListener {
	private final Logger logger = LoggerFactory.getLogger(BlobFile.class);
	private final DiskTransaction disk;
	private File readFile;
	private File writeFile;
	private boolean open;
	private boolean initialized;
	private volatile boolean changed;
	private boolean deleted;
	private boolean written;

	protected BlobFile(DiskTransaction disk, URI uri) {
		super(uri);
		this.disk = disk;
	}

	public synchronized String[] getHistory() throws IOException {
		init(false);
		List<String> history = new ArrayList<String>();
		File dir = getLocalDir(disk.getDirectory(), toUri());
		Map<String, String> map = readIndexFile(dir);
		for (String id : map.keySet()) {
			if (id.equals(disk.getID())) {
				break;
			}
			history.add(id);
		}
		Collection<String> iris = disk.getIriFromHexIDs(history).values();
		if (disk.isClosed()) {
			iris.retainAll(Arrays.asList(disk.getHistory()));
		}
		return iris.toArray(new String[iris.size()]);
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
		if (readFile == null)
			return null;
		Lock read = disk.readLock();
		try {
			read.lock();
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
				File dir = getLocalDir(disk.getDirectory(), toUri());
				Map<String, String> map = readIndexFile(dir);
				map.put(disk.getID(), "");
				writeIndexFile(dir, map);
				return true;
			} else if (written) {
				File dir = getLocalDir(disk.getDirectory(), toUri());
				Map<String, String> map = readIndexFile(dir);
				map.put(disk.getID(), writeFile.getName());
				writeIndexFile(dir, map);
				return true;
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
		Map<String, String> map = readIndexFile(dir);
		String id = disk.getID();
		String name = map.get(id);
		if (name != null) {
			map.remove(id);
			writeIndexFile(dir, map);
			boolean ret = delete(dir, name);
			while (dir.list().length == 0 && dir.delete()) {
				dir = dir.getParentFile();
			}
			return ret;
		}
		return false;
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
		Map<String, String> map = readIndexFile(dir);
		Map<String, String> history = null;
		if (disk.isClosed()) {
			history = disk.getIriFromHexIDs(map.keySet());
			history.values().retainAll(Arrays.asList(disk.getHistory()));
		}
		readFile = null;
		String id = disk.getID();
		for (Map.Entry<String, String> e : map.entrySet()) {
			if (e.getValue().length() == 0) {
				readFile = null;
			} else {
				readFile = new File(dir, e.getValue());
			}
			if (e.getKey().equals(id)) {
				writeFile = readFile;
				break;
			} else if (history != null && history.containsKey(e.getKey())) {
				writeFile = readFile;
			} else if (history != null) {
				break;
			}
		}
		if (writeFile == null) {
			String name = getLocalName(dir, "", toUri(), id);
			writeFile = new File(dir, name);
		}
	}

	private Map<String, String> readIndexFile(File dir) throws IOException {
		Lock read = disk.readLock();
		try {
			read.lock();
			File index = new File(dir, getLocalName(dir, "index", toUri(), ""));
			BufferedReader reader = new BufferedReader(new FileReader(index));
			try {
				String line;
				Map<String, String> map = new LinkedHashMap<String, String>();
				while ((line = reader.readLine()) != null) {
					String[] split = line.split("\\s+", 2);
					map.put(split[0], split[1]);
				}
				return map;
			} finally {
				reader.close();
			}
		} catch (FileNotFoundException e) {
			return new LinkedHashMap<String, String>();
		} finally {
			read.unlock();
		}
	}

	private void writeIndexFile(File dir, Map<String, String> index)
			throws IOException {
		int skip = index.size() - disk.getMaxHistoryLength();
		File f = new File(dir, getLocalName(dir, "index", toUri(), ""));
		if (index.isEmpty() && f.delete()) {
			return;
		}
		PrintWriter writer = new PrintWriter(new FileWriter(f));
		try {
			for (Map.Entry<String, String> e : index.entrySet()) {
				if (skip <= 0) {
					writer.print(e.getKey());
					writer.print(' ');
					writer.println(e.getValue());
				} else {
					skip--;
					delete(dir, e.getValue());
				}
			}
		} finally {
			writer.close();
		}
	}

	private boolean delete(File dir, String name) {
		if (name.length() > 0) {
			return new File(dir, name).delete();
		}
		return true;
	}

	private File getLocalDir(File dir, URI uri) {
		String auth = uri.getAuthority();
		if (auth == null)
			return new File(dir, safe(uri.getSchemeSpecificPart()));
		File base = new File(dir, safe(auth));
		String path = uri.getPath();
		return new File(base, safe(path));
	}

	private String safe(String path) {
		if (path == null)
			return "";
		path = path.replace('/', File.separatorChar);
		path = path.replace('\\', File.separatorChar);
		path = path.replace(':', File.separatorChar);
		path = path.replaceAll("[^a-zA-Z0-9\\-./\\\\]", "_");
		return path.toLowerCase();
	}

	private String getLocalName(File dir, String prefix, URI uri, String suffix) {
		String name = Integer.toHexString(Math.abs(uri.toString().hashCode()));
		int dot = dir.getName().lastIndexOf('.');
		if (dot > 0 && prefix.length() == 0) {
			name = '$' + name + '$' + suffix + dir.getName().substring(dot);
		} else {
			name = prefix + '$' + name + '$' + suffix;
		}
		return name;
	}

}
