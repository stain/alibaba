package org.openrdf.http.object.model;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

import org.apache.http.nio.entity.ProducingNHttpEntity;

public interface HttpEntityChannel extends ProducingNHttpEntity {

	ReadableByteChannel getReadableByteChannel() throws IOException;
}
