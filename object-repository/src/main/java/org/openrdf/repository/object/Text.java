/*
 * Copyright (c) 2012, 3 Round Stones Inc. Some rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.openrdf.repository.object;

import java.io.Serializable;

/**
 * Represents pairs of strings and language tags, and thus represents plain RDF
 * literals with a language tag.
 * 
 * @author James Leigh
 * 
 */
public class Text implements CharSequence, Serializable, Comparable<Text> {
	private static final long serialVersionUID = 8175463447271413979L;

	public static Text valueOf(String label) {
		return new Text(label);
	}

	public static Text valueOf(String label, String language) {
		return new Text(label, language);
	}

	private final String label;
	private final String lang;

	public Text(String label) {
		this(label, null);
	}

	public Text(String label, String lang) {
		assert label != null;
		this.label = label;
		this.lang = lang == null ? "" : lang;
	}

	public String getLang() {
		return lang;
	}

	public int length() {
		return toString().length();
	}

	public char charAt(int index) {
		return toString().charAt(index);
	}

	public CharSequence subSequence(int start, int end) {
		return toString().subSequence(start, end);
	}

	@Override
	public String toString() {
		return label;
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o instanceof Text) {
			Text other = (Text) o;
			if (!toString().equals(other.toString()))
				return false;
			if (!getLang().equals(other.getLang()))
				return false;
			return true;
		}
		return false;
	}

	public int compareTo(Text o) {
		if (this == o)
			return 0;
		if (o == null)
			return 1;
		int result = this.getLang().compareTo(o.getLang());
		if (result == 0)
			return this.toString().compareTo(o.toString());
		return result;
	}

	/**
	 * Extended filtering compares extended language ranges to language tags.
	 * Each extended language range in the language priority list is considered
	 * in turn, according to priority. A language range matches a particular
	 * language tag if each respective list of subtags matches.
	 * 
	 * Two subtags match if either they are the same when compared
	 * case-insensitively or the language range's subtag is the wildcard '*'.
	 * 
	 * @see RFC 4647 Matching of Language Tags
	 * 
	 * @param range
	 *            In a language range, each subtag MUST either be a sequence of
	 *            ASCII alphanumeric characters or the single character '*'
	 *            (%x2A, ASTERISK). The character '*' is a "wildcard" that
	 *            matches any sequence of subtags. The meaning and uses of
	 *            wildcards vary according to the type of language range.
	 * @return true if this has a language tag that matches the extended
	 *         language range given; otherwise, false
	 */
	public boolean matchesLang(String range) {
		// 1. Split both the extended language range and the language tag being
		// compared into a list of subtags by dividing on the hyphen (%x2D)
		// character.
		String[] subtags = getLang().split("-");
		String[] subranges = range.split("-");

		// 2. Begin with the first subtag in each list. If the first subtag in
		// the range does not match the first subtag in the tag, the overall
		// match fails. Otherwise, move to the next subtag in both the
		// range and the tag.
		if (!subtags[0].equalsIgnoreCase(subranges[0]))
			return false;

		// 3. While there are more subtags left in the language range's list:
		int r = 1, t = 1;
		while (r < subranges.length) {

			// A. If the subtag currently being examined in the range is the
			// wildcard ('*'), move to the next subtag in the range and
			// continue with the loop.
			if ("*".equals(subranges[r])) {
				r++;
				continue;
			}

			// B. Else, if there are no more subtags in the language tag's
			// list, the match fails.
			if (t >= subtags.length)
				return false;

			// C. Else, if the current subtag in the range's list matches the
			// current subtag in the language tag's list, move to the next
			// subtag in both lists and continue with the loop.
			if (subranges[r].equalsIgnoreCase(subtags[t])) {
				r++;
				t++;
				continue;
			}

			// D. Else, if the language tag's subtag is a "singleton" (a single
			// letter or digit, which includes the private-use subtag 'x')
			// the match fails.
			if (subtags[t].length() == 1)
				return false;

			// E. Else, move to the next subtag in the language tag's list and
			// continue with the loop.
			t++;
			continue;
		}
		// 4. When the language range's list has no more subtags, the match
		// succeeds.
		return true;
	}

}
