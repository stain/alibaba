package org.openrdf.http.object.client;

import org.apache.http.HttpEntity;
import org.openrdf.http.object.filters.HttpEntityWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrackedHttpEntity extends HttpEntityWrapper {
	private static Logger logger = LoggerFactory
			.getLogger(TrackedHttpEntity.class);
	private Throwable source;

	public TrackedHttpEntity(HttpEntity entity, Throwable source) {
		super(entity);
		this.source = source;
	}

	@Override
	public HttpEntity getEntityDelegate() {
		if (source == null && logger.isDebugEnabled()) {
			source = new IllegalStateException();
		}
		return super.getEntityDelegate();
	}

	@Override
	protected void finalize() throws Throwable {
		if (isStreaming()) {
			finish();
			if (source == null) {
				logger.error("HttpEntity#consumeContent() must be called");
			} else {
				logger.error("HttpEntity#consumeContent() was not called", source);
			}
		}
		super.finalize();
	}
}