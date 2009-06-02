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
package org.openrdf.server.metadata.http.writers;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;


import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFHandlerException;

/**
 * Delegates to other {@link MessageBodyWriter}s.
 * 
 * @author James Leigh
 *
 */
public class AggregateWriter implements MessageBodyWriter<Object> {
	private List<MessageBodyWriter> writers = new ArrayList<MessageBodyWriter>();

	public AggregateWriter() {
		writers.add(new FileBodyWriter());
		writers.add(new BooleanMessageWriter());
		writers.add(new ModelMessageWriter());
		writers.add(new GraphMessageWriter());
		writers.add(new TupleMessageWriter());
		writers.add(new RDFObjectWriter());
		writers.add(new SetOfRDFObjectWriter());
		writers.add(new StringBodyWriter());
	}

	public String getContentType(Class<?> type, String mimeType, Charset charset) {
		return findWriter(type, mimeType).getContentType(type, mimeType, charset);
	}

	public long getSize(Object result, String mimeType) {
		return findWriter(result.getClass(), mimeType).getSize(result, mimeType);
	}

	public boolean isWriteable(Class<?> type, String mimeType) {
		return findWriter(type, mimeType) != null;
	}

	public void writeTo(Object result, String base, String mimeType,
			OutputStream out, Charset charset) throws IOException, RDFHandlerException,
			QueryEvaluationException, TupleQueryResultHandlerException,
			RepositoryException {
		MessageBodyWriter writer = findWriter(result.getClass(), mimeType);
		writer.writeTo(result, base, mimeType, out, charset);
	}

	private MessageBodyWriter findWriter(Class<?> type, String mimeType) {
		for (MessageBodyWriter w : writers) {
			if (w.isWriteable(type, mimeType)) {
				return w;
			}
		}
		return null;
	}

}
