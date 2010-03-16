package org.openrdf.http.object.tasks;

import java.io.File;
import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.openrdf.http.object.model.Filter;
import org.openrdf.http.object.model.Handler;
import org.openrdf.http.object.model.Request;
import org.openrdf.http.object.util.FileLockManager;
import org.openrdf.http.object.util.NamedThreadFactory;
import org.openrdf.repository.object.ObjectRepository;

public class TaskFactory {

	static final Executor executor;
	static {
		int n = Runtime.getRuntime().availableProcessors();
		Comparator<Runnable> cmp = new Comparator<Runnable>() {
			public int compare(Runnable o1, Runnable o2) {
				if (!(o1 instanceof Task) || !(o2 instanceof Task))
					return 0;
				Task t1 = (Task) o1;
				Task t2 = (Task) o2;
				if (t1.getGeneration() < t2.getGeneration())
					return -1;
				if (t1.getGeneration() > t2.getGeneration())
					return 1;
				if (t1.isSafe() && !t2.isSafe())
					return -1;
				if (!t1.isSafe() && t2.isSafe())
					return 1;
				if (t1.isStorable() && !t2.isStorable())
					return -1;
				if (!t1.isStorable() && t2.isStorable())
					return 1;
				if (t1.getReceivedOn() < t2.getReceivedOn())
					return -1;
				if (t1.getReceivedOn() > t2.getReceivedOn())
					return 1;
				return System.identityHashCode(t1)
						- System.identityHashCode(t2);
			};
		};
		BlockingQueue<Runnable> queue = new PriorityBlockingQueue<Runnable>(
				n * 10, cmp);
		executor = new ThreadPoolExecutor(n, n * 5, 60L, TimeUnit.SECONDS,
				queue, new NamedThreadFactory("HTTP Handler"));
	}

	private File dataDir;
	private ObjectRepository repo;
	private Filter filter;
	private FileLockManager locks = new FileLockManager();
	private Handler handler;

	public TaskFactory(File dataDir, ObjectRepository repository,
			Filter filter, Handler handler) {
		this.dataDir = dataDir;
		this.repo = repository;
		this.filter = filter;
		this.handler = handler;
	}

	public Task createTask(Request req) {
		Task task = new TriageTask(dataDir, repo, req, filter, locks, handler);
		executor.execute(task);
		return task;
	}

}
