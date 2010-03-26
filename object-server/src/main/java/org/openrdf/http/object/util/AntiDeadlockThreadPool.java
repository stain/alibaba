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
			.getIdleThreadPool();
	private BlockingQueue<Runnable> queue;
	private ThreadPoolExecutor executor;
	private ScheduledFuture<?> schedule;

	public AntiDeadlockThreadPool(BlockingQueue<Runnable> queue,
			ThreadFactory threadFactory) {
		this.queue = queue;
		int n = Runtime.getRuntime().availableProcessors();
		executor = new ThreadPoolExecutor(n, Integer.MAX_VALUE, 60L,
				TimeUnit.MINUTES, queue, threadFactory);
		executor.allowCoreThreadTimeOut(true);
	}

	public synchronized void execute(Runnable command) {
		executor.execute(command);
		final Runnable top = queue.peek();
		if (schedule == null && top != null) {
			schedule = scheduler.scheduleAtFixedRate(new Runnable() {
				private Runnable previous = top;

				public void run() {
					synchronized (AntiDeadlockThreadPool.this) {
						Runnable peek = queue.peek();
						if (peek == null) {
							schedule.cancel(false);
							schedule = null;
						} else if (previous == peek) {
							executor
									.setCorePoolSize(executor.getCorePoolSize() + 1);
						} else {
							previous = peek;
						}
					}
				}
			}, 5, 10, TimeUnit.SECONDS);
		}
	}

}
