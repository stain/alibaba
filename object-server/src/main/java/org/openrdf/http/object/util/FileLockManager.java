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
package org.openrdf.http.object.util;

import info.aduna.concurrent.locks.Lock;
import info.aduna.concurrent.locks.ReadWriteLockManager;
import info.aduna.concurrent.locks.WritePrefReadWriteLockManager;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Locks out conflicting requests that are for the same resource.
 */
public class FileLockManager {
	private static class ObjectLockManager implements ReadWriteLockManager {
		private ReadWriteLockManager manager;

		public ObjectLockManager(ReadWriteLockManager manager) {
			this.manager = manager;
		}

		public Lock getReadLock() throws InterruptedException {
			return new ObjectLock(manager.getReadLock());
		}

		public Lock getWriteLock() throws InterruptedException {
			return new ObjectLock(manager.getWriteLock());
		}

		public Lock tryReadLock() {
			return new ObjectLock(manager.tryReadLock());
		}

		public Lock tryWriteLock() {
			return new ObjectLock(manager.tryWriteLock());
		}
	}

	private static class ObjectLock implements Lock {
		private Lock lock;

		public ObjectLock(Lock lock) {
			this.lock = lock;
		}

		public boolean isActive() {
			return lock.isActive();
		}

		public void release() {
			lock.release();
		}
	}

	private boolean trackLocks;
	private Map<Object, WeakReference<ReadWriteLockManager>> managers = new WeakHashMap<Object, WeakReference<ReadWriteLockManager>>();

	public FileLockManager() {
		this(false);
	}

	public FileLockManager(boolean trackLocks) {
		this.trackLocks = trackLocks;
	}

	public synchronized Lock lock(File target, boolean shared)
			throws InterruptedException {
		ReadWriteLockManager manager = getLockManager(target);
		return lock(manager, shared);
	}

	private ReadWriteLockManager getLockManager(Object target) {
		ReadWriteLockManager manager;
		WeakReference<ReadWriteLockManager> ref = managers.get(target);
		if (ref == null)
			return createLockManager(target);
		manager = ref.get();
		if (manager == null)
			return createLockManager(target);
		return manager;
	}

	private ReadWriteLockManager createLockManager(Object target) {
		ReadWriteLockManager manager;
		WeakReference<ReadWriteLockManager> ref;
		manager = new WritePrefReadWriteLockManager(trackLocks);
		manager = new ObjectLockManager(manager);
		ref = new WeakReference<ReadWriteLockManager>(manager);
		managers.remove(target); // ensure new instance is used as the key
		managers.put(target, ref);
		return manager;
	}

	private Lock lock(ReadWriteLockManager manager, boolean shared)
			throws InterruptedException {
		if (shared)
			return manager.getReadLock();
		return manager.getWriteLock();
	}
}
