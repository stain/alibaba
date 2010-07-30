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

public class ManagedThreadPool extends ThreadPoolExecutor implements
		ExecutorService, ThreadPoolMXBean {
	private static final String MXBEAN_TYPE = "org.openrdf:type=ManagedThreads";
	private Logger logger = LoggerFactory.getLogger(ManagedThreadPool.class);
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
		this.oname = MXBEAN_TYPE + ",name=" + name;
		registerMBean();
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
