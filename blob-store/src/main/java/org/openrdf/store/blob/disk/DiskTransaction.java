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
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import org.openrdf.store.blob.BlobObject;
import org.openrdf.store.blob.BlobTransaction;

public class DiskTransaction implements BlobTransaction {
	private final DiskBlobStore store;
	private final String hex;
	private final String iri;
	private final Map<URI, BlobFile> open;
	private volatile boolean closed;
	private String[] history;

	protected DiskTransaction(DiskBlobStore store, String hex, String iri,
			boolean reopened) throws IOException {
		this.store = store;
		this.hex = hex;
		this.iri = iri;
		this.closed = reopened;
		if (closed) {
			this.history = getHistory(store, iri);
			this.open = readChanges(this.getID());
		} else {
			this.history = null;
			this.open = new HashMap<URI, BlobFile>();
		}
	}

	public String[] getHistory() {
		if (history == null && !closed)
			return store.getHistory();
		if (history == null)
			return getHistory(store, iri);
		return history;
	}

	public BlobObject open(URI uri) {
		synchronized (open) {
			BlobFile blob = open.get(uri);
			if (blob != null)
				return blob;
			open.put(uri, blob = new BlobFile(this, uri));
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
				for (BlobFile blob : open.values()) {
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
		Set<URI> set;
		synchronized (open) {
			set = new HashSet<URI>(open.size());
			for (BlobFile blob : open.values()) {
				if (blob.sync()) {
					set.add(blob.toUri());
				}
			}
		}
		if (!set.isEmpty()) {
			store.changed(this.getID(), set);
			writeChanges(this.getID(), set);
		}
		store.unlock();
	}

	public void rollback() {
		try {
			synchronized (open) {
				for (BlobFile blob : open.values()) {
					blob.abort();
				}
			}
		} finally {
			if (store.isLockedByCurrentThread()) {
				store.unlock();
			}
		}
	}

	protected File getDirectory() {
		return store.getDirectory();
	}

	protected boolean isClosed() {
		return closed;
	}

	protected String getID() {
		return hex;
	}

	protected int getMaxHistoryLength() {
		return store.getMaxHistoryLength();
	}

	protected Map<String, String> getIriFromHexIDs(Collection<String> ids) {
		return store.getIriFromHexIDs(ids);
	}

	protected void watch(URI uri, BlobListener listener) {
		store.watch(uri, listener);
	}

	protected boolean unwatch(URI uri, BlobListener listener) {
		return store.unwatch(uri, listener);
	}

	protected Lock readLock() {
		return store.readLock();
	}

	protected boolean erase() throws IOException {
		synchronized (open) {
			for (BlobFile blob : open.values()) {
				blob.erase();
			}
		}
		File dir = new File(store.getDirectory(), "$changes");
		boolean ret = new File(dir, this.getID()).delete();
		String[] list = dir.list();
		if (list != null && list.length == 0) {
			dir.delete();
		}
		return ret;
	}

	private String[] getHistory(DiskBlobStore store, String iri) {
		String[] history = store.getHistory();
		List<String> list = new ArrayList<String>(history.length);
		for (int i = 0; i < history.length && !history[i].equals(iri); i++) {
			list.add(i, history[i]);
		}
		if (list.size() < history.length)
			return list.toArray(new String[list.size()]);
		return history;
	}

	private void writeChanges(String id, Set<URI> changes) throws IOException {
		File dir = new File(store.getDirectory(), "$changes");
		dir.mkdirs();
		PrintWriter writer = new PrintWriter(new FileWriter(new File(dir, id)));
		try {
			for (URI uri : changes) {
				writer.println(uri.toString());
			}
		} finally {
			writer.close();
		}
	}

	private Map<URI, BlobFile> readChanges(String id) throws IOException {
		File dir = new File(store.getDirectory(), "$changes");
		File f = new File(dir, id);
		try {
			BufferedReader reader = new BufferedReader(new FileReader(f));
			try {
				String line;
				Map<URI, BlobFile> map = new HashMap<URI, BlobFile>();
				while ((line = reader.readLine()) != null) {
					URI uri = URI.create(line);
					map.put(uri, new BlobFile(this, uri));
				}
				return map;
			} finally {
				reader.close();
			}
		} catch (FileNotFoundException e) {
			return new HashMap<URI, BlobFile>();
		}
	}

}
