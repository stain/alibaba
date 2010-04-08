package org.openrdf.http.object.tasks;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.Lock;

import org.openrdf.http.object.model.Filter;
import org.openrdf.http.object.model.Handler;
import org.openrdf.http.object.model.Request;
import org.openrdf.http.object.model.ResourceOperation;
import org.openrdf.http.object.model.Response;
import org.openrdf.http.object.util.FileLockManager;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessTask extends Task {
	private Logger logger = LoggerFactory.getLogger(ProcessTask.class);
	private ResourceOperation req;
	private FileLockManager locks;
	private Handler handler;
	private boolean content;

	public ProcessTask(Request request, Filter filter, ResourceOperation operation,
			FileLockManager locks, Handler handler) {
		super(request, filter);
		this.req = operation;
		this.locks = locks;
		this.handler = handler;
	}

	@Override
	public int getGeneration() {
		return 2;
	}

	public void perform() throws Exception {
		final Lock lock = createFileLock(req.getMethod(), req.getFile());
		try {
			Response resp = handler.handle(req);
			if (req.isSafe() || resp.getStatusCode() >= 400) {
				req.rollback();
			} else {
				req.commit();
			}
			if (resp.isContent() && !resp.isException()) {
				resp.onClose(new Runnable() {
					public void run() {
						try {
							req.close();
						} catch (IOException e) {
							logger.error(e.toString(), e);
						} catch (RepositoryException e) {
							logger.error(e.toString(), e);
						}
					}
				});
				content = true;
			}
			submitResponse(resp);
		} finally {
			if (lock != null) {
				lock.unlock();
			}
		}
	}

	@Override
	public void close() {
		super.close();
		try {
			if (!content) {
				req.close();
			}
		} catch (IOException e) {
			logger.error(e.toString(), e);
		} catch (RepositoryException e) {
			logger.error(e.toString(), e);
		}
	}

	private Lock createFileLock(String method, File file)
			throws InterruptedException {
		if (!method.equals("PUT") && (file == null || !file.exists()))
			return null;
		boolean shared = method.equals("GET") || method.equals("HEAD")
				|| method.equals("OPTIONS") || method.equals("TRACE")
				|| method.equals("POST") || method.equals("PROPFIND");
		return locks.lock(file, shared);
	}
}