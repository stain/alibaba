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
package org.openrdf.server.metadata.writers;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.openrdf.OpenRDFException;
import org.openrdf.model.Model;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.impl.GraphQueryResultImpl;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriterFactory;

/**
 * Writes RDF from a {@link Model}.
 * 
 * @author James Leigh
 * 
 */
public class ModelMessageWriter implements MessageBodyWriter<Model> {
	private GraphMessageWriter delegate;

	public ModelMessageWriter() {
		delegate = new GraphMessageWriter();
	}

	public String getContentType(String mimeType, Class<?> type,
			ObjectFactory of, Charset charset) {
		return delegate.getContentType(mimeType, GraphQueryResult.class, of,
				charset);
	}

	public long getSize(String mimeType, Class<?> type, ObjectFactory of,
			Model result, Charset charset) {
		return -1;
	}

	public boolean isWriteable(String mimeType, Class<?> type, ObjectFactory of) {
		if (!Model.class.isAssignableFrom(type))
			return false;
		return delegate.isWriteable(mimeType, GraphQueryResult.class, of);
	}

	public String toString() {
		return delegate.toString();
	}

	public void writeTo(RDFWriterFactory factory, Model model,
			OutputStream out, Charset charset, String base)
			throws RDFHandlerException, QueryEvaluationException {
		GraphQueryResult result = new GraphQueryResultImpl(model
				.getNamespaces(), model);
		delegate.writeTo(factory, result, out, charset, base);
	}

	public void writeTo(String mimeType, Class<?> type, ObjectFactory of,
			Model model, String base, Charset charset, OutputStream out,
			int bufSize) throws IOException, OpenRDFException {
		GraphQueryResult result = new GraphQueryResultImpl(model
				.getNamespaces(), model);
		delegate.writeTo(mimeType, type, of, result, base, charset, out,
				bufSize);
	}
}
