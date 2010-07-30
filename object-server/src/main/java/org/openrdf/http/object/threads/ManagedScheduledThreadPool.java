package org.openrdf.http.object.threads;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManagedScheduledThreadPool extends ScheduledThreadPoolExecutor
		implements ExecutorService, ThreadPoolMXBean {
	private static final String MXBEAN_TYPE = "org.openrdf:type=ManagedThreads";
	private Logger logger = LoggerFactory
			.getLogger(ManagedScheduledThreadPool.class);
	private String oname;

	public ManagedScheduledThreadPool(String name, boolean daemon) {
		this(1, name, daemon, new AbortPolicy());
	}

	public ManagedScheduledThreadPool(int corePoolSize, String name,
			boolean daemon) {
		this(corePoolSize, name, daemon, new AbortPolicy());
	}

	public ManagedScheduledThreadPool(int corePoolSize, String name,
			boolean daemon, RejectedExecutionHandler handler) {
		super(corePoolSize, new NamedThreadFactory(name, daemon), handler);
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
			if (tasks[i] instanceof RunnableScheduledFuture<?>) {
				RunnableScheduledFuture<?> f = (RunnableScheduledFuture<?>) tasks[i];
				if (f.isPeriodic()) {
					result[i] = "and periodicly " + result[i];
				}
				long minutes = f.getDelay(TimeUnit.MINUTES);
				result[i] = "in " + minutes + " minutes " + result[i];
			} else {
			}
		}
		return result;
	}

	public void runAllInQueue() {
		Runnable task;
		BlockingQueue<Runnable> queue = getQueue();
		while ((task = queue.peek()) != null) {
			if (queue.remove(task)) {
				task.run();
			}
		}
	}

	public void runNextInQueue() {
		BlockingQueue<Runnable> queue = getQueue();
		Runnable task = queue.peek();
		if (task != null && queue.remove(task)) {
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
