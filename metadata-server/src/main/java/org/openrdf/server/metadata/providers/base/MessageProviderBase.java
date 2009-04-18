package org.openrdf.server.metadata.providers.base;

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.MediaType.WILDCARD_TYPE;
import info.aduna.lang.FileFormat;
import info.aduna.lang.service.FileFormatServiceRegistry;

import java.nio.charset.Charset;

import javax.ws.rs.core.MediaType;

public abstract class MessageProviderBase<FF extends FileFormat, S> {
	private FileFormatServiceRegistry<FF, S> registry;

	public MessageProviderBase(FileFormatServiceRegistry<FF, S> registry) {
		this.registry = registry;
	}

	protected S getFactory(MediaType media) {
		FF format = getFormat(media);
		if (format == null)
			return null;
		return registry.get(format);
	}

	protected FF getFormat(MediaType media) {
		if (media == null || WILDCARD_TYPE.equals(media)
				|| APPLICATION_OCTET_STREAM_TYPE.equals(media)) {
			for (FF format : registry.getKeys()) {
				if (registry.get(format) != null)
					return format;
			}
			return null;
		}
		// FIXME FileFormat does not understand MIME parameters
		String mimeType = media.getType() + "/" + media.getSubtype();
		return registry.getFileFormatForMIMEType(mimeType);
	}

	protected Charset getCharset(MediaType m, Charset defCharset) {
		if (m == null)
			return defCharset;
		String name = m.getParameters().get("charset");
		if (name == null)
			return defCharset;
		return Charset.forName(name);
	}

}
