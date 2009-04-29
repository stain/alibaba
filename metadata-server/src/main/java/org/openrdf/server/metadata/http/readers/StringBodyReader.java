package org.openrdf.server.metadata.http.readers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import javax.ws.rs.core.MediaType;

import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.QueryResultParseException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;

public class StringBodyReader implements MessageBodyReader<String> {

	public boolean isReadable(Class<?> type, Type genericType,
			MediaType mediaType, ObjectConnection con) {
		return String.class == type;
	}

	public void writeTo(String result, String base, MediaType mediaType,
			OutputStream out) throws IOException {
		Writer writer = new OutputStreamWriter(out);
		writer.write(result);
		writer.flush();
	}

	public String readFrom(Class<? extends String> type, Type genericType,
			MediaType mediaType, InputStream in, Charset charset, String base,
			String location, ObjectConnection con)
			throws QueryResultParseException, TupleQueryResultHandlerException,
			QueryEvaluationException, IOException, RepositoryException {
		if (charset == null) {
			charset = Charset.defaultCharset();
		}
		StringWriter writer = new StringWriter();
		Reader reader = new InputStreamReader(in, charset);
		char[] cbuf = new char[512];
		int read;
		while ((read = reader.read(cbuf)) >= 0) {
			writer.write(cbuf, 0, read);
		}
		return writer.toString();
	}
}
