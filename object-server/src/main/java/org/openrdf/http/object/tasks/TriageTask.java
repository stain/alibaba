package org.openrdf.http.object.tasks;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import org.apache.http.HttpResponse;
import org.openrdf.http.object.model.Filter;
import org.openrdf.http.object.model.Handler;
import org.openrdf.http.object.model.Request;
import org.openrdf.http.object.model.ResourceOperation;
import org.openrdf.http.object.util.FileLockManager;
import org.openrdf.repository.object.ObjectRepository;

public final class TriageTask extends Task {
	private Request req;
	private Filter filter;
	private FileLockManager locks;
	private Handler handler;
	private File dataDir;
	private ObjectRepository repository;
	private CountDownLatch latch = new CountDownLatch(1);

	public TriageTask(File dataDir, ObjectRepository repository,
			Request request, Filter filter, FileLockManager locks,
			Handler handler) {
		super(request, filter);
		this.dataDir = dataDir;
		this.repository = repository;
		this.req = request;
		this.filter = filter;
		this.locks = locks;
		this.handler = handler;
	}

	@Override
	public int getGeneration() {
		return 0;
	}

	public void awaitVerification() throws InterruptedException {
		latch.await();
		super.awaitVerification();
	}

	@Override
	public void close() {
		latch.countDown();
		super.close();
	}

	void perform() throws Exception {
		req = filter.filter(req);
		HttpResponse resp = filter.intercept(req);
		if (resp == null) {
			ResourceOperation op = new ResourceOperation(dataDir, req,
					repository);
			bear(new VerifyTask(req, filter, op, locks, handler));
			latch.countDown();
		} else {
			submitResponse(resp);
		}
	}
}