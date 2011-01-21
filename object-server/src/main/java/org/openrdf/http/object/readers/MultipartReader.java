package org.openrdf.http.object.readers;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMultipart;

import org.openrdf.http.object.util.ChannelUtil;
import org.openrdf.http.object.util.MessageType;

public class MultipartReader implements MessageBodyReader<MimeMultipart> {

	public boolean isReadable(MessageType mtype) {
		String mimeType = mtype.getMimeType();
		if (!mtype.isUnknown() && !mtype.is(Multipart.class) && !mtype.is(MimeMultipart.class))
			return false;
		return mimeType == null || mimeType.startsWith("multipart/");
	}

	public MimeMultipart readFrom(MessageType mtype, ReadableByteChannel in,
			Charset charset, String base, String location) throws IOException,
			MessagingException {
		String mimeType = mtype.getMimeType();
		if (mimeType == null) {
			mimeType = "multipart/mixed";
		}
		InputStream sin = ChannelUtil.newInputStream(in);
		return new CloseableMimeMultipart(sin, mimeType);
	}

	/**
	 * This class delays parsing the input stream until it is needed. This
	 * allows a remote TCP socket to buffer the input before it is read.
	 * 
	 * @author James Leigh
	 * 
	 */
	private class CloseableMimeMultipart extends MimeMultipart implements
			Closeable {
		private InputStream in;

		public CloseableMimeMultipart(final InputStream in,
				final String mimeType) throws MessagingException {
			super(new DataSource() {
				public OutputStream getOutputStream() throws IOException {
					throw new UnsupportedOperationException();
				}

				public String getName() {
					return mimeType;
				}

				public InputStream getInputStream() throws IOException {
					return in;
				}

				public String getContentType() {
					return mimeType;
				}
			});
			this.in = in;
		}

		public void close() throws IOException {
			in.close();
		}
	}
}
