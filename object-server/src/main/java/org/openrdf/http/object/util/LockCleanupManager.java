package org.openrdf.http.object.util;

import info.aduna.concurrent.locks.Lock;
import info.aduna.concurrent.locks.ReadWriteLockManager;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LockCleanupManager {
	private static class LockTrace {
		Lock lock;
		WeakReference<Lock> weak;
		String ref;
		Throwable stack;
	}

	private Logger logger = LoggerFactory.getLogger(LockCleanupManager.class);
	private ReadWriteLockManager delegate;
	private boolean writing;
	private Set<LockTrace> locks = new HashSet<LockTrace>();

	public LockCleanupManager(ReadWriteLockManager delegate) {
		this.delegate = delegate;
	}

	public synchronized Lock getReadLock(String ref)
			throws InterruptedException {
		if (writing) {
			releaseAbanded();
		}
		Lock lock = delegate.tryReadLock();
		if (lock == null) {
			System.gc();
			Thread.yield();
			releaseAbanded();
			lock = delegate.getReadLock();
		}
		writing = false;
		return track(lock, ref, new Throwable());
	}

	public synchronized Lock getWriteLock(String ref)
			throws InterruptedException {
		releaseAbanded();
		Lock lock = delegate.tryWriteLock();
		if (lock == null) {
			System.gc();
			Thread.yield();
			releaseAbanded();
			lock = delegate.getWriteLock();
		}
		writing = true;
		return track(lock, ref, new Throwable());
	}

	private void releaseAbanded() {
		synchronized (locks) {
			if (!locks.isEmpty()) {
				LockTrace[] ar = new LockTrace[locks.size()];
				for (LockTrace trace : locks.toArray(ar)) {
					if (trace.lock.isActive() && trace.weak.get() == null) {
						String msg = "Lock " + trace.ref + " abandoned";
						logger.warn(msg, trace.stack);
						trace.lock.release();
					}
				}
			}
		}
	}

	private Lock track(final Lock lock, String ref, Throwable stack) {
		final LockTrace trace = new LockTrace();
		trace.lock = lock;
		trace.ref = ref;
		trace.stack = stack;
		Lock weakLock = new Lock() {
			public boolean isActive() {
				return lock.isActive();
			}

			public void release() {
				try {
					lock.release();
				} finally {
					synchronized (locks) {
						locks.remove(trace);
					}
				}
			}
		};
		trace.weak = new WeakReference<Lock>(weakLock);
		synchronized (locks) {
			locks.add(trace);
		}
		return weakLock;
	}

}
