package org.openrdf.repository.object.advisers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.channels.ReadableByteChannel;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.openrdf.OpenRDFException;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.advice.Advice;
import org.openrdf.repository.object.advisers.helpers.SparqlEvaluator;
import org.openrdf.repository.object.advisers.helpers.SparqlEvaluator.SparqlBuilder;
import org.openrdf.repository.object.traits.ObjectMessage;
import org.openrdf.repository.object.util.GenericType;
import org.openrdf.result.Result;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class SparqlAdvice implements Advice {
	private final SparqlEvaluator evaluator;
	private final String[][] bindingNames;

	public SparqlAdvice(SparqlEvaluator evaluator, String[][] bindingNames) {
		this.evaluator = evaluator;
		this.bindingNames = bindingNames;
	}

	public Object intercept(ObjectMessage message) throws Throwable {
		Object target = message.getMsgTarget();
		ObjectConnection con = ((RDFObject) target).getObjectConnection();
		Resource self = ((RDFObject) target).getResource();
		GenericType rtype = new GenericType(message.getMethod()
				.getGenericReturnType());
		SparqlBuilder with = evaluator.prepare(con).with("this", self);
		Object[] args = message.getParameters();
		Class<?>[] types = message.getMethod().getParameterTypes();
		for (int i = 0; i < args.length && i < bindingNames.length; i++) {
			if (Set.class.equals(types[i])) {
				for (String name : bindingNames[i]) {
					with = with.with(name, (Set<?>) args[i]);
				}
			} else {
				for (String name : bindingNames[i]) {
					with = with.with(name, args[i]);
				}
			}
		}
		return as(with, rtype);
	}

	private Object as(SparqlBuilder result, GenericType rtype)
			throws OpenRDFException, TransformerException, IOException,
			ParserConfigurationException, SAXException, XMLStreamException {
		Class<?> rclass = rtype.getClassType();

		if (TupleQueryResult.class.equals(rclass)) {
			return result.asTupleQueryResult();
		} else if (GraphQueryResult.class.equals(rclass)) {
			return result.asGraphQueryResult();
		} else if (Result.class.equals(rclass)) {
			return result.asResult(rtype.getComponentClass());
		} else if (Set.class.equals(rclass)) {
			return result.asSet(rtype.getComponentClass());
		} else if (List.class.equals(rclass)) {
			return result.asList(rtype.getComponentClass());

		} else if (Void.class.equals(rclass) || Void.TYPE.equals(rclass)) {
			result.asUpdate();
			return null;
		} else if (Boolean.class.equals(rclass) || Boolean.TYPE.equals(rclass)) {
			return result.asBoolean();
		} else if (Byte.class.equals(rclass) || Byte.TYPE.equals(rclass)) {
			return result.asByte();
		} else if (Character.class.equals(rclass)
				|| Character.TYPE.equals(rclass)) {
			return result.asChar();
		} else if (Double.class.equals(rclass) || Double.TYPE.equals(rclass)) {
			return result.asDouble();
		} else if (Float.class.equals(rclass) || Float.TYPE.equals(rclass)) {
			return result.asFloat();
		} else if (Integer.class.equals(rclass) || Integer.TYPE.equals(rclass)) {
			return result.asInt();
		} else if (Long.class.equals(rclass) || Long.TYPE.equals(rclass)) {
			return result.asLong();
		} else if (Short.class.equals(rclass) || Short.TYPE.equals(rclass)) {
			return result.asShort();

		} else if (Model.class.equals(rclass)) {
			return result.asModel();
		} else if (Statement.class.equals(rclass)) {
			return result.asStatement();
		} else if (BindingSet.class.equals(rclass)) {
			return result.asBindingSet();
		} else if (URI.class.equals(rclass)) {
			return result.asURI();
		} else if (BNode.class.equals(rclass)) {
			return result.asBNode();
		} else if (Literal.class.equals(rclass)) {
			return result.asLiteral();
		} else if (Resource.class.equals(rclass)) {
			return result.asResource();
		} else if (Value.class.equals(rclass)) {
			return result.asValue();

		} else if (Document.class.equals(rclass)) {
			return result.asDocument();
		} else if (DocumentFragment.class.equals(rclass)) {
			return result.asDocumentFragment();
		} else if (Element.class.equals(rclass)) {
			return result.asElement();
		} else if (Node.class.equals(rclass)) {
			return result.asNode();

		} else if (Reader.class.equals(rclass)) {
			return result.asReader();
		} else if (ByteArrayOutputStream.class.equals(rclass)) {
			return result.asByteArrayOutputStream();
		} else if (ReadableByteChannel.class.equals(rclass)) {
			return result.asReadableByteChannel();
		} else if (InputStream.class.equals(rclass)) {
			return result.asInputStream();
		} else if (XMLEventReader.class.equals(rclass)) {
			return result.asXMLEventReader();

		} else {
			return result.as(rclass);
		}
	}

}
