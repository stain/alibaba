/*
 * Copyright (c) 2010, Zepheira LLC, Some rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.openrdf.http.object.tasks;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.Lock;

import org.openrdf.http.object.exceptions.Conflict;
import org.openrdf.http.object.model.Filter;
import org.openrdf.http.object.model.Handler;
import org.openrdf.http.object.model.Request;
import org.openrdf.http.object.model.ResourceOperation;
import org.openrdf.http.object.model.Response;
import org.openrdf.http.object.util.FileLockManager;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Invokes the operation and submits the response when this task is run.
 * 
 * @author James Leigh
 * 
 */
public class ProcessTask extends Task {
	private Logger logger = LoggerFactory.getLogger(ProcessTask.class);
	private Request request;
	private Filter filter;
	private ResourceOperation req;
	private FileLockManager locks;
	private Handler handler;
	private boolean content;
	private int max;

	public ProcessTask(Request request, Filter filter,
			ResourceOperation operation, FileLockManager locks, Handler handler) {
		this(request, filter, operation, locks, handler, 20);
	}

	public ProcessTask(Request request, Filter filter,
			ResourceOperation operation, FileLockManager locks,
			Handler handler, int max) {
		super(request, filter);
		this.request = request;
		this.filter = filter;
		this.req = operation;
		this.locks = locks;
		this.handler = handler;
		this.max = max;
	}

	@Override
	public int getGeneration() {
		return 2;
	}

	public void perform() throws Exception {
		String method = req.getMethod();
		File file = req.getFile();
		Lock lock = null;
		if (method.equals("PUT") || file != null && file.exists()) {
			boolean shared = method.equals("GET") || method.equals("HEAD")
					|| method.equals("OPTIONS") || method.equals("TRACE")
					|| method.equals("POST") || method.equals("PROPFIND");
			if (shared) {
				lock = locks.lock(file, shared);
			} else {
				lock = locks.tryLock(file, shared);
				if (lock == null && max < 0) {
					req.close();
					submitResponse(new Response().exception(new Conflict()));
					return;
				} else if (lock == null) {
					bear(new ProcessTask(request, filter, req, locks, handler,
							max - 1));
					return;
				}
			}
		}
		try {
			Response resp = handler.handle(req);
			if (req.isSafe() || resp.getStatusCode() >= 400) {
				req.rollback();
			} else {
				req.commit();
			}
			if (resp.isContent() && !resp.isException()) {
				content = true;
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
				submitResponse(resp);
			} else {
				submitResponse(resp);
			}
		} finally {
			if (lock != null) {
				lock.unlock();
			}
		}
	}

	@Override
	public void abort() {
		super.abort();
		try {
			if (content) {
				req.close();
			}
		} catch (IOException e) {
			logger.error(e.toString(), e);
		} catch (RepositoryException e) {
			logger.error(e.toString(), e);
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
}
