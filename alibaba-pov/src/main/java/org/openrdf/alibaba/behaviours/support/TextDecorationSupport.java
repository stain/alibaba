package org.openrdf.alibaba.behaviours.support;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openrdf.alibaba.behaviours.DecorationBehaviour;
import org.openrdf.alibaba.concepts.Encoding;
import org.openrdf.alibaba.concepts.Format;
import org.openrdf.alibaba.concepts.LiteralDisplay;
import org.openrdf.alibaba.concepts.Presentation;
import org.openrdf.alibaba.concepts.TextDecoration;
import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.exceptions.BadRequestException;
import org.openrdf.alibaba.vocabulary.POV;
import org.openrdf.elmo.annotations.rdf;

@rdf(POV.NS + "TextDecoration")
public class TextDecorationSupport implements DecorationBehaviour {
	private TextDecoration decoration;

	public TextDecorationSupport(TextDecoration decoration) {
		this.decoration = decoration;
	}

	public boolean isSeparation() {
		return decoration.getPovSeparation() != null;
	}

	public boolean isAfter(Map<String, ?> bindings) throws AlibabaException,
			IOException {
		return check(decoration.getPovAfter(), bindings);
	}

	public boolean isBefore(Map<String, ?> bindings) throws AlibabaException,
			IOException {
		return check(decoration.getPovBefore(), bindings);
	}

	public boolean isEmpty(Map<String, ?> bindings) throws AlibabaException,
			IOException {
		return check(decoration.getPovEmpty(), bindings);
	}

	public boolean isSeparation(Map<String, ?> bindings)
			throws AlibabaException, IOException {
		return check(decoration.getPovSeparation(), bindings);
	}

	public void after(Map<String, ?> bindings) throws AlibabaException,
			IOException {
		printOrRead(decoration.getPovAfter(), bindings);
	}

	public void before(Map<String, ?> bindings) throws AlibabaException,
			IOException {
		printOrRead(decoration.getPovBefore(), bindings);
	}

	public void separation(Map<String, ?> bindings) throws AlibabaException,
			IOException {
		printOrRead(decoration.getPovSeparation(), bindings);
	}

	public void empty(Map<String, ?> bindings) throws AlibabaException,
			IOException {
		printOrRead(decoration.getPovEmpty(), bindings);
	}

	public void values(Collection values, Map<String, ?> bindings) throws AlibabaException, IOException {
		assert bindings.get("presentation") instanceof Presentation : bindings;
		assert bindings.get("display") instanceof LiteralDisplay : bindings;
		Presentation presentation = (Presentation) bindings.get("presentation");
		Encoding enc = presentation.getPovEncoding();
		LiteralDisplay display = (LiteralDisplay) bindings.get("display");
		Format format = display.getPovFormat();
		if (bindings.containsKey("in")) {
			for (String encoded : parseValues(bindings)) {
				values.add(format.parse(enc.decode(encoded)));
			}
		} else {
			assert bindings.containsKey("out") : bindings;
			assert bindings.get("out") instanceof PrintWriter : bindings;
			PrintWriter out = (PrintWriter) bindings.get("out");
			Iterator<?> iter = values.iterator();
			while (iter.hasNext()) {
				out.print(enc.encode(format.format(iter.next())));
				if (iter.hasNext()) {
					separation(bindings);
				}
			}
		}
	}

	protected void print(String text, Map<String, ?> bindings)
			throws AlibabaException, IOException {
		assert bindings.get("out") instanceof PrintWriter : bindings;
		print(text, (PrintWriter) bindings.get("out"));
	}

	private void print(String text, PrintWriter out) {
		out.print(text);
	}

	protected void read(String text, Map<String, ?> bindings)
			throws AlibabaException, IOException {
		assert bindings.get("in") instanceof BufferedReader : bindings;
		read(text, (BufferedReader) bindings.get("in"));
	}

	protected void read(String text, BufferedReader in) throws IOException,
			AlibabaException, BadRequestException {
		in.mark(text.length());
		if (!readAlong(in, text, 0)) {
			in.reset();
			char[] cbuf = new char[text.length()];
			in.read(cbuf);
			throw new BadRequestException("Was \"" + String.valueOf(cbuf)
					+ "\" expected \"" + text + "\"");
		}
	}

	protected boolean check(String text, Map<String, ?> bindings)
			throws AlibabaException, IOException {
		assert bindings.get("in") instanceof BufferedReader : bindings;
		return check(text, (BufferedReader) bindings.get("in"));
	}

	protected boolean check(String text, BufferedReader in) throws IOException,
			AlibabaException {
		if (text == null)
			return true;
		in.mark(text.length());
		boolean checked = readAlong(in, text, 0);
		in.reset();
		return checked;
	}

	protected List<String> parseValues(Map<String, ?> bindings) throws AlibabaException, IOException {
		assert bindings.get("in") instanceof BufferedReader : bindings;
		BufferedReader in = (BufferedReader) bindings.get("in");
		String separation = decoration.getPovSeparation();
		String after = decoration.getPovAfter();
		return parseValues(separation, after, in);
	}

	protected List<String> parseValues(String separation, String termination,
			BufferedReader in) throws AlibabaException, IOException {
		List<String> list = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		while (true) {
			in.mark(1);
			int i = in.read();
			if (i == -1)
				break;
			char c = (char) i;
			if (separation.length() > 0 && separation.charAt(0) == c) {
				in.reset();
				if (check(separation, in)) {
					read(separation, in);
					list.add(sb.toString());
					sb.delete(0, sb.length());
					continue;
				}
				in.mark(1);
				in.read();
			}
			if (termination.length() > 0 && termination.charAt(0) == c) {
				in.reset();
				if (check(termination, in)) {
					list.add(sb.toString());
					return list;
				}
				in.mark(1);
				in.read();
			}
			sb.append(c);
		}
		return list;
	}

	private void printOrRead(String text, Map<String, ?> bindings)
			throws AlibabaException, IOException {
		if (text != null && bindings.containsKey("in")) {
			assert bindings.get("in") instanceof BufferedReader : bindings;
			read(text, bindings);
		} else if (text != null) {
			assert bindings.get("out") instanceof PrintWriter : bindings;
			print(text, bindings);
		}
	}

	private boolean readAlong(BufferedReader reader, String shouldBe, int off)
			throws AlibabaException, IOException {
		char[] cbuf = new char[shouldBe.length() - off];
		int count = reader.read(cbuf, 0, cbuf.length);
		for (int i = 0; i < count; i++) {
			if (cbuf[i] != shouldBe.charAt(off + i)) {
				return false;
			}
		}
		if (count + off < shouldBe.length()) {
			return readAlong(reader, shouldBe, count + off);
		}
		return true;
	}

}
