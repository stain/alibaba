/*
 * Copyright (c) 2009, Zepheira All rights reserved.
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
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Set;

import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.RDFObject;

/**
 * Writes RDF Object datatypes.
 * 
 * @author James Leigh
 * 
 */
public class DatatypeWriter implements MessageBodyWriter<Object> {
	private StringBodyWriter delegate = new StringBodyWriter();

	public long getSize(String mimeType, Class<?> type, Type genericType,
			ObjectFactory of, Object object, Charset charset) {
		String label = of.createLiteral(object).getLabel();
		return delegate.getSize(mimeType, String.class, String.class, of,
				label, charset);
	}

	public boolean isWriteable(String mimeType, Class<?> type,
			Type genericType, ObjectFactory of) {
		if (Set.class.equals(type))
			return false;
		if (Object.class.equals(type))
			return false;
		if (RDFObject.class.isAssignableFrom(type))
			return false;
		if (!delegate.isWriteable(mimeType, String.class, String.class, of))
			return false;
		if (of == null)
			return false;
		return of.isDatatype(type);
	}

	public String getContentType(String mimeType, Class<?> type,
			Type genericType, ObjectFactory of, Charset charset) {
		return delegate.getContentType(mimeType, String.class, String.class,
				of, charset);
	}

	public void writeTo(String mimeType, Class<?> type, Type genericType,
			ObjectFactory of, Object object, String base, Charset charset,
			OutputStream out, int bufSize) throws IOException {
		String label = of.createLiteral(object).getLabel();
		delegate.writeTo(mimeType, String.class, String.class, of, label, base,
				charset, out, bufSize);
	}

}
