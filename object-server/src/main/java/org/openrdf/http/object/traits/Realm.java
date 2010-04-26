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
import org.openrdf.http.object.annotations.cacheControl;
import org.openrdf.http.object.annotations.operation;
import org.openrdf.http.object.annotations.parameter;
import org.openrdf.http.object.annotations.type;

/**
 * A common set of services all realms must implement.
 */
public interface Realm {
	public static Set<String> OPERATIONS = new HashSet<String>(Arrays.asList(
			"allow-origin", "unauthorized", "authorize"));

	/**
	 * The code origins that are permitted to send requests to this realm as
	 * defined in the HTTP header Access-Control-Allow-Origin.
	 * 
	 * @return a comma separated list of acceptable domains or null if any
	 *         domain is allowed.
	 */
	@operation("allow-origin")
	String allowOrigin();

	/**
	 * The response that should be returned when the request could not be
	 * authorised.
	 * 
	 * @return An HTTP response
	 */
	@cacheControl("no-store")
	@type("message/http")
	@operation("unauthorized")
	HttpResponse unauthorized() throws IOException;

	/**
	 * Called to authorise requests that have no authorization header.
	 * 
	 * Checks if a request method needs further authorisation in this realm.
	 * 
	 * @param via
	 *            List of hosts or pseudonym that sent this request. Including
	 *            the HTTP Via header entries.
	 * @param method
	 *            The HTTP request method.
	 * @return <code>true</code> if these requests are permitted without further
	 *         authorisation.
	 */
	@operation("authorizeAgent")
	boolean authorizeAgent(@parameter("via") String[] via,
			@parameter("name") Set<String> names,
			@parameter("algorithm") String algorithm,
			@parameter("encoded") byte[] encoded,
			@parameter("method") String method);

	/**
	 * Called when the request includes an authorization header.
	 * 
	 * @param via
	 *            List of hosts or pseudonym that sent this request. Including
	 *            the HTTP Via header entries.
	 * @param method
	 *            The HTTP request method.
	 * @param authorization
	 *            A map with "request-target" that was used in the request line,
	 *            "request-uri" that includes just the scheme, authority and
	 *            path, "content-md5" that is the base64 of 128 bit MD5 digest
	 *            as per RFC1864 if a request body was sent, "authorization"
	 *            that is the HTTP request header of the same name.
	 * @return <code>true</code> if this request is permitted.
	 */
	@operation("authorizeRequest")
	boolean authorizeRequest(@parameter("via") String[] via,
			@parameter("name") Set<String> names,
			@parameter("algorithm") String algorithm,
			@parameter("encoded") byte[] encoded,
			@parameter("method") String method,
			Map<String, String> authorization);

}
