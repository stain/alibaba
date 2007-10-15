package org.openrdf.alibaba.decor.support;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openrdf.alibaba.decor.DecorationBehaviour;
import org.openrdf.alibaba.decor.Encoding;
import org.openrdf.alibaba.decor.Presentation;
import org.openrdf.alibaba.decor.TextDecoration;
import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.exceptions.BadRequestException;
import org.openrdf.alibaba.formats.Format;
import org.openrdf.alibaba.pov.Display;
import org.openrdf.alibaba.vocabulary.DCR;
import org.openrdf.elmo.annotations.rdf;

@rdf(DCR.NS + "TextDecoration")
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
		String after = decoration.getPovAfter();
		String separation = decoration.getPovSeparation();
		return isFrist(after, separation, bindings);
	}

	public boolean isBefore(Map<String, ?> bindings) throws AlibabaException,
			IOException {
		String before = decoration.getPovBefore();
		String empty = decoration.getPovEmpty();
		return isFrist(before, empty, bindings);
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
		assert bindings.get("display") instanceof Display : bindings;
		Presentation presentation = (Presentation) bindings.get("presentation");
		Encoding enc = presentation.getPovEncoding();
		Display display = (Display) bindings.get("display");
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

	protected String interpret(String text, Map<String, ?> bindings)
			throws AlibabaException, IOException {
		return text;
	}

	protected void print(String text, Map<String, ?> bindings)
			throws AlibabaException, IOException {
		assert bindings.get("out") instanceof PrintWriter : bindings;
		String interpreted = interpret(text, bindings);
		print(interpreted, (PrintWriter) bindings.get("out"));
	}

	private void print(String text, PrintWriter out) {
		out.print(text);
	}

	private boolean isFrist(String first, String second, Map<String, ?> bindings) throws AlibabaException, IOException {
		assert bindings.get("in") instanceof BufferedReader : bindings;
		BufferedReader reader = (BufferedReader) bindings.get("in");
		String after = interpret(first, bindings);
		if (after == null) {
			String separation = interpret(second, bindings);
			return separation == null || !check(separation, reader);
		}
		if (!check(after, reader))
			return false;
		String separation = interpret(second, bindings);
		if (separation == null || !check(separation, reader))
			return true;
		return separation.length() <= after.length();
	}

	private void read(String text, Map<String, ?> bindings)
			throws AlibabaException, IOException {
		assert bindings.get("in") instanceof BufferedReader : bindings;
		String interpreted = interpret(text, bindings);
		read(interpreted, (BufferedReader) bindings.get("in"));
	}

	private void read(String text, BufferedReader in) throws IOException,
			AlibabaException, BadRequestException {
		in.mark(text.length());
		if (!readAlong(in, text, 0)) {
			in.reset();
			char[] cbuf = new char[text.length()];
			in.read(cbuf);
			StringBuilder msg = new StringBuilder();
			msg.append("Expected \"").append(text).append("\" at \"");
			msg.append(cbuf).append(in.readLine()).append("\"");
			throw new BadRequestException(msg.toString());
		}
	}

	private boolean check(String text, BufferedReader in) throws IOException,
			AlibabaException {
		if (text == null)
			return true;
		in.mark(text.length());
		boolean checked = readAlong(in, text, 0);
		in.reset();
		return checked;
	}

	private List<String> parseValues(Map<String, ?> bindings) throws AlibabaException, IOException {
		assert bindings.get("in") instanceof BufferedReader : bindings;
		BufferedReader in = (BufferedReader) bindings.get("in");
		String separation = interpret(decoration.getPovSeparation(), bindings);
		String after = interpret(decoration.getPovAfter(), bindings);
		return parseValues(separation, after, in);
	}

	private List<String> parseValues(String separation, String termination,
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
		String interpreted = interpret(text, bindings);
		if (interpreted != null && bindings.containsKey("in")) {
			assert bindings.get("in") instanceof BufferedReader : bindings;
			read(interpreted, bindings);
		} else if (interpreted != null) {
			assert bindings.get("out") instanceof PrintWriter : bindings;
			print(interpreted, bindings);
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
