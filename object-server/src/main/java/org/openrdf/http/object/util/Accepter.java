package org.openrdf.http.object.util;

import static java.util.Collections.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

public class Accepter {
	private final static class MimeTypeComparator implements Comparator<MimeType> {
		public int compare(MimeType o1, MimeType o2) {
			String s1 = o1.getParameter("q");
			String s2 = o2.getParameter("q");
			Double q1 = s1 == null ? 1 : Double.valueOf(s1);
			Double q2 = s2 == null ? 1 : Double.valueOf(s2);
			int compare = q2.compareTo(q1);
			if (compare != 0)
				return compare;
			if (!"*".equals(o1.getPrimaryType())
					&& "*".equals(o2.getPrimaryType()))
				return -1;
			if ("*".equals(o1.getPrimaryType())
					&& !"*".equals(o2.getPrimaryType()))
				return 1;
			if (!"*".equals(o1.getSubType()) && "*".equals(o2.getSubType()))
				return -1;
			if ("*".equals(o1.getSubType()) && !"*".equals(o2.getSubType()))
				return 1;
			if (!"*".equals(o1.getSubType()) && "*".equals(o2.getSubType()))
				return -1;
			if (o1.getSubType().contains("+") && !o2.getSubType().contains("+"))
				return -1;
			if (!o1.getSubType().contains("+") && o2.getSubType().contains("+"))
				return 1;
			return o1.toString().compareTo(o2.toString());
		}
	}

	public static MimeType parse(String mediaType) throws MimeTypeParseException {
		if (mediaType == null || mediaType.equals("*")) {
			return new MimeType("*/*");
		}
		if (mediaType.indexOf('/') < 0) {
			int dash = mediaType.indexOf('-');
			if (dash >= 0) {
				return new MimeType(mediaType.substring(0, dash), mediaType.substring(dash + 1));
			}
			return new MimeType(mediaType, "*");
		}
		return new MimeType(mediaType);
	}

	public static boolean isCompatible(MimeType accept, MimeType media) {
		if (accept == null || media == null)
			return true;
		if (accept.match(media))
			return isParametersCompatible(accept, media);
		if ("*".equals(media.getPrimaryType()))
			return isParametersCompatible(accept, media);
		if ("*".equals(accept.getPrimaryType()))
			return isParametersCompatible(accept, media);
		if (!media.getPrimaryType().equals(accept.getPrimaryType()))
			return false;
		if ("*".equals(media.getSubType()))
			return isParametersCompatible(accept, media);
		if ("*".equals(accept.getSubType()))
			return isParametersCompatible(accept, media);
		if (media.getSubType().endsWith("+" + accept.getSubType()))
			return isParametersCompatible(accept, media);
		return false;
	}

