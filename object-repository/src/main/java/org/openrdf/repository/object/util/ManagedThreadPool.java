/*
 * Copyright (c) 2010, Zepheira LLC, Some rights reserved.
 * Copyright (c) 2011, Talis Inc., Some rights reserved.
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
package org.openrdf.repository.object.util;

import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exposes ThreadPoolExecutor properties in an MXBean.
 * 
 * @author James Leigh
 * 
 */
public class ManagedThreadPool implements ExecutorService, ThreadPoolMXBean {
	private static final String MXBEAN_TYPE = "org.openrdf:type=ManagedThreads";
	private final Logger logger = LoggerFactory.getLogger(ManagedThreadPool.class);
	private ThreadPoolExecutor delegate;
	private final String oname;

	public ManagedThreadPool(String name, boolean daemon) {
		this(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS,
				new SynchronousQueue<Runnable>(), name, daemon,
				new ThreadPoolExecutor.AbortPolicy());
	}

	public ManagedThreadPool(int corePoolSize, int maximumPoolSize,
			long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue, String name, boolean daemon) {
		this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
				name, daemon, new ThreadPoolExecutor.AbortPolicy());
	}

	public ManagedThreadPool(int corePoolSize, int maximumPoolSize,
			long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue, String name, boolean daemon,
			RejectedExecutionHandler handler) {
		this(new ThreadPoolExecutor(corePoolSize, maximumPoolSize,
				keepAliveTime, unit, workQueue, new NamedThreadFactory(name,
						daemon), handler));
	}

	protected ManagedThreadPool(ThreadPoolExecutor delegate) {
		this.oname = MXBEAN_TYPE + ",name=" + delegate.getThreadFactory().toString();
		setDelegate(delegate);
		registerMBean();
	}

	@Override
	public String toString() {
		return getDelegate().getThreadFactory().toString();
	}

	public void setCorePoolSize(int corePoolSize) {
		if (getCorePoolSize() > corePoolSize)
			logger.info("Increasing {} thread  pool size to {}", toString(),
					corePoolSize);
		getDelegate().setCorePoolSize(corePoolSize);
	}

	public void shutdown() {
		getDelegate().shutdown();
		unregisterMBean();
	}

	public List<Runnable> shutdownNow() {
		List<Runnable> tasks = getDelegate().shutdownNow();
		unregisterMBean();
		return tasks;
	}

	public synchronized void interruptWorkers() throws InterruptedException {
		int corePoolSize = getCorePoolSize();
		int maximumPoolSize = getMaximumPoolSize();
		long keepAliveTime = getKeepAliveTime();
		TimeUnit unit = TimeUnit.SECONDS;
		BlockingQueue<Runnable> workQueue = getDelegate().getQueue();
		ThreadFactory factory = getDelegate().getThreadFactory();
		RejectedExecutionHandler handler = getDelegate()
				.getRejectedExecutionHandler();
		try {
			logger.info("Terminating {} {} threads", getActiveCount(),
					toString());
			getDelegate().shutdown();
			if (!getDelegate().awaitTermination(1, TimeUnit.MINUTES)) {
				logger.info("Could not terminate {} {} threads",
						getActiveCount(), toString());
			}
		} finally {
			setDelegate(new ThreadPoolExecutor(corePoolSize,
					maximumPoolSize, keepAliveTime, unit, workQueue, factory,
					handler));
		}
	}

	public void clearQueue() {
		getQueue().clear();
	}

	public long getKeepAliveTime() {
		return getDelegate().getKeepAliveTime(TimeUnit.SECONDS);
	}

	public void setKeepAliveTime(long seconds) {
		getDelegate().setKeepAliveTime(seconds, TimeUnit.SECONDS);
	}

	public String[] getQueueDescription() {
		Object[] tasks = getQueue().toArray();
		String[] result = new String[tasks.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = tasks[i].toString();
		}
		return result;
	}

	public void runAllInQueue() {
		Runnable task;
		while ((task = getQueue().poll()) != null) {
			task.run();
		}
	}

	public void runNextInQueue() {
		Runnable task = getQueue().poll();
		if (task != null) {
			task.run();
		}
	}

	public int getQueueRemainingCapacity() {
		return getQueue().remainingCapacity();
	}

	public int getQueueSize() {
		return getQueue().size();
	}

	public boolean isAllowsCoreThreadTimeOut() {
		return getDelegate().allowsCoreThreadTimeOut();
	}

	public boolean isContinueExistingPeriodicTasksAfterShutdownPolicy() {
		return false;
	}

	public boolean isExecuteExistingDelayedTasksAfterShutdownPolicy() {
		return false;
	}

	public void setAllowCoreThreadTimeOut(boolean allow) {
		getDelegate().allowCoreThreadTimeOut(allow);
	}

	public void setContinueExistingPeriodicTasksAfterShutdownPolicy(
			boolean policy) {
		throw new UnsupportedOperationException();
	}

	public void setExecuteExistingDelayedTasksAfterShutdownPolicy(boolean policy) {
		throw new UnsupportedOperationException();
	}

	public void startAllCoreThreads() {
		getDelegate().prestartAllCoreThreads();
	}

	public void startCoreThread() {
		getDelegate().prestartCoreThread();
	}

	public int getActiveCount() {
		return getDelegate().getActiveCount();
	}

	public long getCompletedTaskCount() {
		return getDelegate().getCompletedTaskCount();
	}

	public int getCorePoolSize() {
		return getDelegate().getCorePoolSize();
	}

	public int getPoolSize() {
		return getDelegate().getPoolSize();
	}

	public long getTaskCount() {
		return getDelegate().getTaskCount();
	}

	public boolean isTerminating() {
		return getDelegate().isTerminating();
	}

	public int getLargestPoolSize() {
		return getDelegate().getLargestPoolSize();
	}

	public int getMaximumPoolSize() {
		return getDelegate().getMaximumPoolSize();
	}

	public void purge() {
		getDelegate().purge();
	}

	public void setMaximumPoolSize(int maximumPoolSize) {
		getDelegate().setMaximumPoolSize(maximumPoolSize);
	}

	public boolean awaitTermination(long timeout, TimeUnit unit)
			throws InterruptedException {
		return getDelegate().awaitTermination(timeout, unit);
	}

	public void execute(Runnable command) {
		getDelegate().execute(command);
	}

	public <T> List<Future<T>> invokeAll(
			Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException {
		return getDelegate().invokeAll(tasks, timeout, unit);
	}

	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
			throws InterruptedException {
		return getDelegate().invokeAll(tasks);
	}

	public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
			long timeout, TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException {
		return getDelegate().invokeAny(tasks, timeout, unit);
	}

	public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
			throws InterruptedException, ExecutionException {
		return getDelegate().invokeAny(tasks);
	}

	public boolean isShutdown() {
		return getDelegate().isShutdown();
	}

	public boolean isTerminated() {
		return getDelegate().isTerminated();
	}

	public <T> Future<T> submit(Callable<T> task) {
		return getDelegate().submit(task);
	}

	public <T> Future<T> submit(Runnable task, T result) {
		return getDelegate().submit(task, result);
	}

	public Future<?> submit(Runnable task) {
		return getDelegate().submit(task);
	}

	protected synchronized ThreadPoolExecutor getDelegate() {
		return delegate;
	}

	protected synchronized void setDelegate(ThreadPoolExecutor delegate) {
		this.delegate = delegate;
	}

	private BlockingQueue<Runnable> getQueue() {
		return getDelegate().getQueue();
	}

	private void registerMBean() {
		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			mbs.registerMBean(this, new ObjectName(oname));
		} catch (Exception e) {
			logger.info(e.toString(), e);
		}
	}

	private void unregisterMBean() {
		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			mbs.unregisterMBean(new ObjectName(oname));
		} catch (Exception e) {
			logger.info(e.toString(), e);
		}
	}

}
