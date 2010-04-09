package org.openrdf.http.object.client;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

public interface HTTPService {

	/**
	 * {@link HttpEntity#consumeContent()} or
	 * {@link HttpEntity#writeTo(java.io.OutputStream)} must be called if
	 * {@link HttpResponse#getEntity()} is non-null.
	 */
	HttpResponse service(HttpRequest request) throws IOException;
}
