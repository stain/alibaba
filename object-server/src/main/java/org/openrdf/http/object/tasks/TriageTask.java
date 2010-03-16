package org.openrdf.http.object.tasks;

import java.io.File;

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

	void perform() throws Exception {
		HttpResponse resp = filter.intercept(req);
		if (resp == null) {
			ResourceOperation op = new ResourceOperation(dataDir, req,
					repository);
			bear(new VerifyTask(req, filter, op, locks, handler));
		} else {
			submitResponse(resp);
		}
	}
}