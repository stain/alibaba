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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import org.openrdf.store.blob.BlobObject;
import org.openrdf.store.blob.BlobTransaction;

@Deprecated
public class FileTransaction implements BlobTransaction {
	private final FileBlobStore store;
	private final String hex;
	private final Map<URI, FileObjectImpl> open;
	private volatile boolean closed;

	protected FileTransaction(FileBlobStore store, String hex) throws IOException {
		this.store = store;
		this.hex = hex;
		this.open = new HashMap<URI, FileObjectImpl>();
	}

	public String[] getHistory() {
		return store.getHistory();
	}

	public BlobObject open(URI uri) {
		synchronized (open) {
			FileObjectImpl blob = open.get(uri);
			if (blob != null)
				return blob;
			open.put(uri, blob = new FileObjectImpl(this, uri));
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
				for (FileObjectImpl blob : open.values()) {
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
			for (FileObjectImpl blob : open.values()) {
				if (blob.sync()) {
					set.add(blob.toUri());
				}
			}
		}
		if (!set.isEmpty()) {
			store.changed(this.getID(), set);
		}
		store.unlock();
	}

	public void rollback() {
		try {
			synchronized (open) {
				for (FileObjectImpl blob : open.values()) {
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

	protected void watch(URI uri, FileListener listener) {
		store.watch(uri, listener);
	}

	protected boolean unwatch(URI uri, FileListener listener) {
		return store.unwatch(uri, listener);
	}

	protected Lock readLock() {
		return store.readLock();
	}

}
