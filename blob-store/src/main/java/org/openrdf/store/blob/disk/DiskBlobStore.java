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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.openrdf.store.blob.BlobStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiskBlobStore implements BlobStore {
	private final Logger logger = LoggerFactory.getLogger(DiskBlobStore.class);
	private final File dir;
	private final ReentrantReadWriteLock diskLock = new ReentrantReadWriteLock();
	private final Map<URI, Set<BlobListener>> listeners = new HashMap<URI, Set<BlobListener>>();
	/** synchronized by transIDs */
	private final List<String> history = new LinkedList<String>();
	/** Transaction Hex -> URI */
	private final Map<String, String> transIRIs;
	/** Transaction Hex -> open DiskTransaction */
	private final Map<String, DiskTransaction> transactions;
	private int maxHistory = 10240;

	public DiskBlobStore(File dir) throws IOException {
		this.dir = dir;
		this.transIRIs = readTransactions(dir);
		this.history.addAll(transIRIs.keySet());
		this.transactions = new WeakHashMap<String, DiskTransaction>();
	}

	public int getMaxHistoryLength() {
		return maxHistory;
	}

	public void setMaxHistoryLength(int maxHistory) throws IOException {
		int previously = this.maxHistory;
		this.maxHistory = maxHistory;
		if (previously > maxHistory) {
			this.lock();
			try {
				synchronized (transIRIs) {
					if (history.size() > maxHistory) {
						writeTransactions(dir, history, transIRIs);
					}
				}
			} finally {
				this.unlock();
			}
		}
	}

	public String[] getHistory() {
		synchronized (transIRIs) {
			String[] ret = new String[history.size()];
			for (int i = 0; i < ret.length; i++) {
				ret[i] = transIRIs.get(history.get(i));
			}
			return ret;
		}
	}

	public DiskTransaction reopen(String iri) throws IOException {
		int code = iri.hashCode();
		String id = Integer.toHexString(Math.abs(code));
		synchronized (transIRIs) {
			if (!iri.equals(transIRIs.get(id))) {
				id = null;
				for (Map.Entry<String, String> e : transIRIs.entrySet()) {
					if (e.getValue().equals(iri))
						id = e.getKey();
				}
				if (id == null)
					throw new IllegalArgumentException(
							"Could not find transaction in history: " + iri);
			}
		}
		synchronized (transactions) {
			DiskTransaction ref = transactions.get(id);
			if (ref != null)
				return ref;
			ref = new DiskTransaction(this, id, iri, true);
			transactions.put(id, ref);
			return ref;
		}
	}

	public DiskTransaction open(String iri) throws IOException {
		String id = getHexID(iri);
		synchronized (transactions) {
			DiskTransaction ref = transactions.get(id);
			if (ref != null)
				return ref;
			ref = new DiskTransaction(this, id, iri, false);
			transactions.put(id, ref);
			return ref;
		}
	}

	public boolean erase() throws IOException {
		lock();
		try {
			for (String iri : getHistory()) {
				reopen(iri).erase();
			}
			return getTransactionFile(getDirectory()).delete();
		} finally {
			unlock();
		}
	}

	protected File getDirectory() {
		return dir;
	}

	protected void watch(URI uri, BlobListener listener) {
		synchronized (listeners) {
			Set<BlobListener> set = listeners.get(uri);
			if (set == null) {
				listeners.put(uri, set = new HashSet<BlobListener>());
			}
			set.add(listener);
		}
	}

	protected boolean unwatch(URI uri, BlobListener listener) {
		synchronized (listeners) {
			Set<BlobListener> set = listeners.get(uri);
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
		synchronized (transIRIs) {
			String uri = transIRIs.get(id);
			if (uri == null)
				throw new IllegalArgumentException("Unknown transaction id: "
						+ id);
			history.add(id);
			writeTransactions(dir, history, transIRIs);
		}
		for (URI uri : blobs) {
			Set<BlobListener> set = listeners.get(uri);
			if (set != null) {
				for (BlobListener listener : set) {
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
		synchronized (transIRIs) {
			for (String id : ids) {
				iris.put(id, transIRIs.get(id));
			}
		}
		return iris;
	}

	private String getHexID(String iri) {
		int code = iri.hashCode();
		String id = Integer.toHexString(Math.abs(code));
		synchronized (transIRIs) {
			while (transIRIs.containsKey(id) && !iri.equals(transIRIs.get(id))) {
				id = Integer.toHexString(Math.abs(++code));
			}
			if (!transIRIs.containsKey(id)) {
				transIRIs.put(id, iri);
			}
		}
		return id;
	}

	private Map<String, String> readTransactions(File dir) throws IOException {
		File f = getTransactionFile(dir);
		try {
			BufferedReader reader = new BufferedReader(new FileReader(f));
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
		}
	}

	/** synchronized by transIRIs */
	private void writeTransactions(File dir, List<String> keys,
			Map<String, String> values) throws IOException {
		int skip = keys.size() - getMaxHistoryLength();
		File f = getTransactionFile(dir);
		PrintWriter writer = new PrintWriter(new FileWriter(f));
		try {
			Iterator<String> iter = keys.iterator();
			while (iter.hasNext()) {
				String key = iter.next();
				if (skip <= 0) {
					writer.print(key);
					writer.print(' ');
					writer.println(values.get(key));
				} else {
					skip--;
					try {
						reopen(transIRIs.get(key)).erase();
						iter.remove();
					} catch (IOException e) {
						logger.error(e.toString(), e);
					}
				}
			}
		} finally {
			writer.close();
		}
	}

	private File getTransactionFile(File dir) {
		return new File(dir, "$transactions");
	}

}
