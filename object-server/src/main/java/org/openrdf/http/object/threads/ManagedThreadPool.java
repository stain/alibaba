/*
 * Copyright (c) 2010, Zepheira LLC Some rights reserved.
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
package org.openrdf.http.object.threads;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
public class ManagedThreadPool extends ThreadPoolExecutor implements
		ExecutorService, ThreadPoolMXBean {
	private static final String MXBEAN_TYPE = "org.openrdf:type=ManagedThreads";
	private Logger logger = LoggerFactory.getLogger(ManagedThreadPool.class);
	private String name;
	private String oname;

	public ManagedThreadPool(String name, boolean daemon) {
		this(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS,
				new SynchronousQueue<Runnable>(), name, daemon,
				new AbortPolicy());
	}

	public ManagedThreadPool(int corePoolSize, int maximumPoolSize,
			long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue, String name, boolean daemon) {
		this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
				name, daemon, new AbortPolicy());
	}

	public ManagedThreadPool(int corePoolSize, int maximumPoolSize,
			long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue, String name, boolean daemon,
			RejectedExecutionHandler handler) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
				new NamedThreadFactory(name, daemon), handler);
		this.name = name;
		this.oname = MXBEAN_TYPE + ",name=" + name;
		registerMBean();
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public void setCorePoolSize(int corePoolSize) {
		if (getCorePoolSize() > corePoolSize)
			logger.info("Increasing {} thread  pool size to {}", name, corePoolSize);
		super.setCorePoolSize(corePoolSize);
	}

	@Override
	public void shutdown() {
		super.shutdown();
		unregisterMBean();
	}

	@Override
	public List<Runnable> shutdownNow() {
		List<Runnable> tasks = super.shutdownNow();
		unregisterMBean();
		return tasks;
	}

	public void clearQueue() {
		getQueue().clear();
	}

	public long getKeepAliveTime() {
		return getKeepAliveTime(TimeUnit.SECONDS);
	}

	public void setKeepAliveTime(long seconds) {
		setKeepAliveTime(seconds, TimeUnit.SECONDS);
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
		return allowsCoreThreadTimeOut();
	}

	public boolean isContinueExistingPeriodicTasksAfterShutdownPolicy() {
		return false;
	}

	public boolean isExecuteExistingDelayedTasksAfterShutdownPolicy() {
		return false;
	}

	public void setAllowCoreThreadTimeOut(boolean allow) {
		allowCoreThreadTimeOut(allow);
	}

	public void setContinueExistingPeriodicTasksAfterShutdownPolicy(
			boolean policy) {
		throw new UnsupportedOperationException();
	}

	public void setExecuteExistingDelayedTasksAfterShutdownPolicy(boolean policy) {
		throw new UnsupportedOperationException();
	}

	public void startAllCoreThreads() {
		prestartAllCoreThreads();
	}

	public void startCoreThread() {
		prestartCoreThread();
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
