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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
	private volatile boolean closed;

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

	public BlobObject open(String uri) {
		synchronized (open) {
			DiskBlob blob = open.get(uri);
			if (blob != null)
				return blob;
			open.put(uri, blob = new DiskBlob(this, uri));
			return blob;
		}
	}

	public void prepare() throws IOException {
		if (closed)
			throw new IllegalStateException("Transaction has already completed");
		closed = true;
		store.lock();
		try {
			synchronized (open) {
				for (DiskBlob blob : open.values()) {
					if (blob.hasConflict())
						throw new IOException(
								"Resource has since been modified: "
										+ blob.toUri());
				}
			}
		} catch (IOException e) {
			store.unlock();
			throw e;
		} catch (RuntimeException e) {
			store.unlock();
			throw e;
		} catch (Error e) {
			store.unlock();
			throw e;
		}
	}

	public void commit() throws IOException {
		if (!store.isLockedByCurrentThread()) {
			prepare();
		}
		Set<String> set;
		synchronized (open) {
			set = new HashSet<String>(open.size());
			for (Map.Entry<String, DiskBlob> e : open.entrySet()) {
				if (e.getValue().sync()) {
					set.add(e.getKey());
				}
			}
			open.keySet().retainAll(set);
		}
		if (!set.isEmpty()) {
			File file = writeChanges(this.getVersion(), set);
			store.changed(this.getVersion(), set, file);
		}
		store.unlock();
	}

	public void rollback() {
		try {
			synchronized (open) {
				for (DiskBlob blob : open.values()) {
					blob.abort();
				}
			}
		} finally {
			if (store.isLockedByCurrentThread()) {
				store.unlock();
			}
		}
	}

	public boolean erase() throws IOException {
		if (!closed)
			throw new IllegalStateException("Transaction is not complete");
		assert entry != null;
		store.lock();
		try {
			synchronized (open) {
				for (DiskBlob blob : open.values()) {
					blob.erase();
				}
			}
			boolean ret = entry.delete();
			store.removeFromIndex(getVersion());
			return ret;
		} finally {
			store.unlock();
		}
	}

	protected File getDirectory() {
		return store.getDirectory();
	}

	protected String getVersion() {
		return version;
	}

	protected boolean isClosed() {
		return closed;
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

	protected void addOpenBlobs(Collection<String> set) {
		synchronized (open) {
			set.addAll(open.keySet());
		}
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

	private File writeChanges(String iri, Set<String> changes) throws IOException {
		File file = getJournalFile(iri);
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

	private File getJournalFile(String iri) {
		if (entry != null)
			return entry;
		journal.mkdirs();
		int code = iri.hashCode();
		File file;
		do {
			file = new File(journal, Integer.toHexString(Math.abs(code)));
		} while (file.exists());
		return entry = file;
	}

}
