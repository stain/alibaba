/*
 * Copyright (c) 2009, James Leigh All rights reserved.
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
package org.openrdf.http.object.traits;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.openrdf.repository.RepositoryException;

/**
 * A common set of services all realms must implement.
 */
public interface Realm {
	public static Set<String> OPERATIONS = new HashSet<String>(Arrays.asList(
			"allow-origin", "unauthorized", "authorize"));

	/**
	 * The set of URL prefixes that this realm protects.
	 * 
	 * @return a space separated list of URL prefixes or path prefixes or null
	 *         for all request targets.
	 */
	String protectionDomain();

	/**
	 * The code origins that are permitted to send requests to this realm as
	 * defined in the HTTP header Access-Control-Allow-Origin.
	 * 
	 * @return a comma separated list of acceptable domains or "*" if any domain
	 *         is allowed or null if no scripts are allowed.
	 */
	String allowOrigin();

	/**
	 * Called to authenticate requests that have no authorization header. Checks
	 * if a request method needs further authentication in this realm.
	 * 
	 * @param method
	 *            The HTTP request method.
	 * @param via
	 *            List of hosts or pseudonym that sent this request. Including
	 *            the HTTP Via header entries.
	 * @return The authorisation credentials or a null result if unauthorised.
	 */
	Object authenticateAgent(String method, String via, Set<String> names,
			String algorithm, byte[] encoded) throws RepositoryException;

	/**
	 * Called when the request includes an authorization header.
	 * 
	 * @param method
	 *            The HTTP request method.
	 * @param resource
	 *            The target uri for this request.
	 * @param request
	 *            A map with "request-target" that was used in the request line,
	 *            "content-md5" that is the base64 of 128 bit MD5 digest as per
	 *            RFC1864 if a request body was sent, "authorization" that is
	 *            the HTTP request header of the same name, "via" that is a list
	 *            of hosts or pseudonym that sent this request. Including the
	 *            HTTP Via header entries.
	 * @return The authenticated credentials or a null result if invalid
	 *         credentials.
	 */
	Object authenticateRequest(String method, Object resource,
			Map<String, String[]> request) throws RepositoryException;

	/**
	 * The response that should be returned when the request could not be
	 * authenticated.
	 * 
	 * @return An HTTP response
	 */
	HttpResponse unauthorized() throws IOException;

	/**
	 * Called after a request has been authenticate.
	 * 
	 * @param credential
	 *            Response from authenticateAgent or authenticateRequest.
	 * @param method
	 *            The HTTP request method.
	 * @param resource
	 *            The target uri for this request.
	 * @param qs
	 *            Any query parameters passed in the request.
	 * @return <code>true</code> if the credentials are authorized on this
	 *         resource
	 */
	boolean authorizeCredential(Object credential, String method,
			Object resource, String qs);

	/**
	 * The response that should be returned when the request is authenticated,
	 * but could not be authorised or the request originated from an invalid
	 * origin.
	 * 
	 * @return An HTTP response
	 */
	HttpResponse forbidden() throws IOException;

}
