package org.openrdf.http.object.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.apache.http.HttpEntity;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.entity.ConsumingNHttpEntityTemplate;
import org.openrdf.http.object.util.ChannelUtil;
import org.openrdf.http.object.util.ReadableContentListener;

public class ConsumingHttpEntity extends ConsumingNHttpEntityTemplate implements
		HttpEntityChannel {
	private ReadableContentListener in;
	private ByteBuffer buf;

	public ConsumingHttpEntity(HttpEntity httpEntity, ReadableContentListener in) {
		super(httpEntity, in);
		this.in = in;
	}

	@Override
	public String toString() {
		return in.toString();
	}

	@Override
	public InputStream getContent() throws IOException,
			UnsupportedOperationException {
		return ChannelUtil.newInputStream(in);
	}

	@Override
	public void writeTo(OutputStream out) throws IOException,
			UnsupportedOperationException {
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

	public ReadableByteChannel getReadableByteChannel() throws IOException {
		return in;
	}

	public void produceContent(ContentEncoder encoder, IOControl ioctrl)
			throws IOException {
		if (buf == null) {
			buf = ByteBuffer.allocate(1024);
		}
		buf.clear();
		if (in.read(buf) < 0) {
			encoder.complete();
		} else {
			buf.flip();
			encoder.write(buf);
		}

	}

}
