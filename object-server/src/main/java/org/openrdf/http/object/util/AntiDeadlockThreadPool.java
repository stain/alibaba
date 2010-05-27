package org.openrdf.http.object.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AntiDeadlockThreadPool implements Executor {
	private static ScheduledExecutorService scheduler = SharedExecutors
			.getTimeoutThreadPool();
	private int corePoolSize;
	private int maximumPoolSize;
	private BlockingQueue<Runnable> queue;
	private ThreadPoolExecutor executor;
	private ScheduledFuture<?> schedule;

	public AntiDeadlockThreadPool(BlockingQueue<Runnable> queue,
			ThreadFactory threadFactory) {
		this(Runtime.getRuntime().availableProcessors() * 2 + 1, Runtime
				.getRuntime().availableProcessors() * 100, queue,
				threadFactory);
	}

	public AntiDeadlockThreadPool(int corePoolSize, int maximumPoolSize,
			BlockingQueue<Runnable> queue, ThreadFactory threadFactory) {
		this.queue = queue;
		this.corePoolSize = corePoolSize;
		this.maximumPoolSize = maximumPoolSize;
		executor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, 60L,
				TimeUnit.MINUTES, queue, threadFactory);
		executor.allowCoreThreadTimeOut(true);
	}

	public synchronized void execute(Runnable command) {
		executor.execute(command);
		if (corePoolSize >= maximumPoolSize)
			return;
		final Runnable top = queue.peek();
		if (schedule == null && top != null) {
			schedule = scheduler.scheduleWithFixedDelay(new Runnable() {
				private Runnable previous = top;

				public void run() {
					synchronized (AntiDeadlockThreadPool.this) {
						Runnable peek = queue.peek();
						if (peek == null || corePoolSize >= maximumPoolSize) {
							schedule.cancel(false);
							schedule = null;
						} else if (previous == peek) {
							executor.setCorePoolSize(++corePoolSize);
						} else {
							previous = peek;
						}
					}
				}
			}, 5, 5, TimeUnit.SECONDS);
		}
	}

}
