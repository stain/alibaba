/*
 * Copyright 2009-2010, James Leigh and Zepheira LLC Some rights reserved.
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
package org.openrdf.http.object.writers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.openrdf.OpenRDFException;
import org.openrdf.http.object.util.ChannelUtil;
import org.openrdf.http.object.util.GenericType;
import org.openrdf.repository.object.ObjectFactory;

/**
 * Writes a percent encoded form from a {@link Map}.
 * 
 * @author James Leigh
 * 
 */
public class FormMapMessageWriter implements
		MessageBodyWriter<Map<String, Object>> {
	private MessageBodyWriter delegate = AggregateWriter.getInstance();

	public boolean isText(String mimeType, Class<?> type, Type genericType,
			ObjectFactory of) {
		return true;
	}

	public boolean isWriteable(String mimeType, Class<?> ctype, Type gtype,
			ObjectFactory of) {
		GenericType<?> type = new GenericType(ctype, gtype);
		if (!type.isMap())
			return false;
		GenericType kt = type.getKeyGenericType();
		if (!kt.isUnknown()) {
			if (!delegate.isWriteable("text/plain", kt.clas(), kt.type(), of))
				return false;
		}
		GenericType vt = type.getComponentGenericType();
		if (vt.isSetOrArray()) {
			Class<?> vvc = vt.getComponentClass();
			Type vvt = vt.getComponentType();
			if (!delegate.isWriteable("text/plain", vvc, vvt, of))
				return false;
		} else if (!vt.isUnknown()) {
			if (!delegate.isWriteable("text/plain", vt.clas(), vt.type(), of))
				return false;
		}
		return mimeType == null || mimeType.startsWith("*")
				|| mimeType.startsWith("application/*")
				|| mimeType.startsWith("application/x-www-form-urlencoded");
	}

	public long getSize(String mimeType, Class<?> type, Type genericType,
			ObjectFactory of, Map<String, Object> map, Charset charset) {
		return -1;
	}

	public String getContentType(String mimeType, Class<?> type,
			Type genericType, ObjectFactory of, Charset charset) {
		return "application/x-www-form-urlencoded";
	}

	public ReadableByteChannel write(final String mimeType,
			final Class<?> type, final Type genericType,
			final ObjectFactory of, final Map<String, Object> result,
			final String base, final Charset charset) throws IOException,
			OpenRDFException, XMLStreamException, TransformerException,
			ParserConfigurationException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		writeTo(mimeType, type, genericType, of, result, base, charset, out,
				1024);
		return ChannelUtil.newChannel(out.toByteArray());
	}

	public void writeTo(String mimeType, Class<?> ctype, Type gtype,
			ObjectFactory of, Map<String, Object> result, String base,
			Charset charset, OutputStream out, int bufSize) throws IOException,
			OpenRDFException, XMLStreamException, TransformerException,
			ParserConfigurationException {
		if (charset == null) {
			charset = Charset.forName("ISO-8859-1");
		}
		GenericType<?> type = new GenericType(ctype, gtype);
		Class<?> kc = type.getKeyClass();
		Type kct = type.getKeyType();
		Class<?> vc = type.getComponentClass();
		Type vct = type.getComponentType();
		GenericType<?> vtype = new GenericType(vc, vct);
		if (vtype.isUnknown()) {
			vct = vc = String[].class;
			vtype = new GenericType(vc, vct);
		}
		if (vtype.isSetOrArray()) {
			vc = vtype.getComponentClass();
			vct = vtype.getComponentType();
		}
		Writer writer = new OutputStreamWriter(out, charset);
		try {
			if (result == null)
				return;
			boolean first = true;
			for (Map.Entry<String, Object> e : result.entrySet()) {
				if (e.getKey() != null) {
					String name = enc(writeTo(kc, kct, of, e.getKey(), base));
					Iterator<?> iter = vtype.iteratorOf(e.getValue());
					if (first) {
						first = false;
					} else {
						writer.append("&");
					}
					if (!iter.hasNext()) {
						writer.append(name);
					}
					while (iter.hasNext()) {
						writer.append("&").append(name);
						Object value = iter.next();
						if (value != null) {
							String str = writeTo(vc, vct, of, value, base);
							writer.append("=").append(enc(str));
						}
					}
				}
			}
		} finally {
			writer.close();
		}
	}

	private String enc(String value) throws UnsupportedEncodingException {
		return URLEncoder.encode(value, "UTF-8");
	}

	private String writeTo(Class<?> ctype, Type gtype, ObjectFactory of,
			Object value, String base) throws IOException, OpenRDFException,
			XMLStreamException, TransformerException,
			ParserConfigurationException {
		String txt = "text/plain";
		Charset cs = Charset.forName("ISO-8859-1");
		if (Object.class.equals(ctype) && value != null) {
			gtype = ctype = value.getClass();
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ReadableByteChannel in = delegate.write(txt, ctype, gtype, of, value,
				base, cs);
		try {
			ChannelUtil.transfer(in, out);
		} finally {
			in.close();
		}
		return out.toString("ISO-8859-1");
	}
}
