/*
 * Copyright 2010, Zehperia LLC Some rights reserved.
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
package org.openrdf.http.object.filters;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.entity.ProducingNHttpEntity;
import org.openrdf.http.object.model.HttpEntityChannel;
import org.openrdf.http.object.util.ChannelUtil;

/**
 * Implements the ProducingNHttpEntity interface for subclasses.
 * 
 * @author James Leigh
 * 
 */
public class HttpEntityWrapper implements HttpEntityChannel {
	private HttpEntity entity;
	private ReadableByteChannel cin;
	private ByteBuffer buf = ByteBuffer.allocate(1024);

	public HttpEntityWrapper(HttpEntity entity) {
		this.entity = entity;
	}

	public ReadableByteChannel getReadableByteChannel() throws IOException {
		if (entity instanceof HttpEntityChannel)
			return ((HttpEntityChannel) entity).getReadableByteChannel();
		return ChannelUtil.newChannel(entity.getContent());
	}

	@Override
	public String toString() {
		return entity.toString();
	}

	public final void consumeContent() throws IOException {
		finish();
	}

	public InputStream getContent() throws IOException, IllegalStateException {
		return entity.getContent();
	}

	public Header getContentEncoding() {
		return entity.getContentEncoding();
	}

	public long getContentLength() {
		return entity.getContentLength();
	}

	public Header getContentType() {
		return entity.getContentType();
	}

	public boolean isChunked() {
		return entity.isChunked();
	}

	public final boolean isRepeatable() {
		return false;
	}

	public final boolean isStreaming() {
		return true;
	}

	public void writeTo(OutputStream out) throws IOException {
		InputStream in = getContent();
		try {
			int l;
			byte[] buf = new byte[2048];
			while ((l = in.read(buf)) != -1) {
				out.write(buf, 0, l);
			}
		} finally {
			in.close();
		}
	}

	public final void finish() throws IOException {
		try {
			if (entity instanceof ProducingNHttpEntity) {
				((ProducingNHttpEntity) entity).finish();
			} else {
				entity.consumeContent();
			}
		} finally {
			if (cin != null) {
				cin.close();
			}
		}
	}

	public final void produceContent(ContentEncoder encoder, IOControl ioctrl)
			throws IOException {
		if (cin == null) {
			cin = getReadableByteChannel();
		}
		buf.clear();
		if (cin.read(buf) < 0) {
			encoder.complete();
		} else {
			buf.flip();
			encoder.write(buf);
		}

	}

}
