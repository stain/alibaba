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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import org.openrdf.store.blob.BlobObject;
import org.openrdf.store.blob.BlobVersion;

public class DiskBlobVersion implements BlobVersion {
	private final DiskBlobStore store;
	private final String version;
	private final File journal;
	private File entry;
	private final Map<String, DiskBlob> open;
	private boolean prepared;
	private boolean closed;

	protected DiskBlobVersion(DiskBlobStore store, final String version,
			File file) throws IOException {
		assert store != null;
		assert version != null;
		assert file != null;
		this.store = store;
		this.version = version;
		if (file.isFile()) {
			this.closed = true;
			this.journal = file.getParentFile();
			this.open = readChanges(entry = file);
		} else {
			this.journal = file;
			this.open = new HashMap<String, DiskBlob>();
		}
	}

	public String toString() {
		return version;
	}

	public int hashCode() {
		return version.hashCode();
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DiskBlobVersion other = (DiskBlobVersion) obj;
		if (!version.equals(other.version))
			return false;
		if (!store.equals(other.store))
			return false;
		return true;
	}

	public synchronized String[] getModifications() throws IOException {
		if (closed)
			return open.keySet().toArray(new String[open.size()]);
		List<String> list = new ArrayList<String>(open.size());
		for (Map.Entry<String, DiskBlob> e : open.entrySet()) {
			if (e.getValue().isChangePending())
				list.add(e.getKey());
		}
		return list.toArray(new String[list.size()]);
	}

	public synchronized BlobObject open(String uri) {
		DiskBlob blob = open.get(uri);
		if (blob != null)
			return blob;
		open.put(uri, blob = new DiskBlob(this, uri));
		return blob;
	}

	public synchronized void prepare() throws IOException {
		if (prepared)
			throw new IllegalStateException("This version is already prepared");
		if (closed)
			throw new IllegalStateException(
					"This version has already completed");
		closed = true;
		store.lock();
		prepared = true;
		boolean faild = true;
		try {
			for (DiskBlob blob : open.values()) {
				if (blob.hasConflict())
					throw new IOException("Resource has since been modified: "
							+ blob.toUri());
			}
			faild = false;
		} finally {
			if (faild) {
				prepared = false;
				store.unlock();
			}
		}
	}

	public synchronized void commit() throws IOException {
		if (!prepared) {
			prepare();
		}
		Set<String> set = new HashSet<String>(open.size());
		for (Map.Entry<String, DiskBlob> e : open.entrySet()) {
			if (e.getValue().sync()) {
				set.add(e.getKey());
			}
		}
		open.keySet().retainAll(set);
		if (!set.isEmpty()) {
			File file = writeChanges(this.getVersion(), set);
			store.changed(this.getVersion(), set, file);
		}
		prepared = false;
		store.unlock();
	}

	public synchronized void rollback() {
		try {
			for (DiskBlob blob : open.values()) {
				blob.abort();
			}
		} finally {
			if (prepared) {
				prepared = false;
				store.unlock();
			}
		}
	}

	public synchronized boolean erase() throws IOException {
		if (!closed)
			throw new IllegalStateException("This version is not complete");
		assert entry != null;
		store.lock();
		try {
			for (DiskBlob blob : open.values()) {
				blob.erase();
			}
			boolean ret = entry.delete();
			File d = entry.getParentFile();
			if (d.list().length == 0) {
				d.delete();
			}
			if (d.getParentFile().list().length == 0) {
				d.getParentFile().delete();
			}
			store.removeFromIndex(getVersion());
			return ret;
		} finally {
			store.unlock();
		}
	}

	protected synchronized boolean isClosed() {
		return closed;
	}

	protected synchronized void addOpenBlobs(Collection<String> set) {
		set.addAll(open.keySet());
	}

	protected File getDirectory() {
		return store.getDirectory();
	}

	protected String getVersion() {
		return version;
	}

	protected void watch(String uri, DiskListener listener) {
		store.watch(uri, listener);
	}

	protected boolean unwatch(String uri, DiskListener listener) {
		return store.unwatch(uri, listener);
	}

	protected Lock readLock() {
		return store.readLock();
	}

	private Map<String, DiskBlob> readChanges(File changes) throws IOException {
		Lock readLock = store.readLock();
		try {
			readLock.lock();
			BufferedReader reader = new BufferedReader(new FileReader(changes));
			try {
				String uri;
				Map<String, DiskBlob> map = new HashMap<String, DiskBlob>();
				while ((uri = reader.readLine()) != null) {
					map.put(uri, new DiskBlob(this, uri));
				}
				return map;
			} finally {
				reader.close();
			}
		} catch (FileNotFoundException e) {
			return new HashMap<String, DiskBlob>();
		} finally {
			readLock.unlock();
		}
	}

	private File writeChanges(String iri, Set<String> changes)
			throws IOException {
		File file = newJournalFile(iri);
		PrintWriter writer = new PrintWriter(new FileWriter(file));
		try {
			for (String uri : changes) {
				writer.println(uri);
			}
		} finally {
			writer.close();
		}
		return file;
	}

	private File newJournalFile(String iri) {
		int code = iri.hashCode();
		File file;
		do {
			String name = Integer.toHexString(code++);
			while (name.length() < 8) {
				name = '0' + name;
			}
			name = name.substring(0, 4) + File.separatorChar + name.substring(4);
			file = new File(journal, name);
		} while (file.exists());
		file.getParentFile().mkdirs();
		return entry = file;
	}

}
