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


import java.io.File;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Locks out conflicting requests that are for the same resource.
 */
public class FileLockManager {
	private Map<Object, ReentrantReadWriteLock> managers = new WeakHashMap<Object, ReentrantReadWriteLock>();

	public Lock tryLock(File target, boolean shared) throws InterruptedException {
		ReentrantReadWriteLock manager = getLockManager(target);
		Lock lock = getLock(manager, shared);
		if (lock.tryLock())
			return lock;
		return null;
	}

	public Lock lock(File target, boolean shared) throws InterruptedException {
		ReentrantReadWriteLock manager = getLockManager(target);
		Lock lock = getLock(manager, shared);
		lock.lock();
		return lock;
	}

	private synchronized ReentrantReadWriteLock getLockManager(Object target) {
		ReentrantReadWriteLock manager = managers.get(target);
		if (manager == null)
			return createLockManager(target);
		return manager;
	}

	private ReentrantReadWriteLock createLockManager(Object target) {
		ReentrantReadWriteLock manager = new ReentrantReadWriteLock();
		managers.remove(target); // ensure new instance is used as the key
		managers.put(target, manager);
		return manager;
	}

	private Lock getLock(ReentrantReadWriteLock manager, boolean shared)
			throws InterruptedException {
		if (shared)
			return manager.readLock();
		return manager.writeLock();
	}
}
