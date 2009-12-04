package org.openrdf.server.metadata.locks;

import info.aduna.concurrent.locks.Lock;
import info.aduna.concurrent.locks.ReadWriteLockManager;
import info.aduna.concurrent.locks.WritePrefReadWriteLockManager;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

public class FileLockManager {
	private static class ObjectLockManager implements ReadWriteLockManager {
		private Object target;
		private ReadWriteLockManager manager;

		public ObjectLockManager(Object target, ReadWriteLockManager manager) {
			this.target = target;
			this.manager = manager;
		}

		public Object getTarget() {
			return target;
		}

		public Lock getReadLock() throws InterruptedException {
			return new ObjectLock(manager.getReadLock(), this);
		}

		public Lock getWriteLock() throws InterruptedException {
			return new ObjectLock(manager.getWriteLock(), this);
		}

		public Lock tryReadLock() {
			return new ObjectLock(manager.tryReadLock(), this);
		}

		public Lock tryWriteLock() {
			return new ObjectLock(manager.tryWriteLock(), this);
		}
	}

	private static class ObjectLock implements Lock {
		private Lock lock;
		private ObjectLockManager manager;

		public ObjectLock(Lock lock, ObjectLockManager manager) {
			this.lock = lock;
			this.manager = manager;
		}

		public ObjectLockManager getManager() {
			return manager;
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
		manager = new ObjectLockManager(target, manager);
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
