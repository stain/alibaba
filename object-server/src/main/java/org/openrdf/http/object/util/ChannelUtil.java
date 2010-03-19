package org.openrdf.http.object.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.security.MessageDigest;

public final class ChannelUtil {

	public static ReadableByteChannel newChannel(InputStream in) {
		if (in == null)
			return null;
		if (in instanceof ChannelInputStream)
			return ((ChannelInputStream) in).getChannel();
		return Channels.newChannel(in);
	}

	public static WritableByteChannel newChannel(OutputStream out) {
		if (out == null)
			return null;
		return Channels.newChannel(out);
	}

	public static ReadableByteChannel newChannel(byte[] bytes) {
		if (bytes == null)
			return null;
		return Channels.newChannel(new ByteArrayInputStream(bytes));
	}

	public static InputStream newInputStream(ReadableByteChannel ch) {
		if (ch == null)
			return null;
		return new ChannelInputStream(ch);
	}

	public static OutputStream newOutputStream(WritableByteChannel ch) {
		if (ch == null)
			return null;
		return Channels.newOutputStream(ch);
	}

	public static BufferedReader newReader(ReadableByteChannel ch, Charset cs) {
		if (ch == null)
			return null;
		return new BufferedReader(Channels.newReader(ch, cs.newDecoder(), -1));
	}

	public static Writer newWriter(WritableByteChannel ch, Charset cs) {
		if (ch == null)
			return null;
		return Channels.newWriter(ch, cs.newEncoder(), -1);
	}

	public static long transfer(InputStream in, OutputStream out)
			throws IOException {
		return transfer(newChannel(in), newChannel(out), null);
	}

	public static long transfer(InputStream in, WritableByteChannel out)
			throws IOException {
		return transfer(newChannel(in), out, null);
	}

	public static long transfer(ReadableByteChannel in, OutputStream out)
			throws IOException {
		return transfer(in, newChannel(out), null);
	}

	public static long transfer(ReadableByteChannel in, WritableByteChannel out)
			throws IOException {
		return transfer(in, out, null);
	}

	public static long transfer(InputStream in, OutputStream out,
			MessageDigest digest) throws IOException {
		return transfer(newChannel(in), out, digest);
	}

	public static long transfer(InputStream in, WritableByteChannel out,
			MessageDigest digest) throws IOException {
		return transfer(newChannel(in), out, digest);
	}

	public static long transfer(ReadableByteChannel in, OutputStream out,
			MessageDigest digest) throws IOException {
		return transfer(in, newChannel(out), digest);
	}

	public static long transfer(ReadableByteChannel in,
			WritableByteChannel out, MessageDigest digest) throws IOException {
		if (digest == null && in instanceof FileChannel) {
			return ((FileChannel) in).transferTo(0, Long.MAX_VALUE, out);
		} else if (digest == null && out instanceof FileChannel) {
			return ((FileChannel) out).transferFrom(in, 0, Long.MAX_VALUE);
		} else {
			long read = 0;
			ByteBuffer buf = ByteBuffer.allocate(1024 * 8);
			buf.clear();
			while (in.read(buf) >= 0 || buf.position() != 0) {
				buf.flip();
				int len = out.write(buf);
				if (digest != null) {
					digest.update(buf.array(), buf.arrayOffset(), len);
				}
				read += len;
				buf.compact();
			}
			return read;
		}
	}

	private static class ChannelInputStream extends FilterInputStream {
		private ReadableByteChannel ch;

		protected ChannelInputStream(ReadableByteChannel ch) {
			super(Channels.newInputStream(ch));
			this.ch = ch;
		}

		public ReadableByteChannel getChannel() {
			return ch;
		}
	}

	private ChannelUtil() {
	}
}
