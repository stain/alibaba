package org.openrdf.http.object.threads;

public interface ThreadPoolMXBean {

	String[] getQueueDescription();

	int getQueueSize();

	int getQueueRemainingCapacity();

	void clearQueue();

	void runNextInQueue();

	void runAllInQueue();

	boolean isContinueExistingPeriodicTasksAfterShutdownPolicy();

	void setContinueExistingPeriodicTasksAfterShutdownPolicy(boolean policy);

	boolean isExecuteExistingDelayedTasksAfterShutdownPolicy();

	void setExecuteExistingDelayedTasksAfterShutdownPolicy(boolean policy);

	void setAllowCoreThreadTimeOut(boolean allow);

	boolean isAllowsCoreThreadTimeOut();

	int getActiveCount();

	long getCompletedTaskCount();

	int getLargestPoolSize();

	int getPoolSize();

	long getTaskCount();

	boolean isShutdown();

	boolean isTerminated();

	boolean isTerminating();

	void startAllCoreThreads();

	void startCoreThread();

	void purge();

	int getCorePoolSize();

	void setCorePoolSize(int size);

	long getKeepAliveTime();

	void setKeepAliveTime(long seconds);

	int getMaximumPoolSize();

	void setMaximumPoolSize(int size);

	void shutdown();
}
