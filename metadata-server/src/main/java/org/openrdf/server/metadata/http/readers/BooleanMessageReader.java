package org.openrdf.server.metadata.http.readers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.BooleanQueryResultFormat;
import org.openrdf.query.resultio.BooleanQueryResultParserFactory;
import org.openrdf.query.resultio.BooleanQueryResultParserRegistry;
import org.openrdf.query.resultio.QueryResultParseException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.server.metadata.http.readers.base.MessageReaderBase;

public class BooleanMessageReader
		extends
		MessageReaderBase<BooleanQueryResultFormat, BooleanQueryResultParserFactory, Boolean> {

	public BooleanMessageReader() {
		super(BooleanQueryResultParserRegistry.getInstance(), Boolean.class);
	}

	@Override
	public boolean isReadable(Class<?> type, Type genericType, String mimeType,
			ObjectConnection con) {
		if (Object.class.equals(type))
			return false;
		if (!type.equals(Boolean.class) && !type.equals(Boolean.TYPE))
			return false;
		if (mimeType == null || mimeType.contains("*")
				|| "application/octet-stream".equals(mimeType))
			return false;
		return getFactory(mimeType) != null;
	}

	@Override
	public Boolean readFrom(BooleanQueryResultParserFactory factory,
			InputStream in, Charset charset, String base)
			throws QueryResultParseException, TupleQueryResultHandlerException,
			IOException, QueryEvaluationException {
		return factory.getParser().parse(in);
	}

}

