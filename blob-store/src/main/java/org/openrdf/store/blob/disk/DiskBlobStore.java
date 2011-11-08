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
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.openrdf.store.blob.BlobObject;
import org.openrdf.store.blob.BlobStore;

public class DiskBlobStore implements BlobStore {
	private static final int MAX_HISTORY = 1000;

	private interface Closure<V> {
		V call(String name, String iri) throws IOException;
	};

	private final File dir;
	private final File journal;
	private final String prefix;
	private final AtomicLong seq = new AtomicLong(0);
	private final ReentrantReadWriteLock diskLock = new ReentrantReadWriteLock();
	private final Map<String, Set<DiskListener>> listeners = new HashMap<String, Set<DiskListener>>();
	/** version -> open DiskTransaction */
	private final Map<String, WeakReference<DiskBlobVersion>> transactions;

	public DiskBlobStore(File dir) throws IOException {
		assert dir != null;
		this.dir = dir;
		this.journal = new File(dir, "$versions");
		this.transactions = new WeakHashMap<String, WeakReference<DiskBlobVersion>>();
		this.prefix = new File(getDirectory(), "trx").toURI().toString();
		eachEntry(new Closure<Void>() {
			public Void call(String name, String iri) {
				if (iri.startsWith(prefix)) {
					try {
						String suffix = iri.substring(prefix.length());
						seq.set(Math.max(seq.get(), Long.parseLong(suffix)));
					} catch (NumberFormatException exc) {
						// ignore
					}
				}
				return null;
			}
		});
	}

	public String toString() {
		return dir.toString();
	}

	public int hashCode() {
		return dir.hashCode();
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DiskBlobStore other = (DiskBlobStore) obj;
		if (!dir.equals(other.dir))
			return false;
		return true;
	}

	public BlobObject open(String uri) throws IOException {
		return new LiveDiskBlob(this, uri);
	}

	public DiskBlobVersion newVersion() throws IOException {
		return newVersion(prefix + seq.incrementAndGet());
	}

	public DiskBlobVersion newVersion(String version) throws IOException {
		synchronized (transactions) {
			WeakReference<DiskBlobVersion> ref = transactions.get(version);
			if (ref != null) {
				DiskBlobVersion result = ref.get();
				if (result != null)
					return result;
			}
			DiskBlobVersion result = new DiskBlobVersion(this, version, journal);
			ref = new WeakReference<DiskBlobVersion>(result);
			transactions.put(version, ref);
			return result;
		}
	}

	public DiskBlobVersion openVersion(final String version) throws IOException {
		File entry = eachEntry(new Closure<File>() {
			public File call(String name, String id) {
				if (id.equals(version))
					return new File(journal, name);
				return null;
			}
		});
		if (entry == null)
			throw new IllegalArgumentException("Unknown blob version: " + version);
		synchronized (transactions) {
			WeakReference<DiskBlobVersion> ref = transactions.get(version);
			if (ref != null) {
				DiskBlobVersion result = ref.get();
				if (result != null)
					return result;
			}
			DiskBlobVersion result = new DiskBlobVersion(this, version, entry);
			ref = new WeakReference<DiskBlobVersion>(result);
			transactions.put(version, ref);
			return result;
		}
	}

	public String[] getRecentModifications() throws IOException {
		Lock readLock = readLock();
		try {
			readLock.lock();
			final LinkedList<String> history = new LinkedList<String>();
			final Map<String, String> map = new HashMap<String, String>(MAX_HISTORY);
			eachEntry(new Closure<Void>() {
				public Void call(String name, String iri) {
					history.addFirst(name);
					if (history.size() > MAX_HISTORY) {
						history.removeLast();
						map.remove(name);
					}
					map.put(name, iri);
					return null;
				}
			});
			final LinkedList<String> blobs = new LinkedList<String>();
			for (String name : history) {
				String version = map.get(name);
				File entry = new File(journal, name);
				new DiskBlobVersion(this, version, entry).addOpenBlobs(blobs);
				if (blobs.size() >= MAX_HISTORY)
					break;
			}
			return blobs.toArray(new String[blobs.size()]);
		} finally {
			readLock.unlock();
		}
	}

	public boolean erase() throws IOException {
		lock();
		try {
			eachEntry(new Closure<Void>() {
				public Void call(String name, String iri) throws IOException {
					openVersion(iri).erase();
					return null;
				}
			});
			return true;
		} finally {
			unlock();
		}
	}

	protected File getDirectory() {
		return dir;
	}

	protected void watch(String uri, DiskListener listener) {
		synchronized (listeners) {
			Set<DiskListener> set = listeners.get(uri);
			if (set == null) {
				listeners.put(uri, set = new HashSet<DiskListener>());
			}
			set.add(listener);
		}
	}

	protected boolean unwatch(String uri, DiskListener listener) {
		synchronized (listeners) {
			Set<DiskListener> set = listeners.get(uri);
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

	protected void changed(String version, Collection<String> blobs, File entry)
			throws IOException {
		for (String uri : blobs) {
			Set<DiskListener> set = listeners.get(uri);
			if (set != null) {
				for (DiskListener listener : set) {
					listener.changed(uri);
				}
			}
		}
		appendIndex(entry, version);
	}

	protected void removeFromIndex(String erasing) throws IOException {
		lock();
		try {
			boolean empty = true;
			File index = new File(journal, "index");
			File rest = new File(journal, "index$"
					+ Integer.toHexString(Math.abs(erasing.hashCode())));
			BufferedReader reader = new BufferedReader(new FileReader(index));
			try {
				PrintWriter writer = new PrintWriter(new FileWriter(rest));
				try {
					String line;
					while ((line = reader.readLine()) != null) {
						String iri = line.substring(line.indexOf(' ') + 1);
						if (!iri.equals(erasing)) {
							writer.println(line);
							empty = false;
						}
					}
				} finally {
					writer.close();
				}
			} finally {
				reader.close();
			}
			index.delete();
			if (empty) {
				rest.delete();
				String[] list = journal.list();
				if (list != null && list.length == 0) {
					journal.delete();
				}
			} else {
				rest.renameTo(index);
			}
		} finally {
			unlock();
		}
	}

	private void appendIndex(File file, String iri) throws IOException {
		lock();
		try {
			PrintWriter index = new PrintWriter(new FileWriter(new File(
					journal, "index"), true));
			try {
				index.print(file.getName());
				index.print(' ');
				index.println(iri);
			} finally {
				index.close();
			}
		} finally {
			unlock();
		}
	}

	private <V> V eachEntry(Closure<V> closure) throws IOException {
		Lock readLock = readLock();
		try {
			readLock.lock();
			File index = new File(journal, "index");
			BufferedReader reader = new BufferedReader(new FileReader(index));
			try {
				String line;
				while ((line = reader.readLine()) != null) {
					String[] split = line.split("\\s+", 2);
					V ret = closure.call(split[0], split[1]);
					if (ret != null)
						return ret;
				}
			} finally {
				reader.close();
			}
		} catch (FileNotFoundException e) {
			// same as empty file
		} finally {
			readLock.unlock();
		}
		return null;
	}

}
