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
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.openrdf.store.blob.BlobStore;

@Deprecated
public class FileBlobStore implements BlobStore {
	private final File dir;
	private final ReentrantReadWriteLock diskLock = new ReentrantReadWriteLock();
	private final Map<URI, Set<FileListener>> listeners = new HashMap<URI, Set<FileListener>>();
	/** Transaction Hex -> URI */
	private final Map<String, String> transIDs;
	/** Transaction Hex -> open DiskTransaction */
	private final Map<String, FileTransaction> transactions;

	public FileBlobStore(File dir) throws IOException {
		this.dir = dir;
		this.transIDs = new LinkedHashMap<String, String>();
		this.transactions = new WeakHashMap<String, FileTransaction>();
	}

	public int getMaxHistoryLength() {
		return 0;
	}

	public void setMaxHistoryLength(int maxHistory) {
		// ignore
	}

	public String[] getHistory() {
		return new String[0];
	}

	public FileTransaction reopen(String iri) throws IOException {
		throw new IllegalArgumentException("No history information is persisted in this store");
	}

	public FileTransaction open(String iri) throws IOException {
		String id = getHexID(iri);
		synchronized (transactions) {
			FileTransaction ref = transactions.get(id);
			if (ref != null)
				return ref;
			ref = new FileTransaction(this, id);
			transactions.put(id, ref);
			return ref;
		}
	}

	public boolean erase() throws IOException {
		lock();
		try {
			boolean result = true;
			File[] listFiles = dir.listFiles();
			if (listFiles != null) {
				for (File f : listFiles) {
					if (!deltree(f)) {
						result = false;
					}
				}
			}
			return result;
		} finally {
			unlock();
		}
	}

	protected File getDirectory() {
		return dir;
	}

	protected void watch(URI uri, FileListener listener) {
		synchronized (listeners) {
			Set<FileListener> set = listeners.get(uri);
			if (set == null) {
				listeners.put(uri, set = new HashSet<FileListener>());
			}
			set.add(listener);
		}
	}

	protected boolean unwatch(URI uri, FileListener listener) {
		synchronized (listeners) {
			Set<FileListener> set = listeners.get(uri);
			if (set == null)
				return false;
			boolean ret = set.remove(listener);
			if (set.isEmpty()) {
				listeners.remove(uri);
			}
			return ret;
		}
	}

	protected Lock readLock() {
		return diskLock.readLock();
	}

	protected void lock() {
		diskLock.writeLock().lock();
	}

	protected boolean isLockedByCurrentThread() {
		return diskLock.isWriteLockedByCurrentThread();
	}

	protected void unlock() {
		diskLock.writeLock().unlock();
	}

	protected void changed(String id, Collection<URI> blobs) throws IOException {
		synchronized (transIDs) {
			String uri = transIDs.get(id);
			if (uri == null)
				throw new IllegalArgumentException("Unknown transaction id: "
						+ id);
		}
		for (URI uri : blobs) {
			Set<FileListener> set = listeners.get(uri);
			if (set != null) {
				for (FileListener listener : set) {
					listener.changed(uri);
				}
			}
		}
	}

	/**
	 * Maps Hex ID to transaction IRIs
	 */
	protected Map<String, String> getIriFromHexIDs(Collection<String> ids) {
		Map<String, String> iris = new LinkedHashMap<String, String>();
		synchronized (transIDs) {
			for (String id : ids) {
				iris.put(id, transIDs.get(id));
			}
		}
		return iris;
	}

	private String getHexID(String iri) {
		int code = iri.hashCode();
		String id = Integer.toHexString(Math.abs(code));
		synchronized (transIDs) {
			while (transIDs.containsKey(id)
					&& !iri.equals(transIDs.get(id))) {
				id = Integer.toHexString(Math.abs(++code));
			}
			if (!transIDs.containsKey(id)) {
				transIDs.put(id, iri);
			}
		}
		return id;
	}

	private boolean deltree(File directory) {
		if (directory == null || !directory.exists()) {
			return true;
		}

		boolean result = true;
		if (directory.isFile()) {
			result = directory.delete();
		}
		else {
			for (File f : directory.listFiles()) {
				if (!deltree(f)) {
					result = false;
				}
			}
			if (!directory.delete()) {
				result = false;
			}
		}
		return result;
	}

}
