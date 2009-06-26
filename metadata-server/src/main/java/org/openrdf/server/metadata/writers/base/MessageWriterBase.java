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
package org.openrdf.server.metadata.writers.base;

import info.aduna.lang.FileFormat;
import info.aduna.lang.service.FileFormatServiceRegistry;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.openrdf.OpenRDFException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.server.metadata.writers.MessageBodyWriter;

/**
 * Base class for writers that use a {@link FileFormat}.
 * 
 * @author James Leigh
 * 
 * @param <FF>
 *            file format
 * @param <S>
 *            reader factory
 * @param <T>
 *            Java type returned
 */
public abstract class MessageWriterBase<FF extends FileFormat, S, T> implements
		MessageBodyWriter<T> {
	private FileFormatServiceRegistry<FF, S> registry;
	private Class<T> type;

	public MessageWriterBase(FileFormatServiceRegistry<FF, S> registry,
			Class<T> type) {
		this.registry = registry;
		this.type = type;
	}

	public long getSize(String mimeType, Class<?> type, ObjectFactory of,
			T result) {
		return -1;
	}

	public boolean isWriteable(String mimeType, Class<?> type, ObjectFactory of) {
		if (!this.type.isAssignableFrom(type))
			return false;
		return getFactory(mimeType) != null;
	}

	public String getContentType(String mimeType, Class<?> type, ObjectFactory of, Charset charset) {
		FF format = getFormat(mimeType);
		String contentType = format.getDefaultMIMEType();
		if (format.hasCharset()) {
			if (charset == null) {
				charset = format.getCharset();
			}
			contentType += "; charset=" + charset.name();
		}
		return contentType;
	}

	public void writeTo(String mimeType, Class<?> type, ObjectFactory of,
			T result, String base, Charset charset, OutputStream out)
			throws IOException, OpenRDFException {
		S factory = getFactory(mimeType);
		writeTo(factory, result, out, charset, base);
	}

	public abstract void writeTo(S factory, T result, OutputStream out,
			Charset charset, String base) throws IOException,
			RDFHandlerException, QueryEvaluationException,
			TupleQueryResultHandlerException;

	protected S getFactory(String mimeType) {
		FF format = getFormat(mimeType);
		if (format == null)
			return null;
		return registry.get(format);
	}

	protected FF getFormat(String mimeType) {
		if (mimeType == null || mimeType.contains("*")
				|| "application/octet-stream".equals(mimeType)) {
			for (FF format : registry.getKeys()) {
				if (registry.get(format) != null)
					return format;
			}
			return null;
		}
		return registry.getFileFormatForMIMEType(mimeType);
	}
}
