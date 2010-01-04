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
package org.openrdf.http.object.writers.base;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.openrdf.OpenRDFException;
import org.openrdf.http.object.util.GenericType;
import org.openrdf.http.object.writers.MessageBodyWriter;
import org.openrdf.http.object.writers.StringBodyWriter;
import org.openrdf.repository.object.ObjectFactory;

/**
 * Writes text/uri-list files.
 */
public class URIListWriter<URI> implements MessageBodyWriter<URI> {
	private static final Charset UTF8 = Charset.forName("UTF-8");
	private StringBodyWriter delegate = new StringBodyWriter();
	private Class<URI> componentType;

	public URIListWriter(Class<URI> componentType) {
		this.componentType = componentType;
	}

	public boolean isWriteable(String mimeType, Class<?> ctype, Type gtype,
			ObjectFactory of) {
		if (componentType != null) {
			GenericType<?> type = new GenericType(ctype, gtype);
			if (type.isSetOrArray()) {
				Class<?> component = type.getComponentClass();
				if (!componentType.isAssignableFrom(component)
						&& !component.equals(Object.class))
					return false;
			} else if (!componentType.isAssignableFrom(ctype)) {
				return false;
			}
		}
		Class<String> t = String.class;
		return delegate.isWriteable(mimeType, t, t, of);
	}

	public String getContentType(String mimeType, Class<?> type,
			Type genericType, ObjectFactory of, Charset charset) {
		if (mimeType == null || mimeType.startsWith("*")
				|| mimeType.startsWith("text/*")) {
			mimeType = "text/uri-list";
		}
		Class<String> t = String.class;
		return delegate.getContentType(mimeType, t, t, of, charset);
	}

	public long getSize(String mimeType, Class<?> type, Type genericType,
			ObjectFactory of, URI result, Charset charset) {
		if (result == null)
			return 0;
		if (Set.class.equals(type))
			return -1;
		if (mimeType == null || mimeType.startsWith("*")
				|| mimeType.startsWith("text/*")) {
			mimeType = "text/uri-list";
		}
		Class<String> t = String.class;
		return delegate.getSize(mimeType, t, t, of, toString(result), charset);
	}

	public void writeTo(String mimeType, Class<?> ctype, Type gtype,
			ObjectFactory of, URI result, String base, Charset charset,
			OutputStream out, int bufSize) throws IOException,
			OpenRDFException, XMLStreamException, TransformerException,
			ParserConfigurationException {
		if (result == null)
			return;
		GenericType<?> type = new GenericType(ctype, gtype);
		if (mimeType == null || mimeType.startsWith("*")
				|| mimeType.startsWith("text/*")) {
			mimeType = "text/uri-list";
		}
		if (type.isSetOrArray()) {// TODO or array
			if (charset == null) {
				charset = UTF8;
			}
			Writer writer = new OutputStreamWriter(out, charset);
			Iterator<URI> iter = (Iterator<URI>) type.iteratorOf(result);
			while (iter.hasNext()) {
				writer.write(toString(iter.next()));
				if (iter.hasNext()) {
					writer.write("\r\n");
				}
			}
			writer.flush();
		} else {
			Class<String> t = String.class;
			delegate.writeTo(mimeType, t, t, of, toString(result), base,
					charset, out, bufSize);
		}
	}

	protected String toString(URI result) {
		return result.toString();
	}

}
