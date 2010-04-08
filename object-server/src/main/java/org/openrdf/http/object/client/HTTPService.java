package org.openrdf.http.object.client;

import java.io.IOException;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

public interface HTTPService {

	/**
	 * {@link HttpEntity#consumeContent()} must be called if
	 * {@link HttpResponse#getEntity()} is non-null (even if writeTo is called).
	 */
	HttpResponse service(HttpRequest request) throws IOException;
}
