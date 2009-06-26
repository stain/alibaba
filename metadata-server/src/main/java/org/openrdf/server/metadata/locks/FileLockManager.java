package org.openrdf.server.metadata.locks;

import info.aduna.concurrent.locks.Lock;
import info.aduna.concurrent.locks.ReadWriteLockManager;
import info.aduna.concurrent.locks.WritePrefReadWriteLockManager;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

public class FileLockManager {
	private final class ObjectLock implements Lock {
		private Lock lock;
		private Object target;
		private ReadWriteLockManager manager;

		private ObjectLock(Lock lock, Object target,
				ReadWriteLockManager manager) {
			this.lock = lock;
			this.target = target;
			this.manager = manager;
		}

		public boolean isActive() {
			return target != null && manager != null && lock.isActive();
		}

		public void release() {
			lock.release();
			target = null;
			manager = null;
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
		Lock lock = lock(manager, shared);
		return new ObjectLock(lock, target, manager);
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
		ref = new WeakReference<ReadWriteLockManager>(manager);
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