	private static boolean isParametersCompatible(MimeType accept, MimeType media) {
		Enumeration names = accept.getParameters().getNames();
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			if ("q".equals(name))
				continue;
			if (!accept.getParameter(name).equals(media.getParameter(name)))
				return false;
		}
		return true;
	}

	private final TreeSet<MimeType> acceptable = new TreeSet<MimeType>(
			new MimeTypeComparator());

	public Accepter() {
		// use addMimeType
	}

	public Accepter(String accept) throws MimeTypeParseException {
		this(accept == null ? null : accept.split(",\\s*"));
	}

	public Accepter(String... mediaTypes) throws MimeTypeParseException {
		if (mediaTypes != null) {
			for (String mediaType : mediaTypes) {
				addMimeType(mediaType);
			}
		}
	}

	public Accepter(Collection<MimeType> mediaTypes) {
		if (mediaTypes != null) {
			for (MimeType mediaType : mediaTypes) {
				addMimeType(mediaType);
			}
		}
	}

	public void addMimeType(String mediaType) throws MimeTypeParseException {
		addMimeType(parse(mediaType));
	}

	public void addMimeType(MimeType mediaType) {
		acceptable.add(mediaType);
	}

	public String toString() {
		if (acceptable.isEmpty())
			return "*/*";
		String str = acceptable.toString();
		return str.substring(1, str.length() - 1);
	}

	public boolean isAcceptable(String mediaType) throws MimeTypeParseException {
		if (acceptable.isEmpty() || mediaType == null)
			return true;
		MimeType media = parse(mediaType);
		for (MimeType accept : acceptable) {
			if (isCompatible(accept, media))
				return true;
		}
		return false;
	}

	public boolean isAcceptable(String[] mediaTypes)
			throws MimeTypeParseException {
		if (acceptable.isEmpty() || mediaTypes == null)
			return true;
		for (String mediaType : mediaTypes) {
			if (isAcceptable(mediaType))
				return true;
		}
		return false;
	}

	public SortedSet<? extends MimeType> getAcceptable()
			throws MimeTypeParseException {
		if (acceptable.isEmpty()) {
			return new TreeSet<MimeType>(singleton(new MimeType("*/*")));
		} else {
			return acceptable;
		}
	}

	public SortedSet<? extends MimeType> getCompatible(String mediaType)
			throws MimeTypeParseException {
		if (mediaType == null && acceptable.isEmpty())
			return new TreeSet(singleton(new MimeType("*/*")));
		if (mediaType == null)
			return acceptable;
		MimeType media = parse(mediaType);
		if (acceptable.isEmpty())
			return new TreeSet(singleton(new MimeType("*/*")));
		SortedSet<MimeType> result = new TreeSet<MimeType>();
		for (MimeType accept : acceptable) {
			if (isCompatible(accept, media)) {
				result.add(accept);
			}
		}
		return result;
	}

	public SortedSet<? extends MimeType> getCompatible(String[] mediaTypes)
			throws MimeTypeParseException {
		SortedSet<MimeType> result = new TreeSet<MimeType>();
		if (acceptable.isEmpty()) {
			result.add(new MimeType("*/*"));
		} else if (mediaTypes == null) {
			return acceptable;
		} else {
			for (MimeType accept : acceptable) {
				for (String mediaType : mediaTypes) {
					if (isCompatible(accept, parse(mediaType))) {
						result.add(accept);
					}
				}
			}
		}
		return result;
	}

	public SortedSet<? extends MimeType> getCompatible(
			Collection<? extends MimeType> mediaTypes)
			throws MimeTypeParseException {
		SortedSet<MimeType> result = new TreeSet<MimeType>();
		if (acceptable.isEmpty()) {
			result.add(new MimeType("*/*"));
		} else if (mediaTypes == null) {
			return acceptable;
		} else {
			for (MimeType accept : acceptable) {
				for (MimeType media : mediaTypes) {
					if (isCompatible(accept, media)) {
						result.add(accept);
					}
				}
			}
		}
		return result;
	}

	public List<? extends MimeType> getAcceptable(String mediaType)
			throws MimeTypeParseException {
		if (mediaType == null && acceptable.isEmpty())
			return singletonList(new MimeType("*/*"));
		if (mediaType == null)
			return new ArrayList<MimeType>(acceptable);
		MimeType media = parse(mediaType);
		if (acceptable.isEmpty())
			return singletonList(media);
		List<MimeType> result = new ArrayList<MimeType>();
		for (MimeType accept : acceptable) {
			if (isCompatible(accept, media)) {
				result.add(combine(accept, media));
			}
		}
		return result;
	}

	public List<? extends MimeType> getAcceptable(String[] mediaTypes)
			throws MimeTypeParseException {
		List<MimeType> result = new ArrayList<MimeType>();
		if (acceptable.isEmpty()) {
			if (mediaTypes == null)
				return singletonList(new MimeType("*/*"));
			for (String mediaType : mediaTypes) {
				result.add(parse(mediaType));
			}
		} else {
			for (MimeType accept : acceptable) {
				if (mediaTypes == null) {
					result.add(accept);
				} else {
					for (String mediaType : mediaTypes) {
						if (mediaType == null) {
							result.add(accept);
						} else {
							MimeType media = parse(mediaType);
							if (isCompatible(accept, media)) {
								result.add(combine(accept, media));
							}
						}
					}
				}
			}
		}
		return result;
	}

	private MimeType combine(MimeType accept, MimeType media) {
		try {
			if (media == null)
				return accept;
			if (accept == null)
				return media;
			if ("*".equals(media.getPrimaryType())) {
				media.setPrimaryType(accept.getPrimaryType());
			}
			if ("*".equals(media.getSubType())) {
				media.setSubType(accept.getSubType());
			}
			Enumeration e = accept.getParameters().getNames();
			while (e.hasMoreElements()) {
				String p = (String) e.nextElement();
				if (!"q".equals(p) && media.getParameter(p) == null) {
					media.setParameter(p, accept.getParameter(p));
				}
			}
			return media;
		} catch (MimeTypeParseException e) {
			throw new AssertionError(e);
		}
	}
}
