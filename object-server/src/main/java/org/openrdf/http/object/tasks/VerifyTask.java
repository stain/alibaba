package org.openrdf.http.object.tasks;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openrdf.http.object.model.Filter;
import org.openrdf.http.object.model.Handler;
import org.openrdf.http.object.model.Request;
import org.openrdf.http.object.model.ResourceOperation;
import org.openrdf.http.object.model.Response;
import org.openrdf.http.object.util.FileLockManager;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VerifyTask extends Task {
	private Logger logger = LoggerFactory.getLogger(VerifyTask.class);
	private Request req;
	private Filter filter;
	private ResourceOperation op;
	private FileLockManager locks;
	private Handler handler;
	private CountDownLatch latch = new CountDownLatch(1);

	public VerifyTask(Request request, Filter filter,
			ResourceOperation operation, FileLockManager locks, Handler handler) {
		super(request, filter);
		this.req = request;
		this.filter = filter;
		this.op = operation;
		this.locks = locks;
		this.handler = handler;
	}

	@Override
	public int getGeneration() {
		return 1;
	}

	public void perform() throws Exception {
		op.init();
		Response resp = handler.verify(op);
		if (resp == null) {
			verified();
			bear(new ProcessTask(req, filter, op, locks, handler));
		} else {
			submitResponse(resp);
		}
	}

	public void verified() {
		latch.countDown();
	}

	public void awaitVerification(long time, TimeUnit unit) throws InterruptedException {
		latch.await(time, unit);
	}

	@Override
	public void close() {
		latch.countDown();
		super.close();
		try {
			op.close();
		} catch (RepositoryException e) {
			logger.error(e.toString(), e);
		}
	}
}