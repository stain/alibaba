package org.openrdf.http.object.writers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeMultipart;

import org.openrdf.http.object.util.CatReadableByteChannel;
import org.openrdf.http.object.util.ChannelUtil;
import org.openrdf.http.object.util.MessageType;

public class MultipartWriter implements MessageBodyWriter<Multipart> {
	private static int part = 0;

	public boolean isWriteable(MessageType mtype) {
		String mimeType = mtype.getMimeType();
		if (!mtype.isUnknown() && !mtype.is(Multipart.class)
				&& !mtype.is(MimeMultipart.class))
			return false;
		return mimeType == null || mimeType.startsWith("*")
				|| mimeType.startsWith("multipart/");

	}

	public boolean isText(MessageType mtype) {
		return false;
	}

	public String getContentType(MessageType mtype, Charset charset) {
		String mimeType = mtype.getMimeType();
		String boundary = getUniqueBoundaryValue();
		if (mimeType == null || mimeType.contains("*"))
			return "multipart/mixed;boundary=\"" + boundary + "\"";
		if (mimeType.contains("boundary="))
			return mimeType;
		return mimeType + ";boundary=\"" + boundary + "\"";
	}

	public long getSize(MessageType mtype, Multipart result, Charset charset) {
		return -1;
	}

	public ReadableByteChannel write(MessageType mtype, Multipart result,
			String base, Charset charset) throws MessagingException,
			IOException {
		CatReadableByteChannel cat = new CatReadableByteChannel();
		ContentType type = new ContentType(mtype.getMimeType());
		String boundary = type.getParameter("boundary");
		if (boundary == null) {
			type = new ContentType(result.getContentType());
			boundary = type.getParameter("boundary");
		}
		if (boundary == null) {
			type = new ContentType(result.getContentType());
			boundary = type.getParameter("boundary");
		}
		if (boundary == null) {
			boundary = getUniqueBoundaryValue();
		}
		boundary = "--" + boundary;

		for (int i = 0, n = result.getCount(); i < n; i++) {
			cat.println(boundary);
			BodyPart bodyPart = result.getBodyPart(i);
			int size = bodyPart.getSize();
			ByteArrayOutputStream out = new ByteArrayOutputStream(
					size > 0 ? size : 32);
			bodyPart.writeTo(out);
			cat.append(ChannelUtil.newChannel(out.toByteArray()));
			cat.println();
		}

		cat.println(boundary + "--");
		return cat;
	}

	private String getUniqueBoundaryValue() {
		StringBuffer s = new StringBuffer();
		// Unique string is --Part_<part>_<hashcode>.<currentTime>
		s.append("--Part_").append(part++).append("_").append(s.hashCode())
				.append('.').append(System.currentTimeMillis());
		return s.toString();
	}

}
