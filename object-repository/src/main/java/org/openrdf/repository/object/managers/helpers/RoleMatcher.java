package org.openrdf.repository.object.managers.helpers;

import java.util.Collection;
import java.util.List;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class RoleMatcher implements Cloneable {
	private ConcurrentNavigableMap<String, List<Class<?>>> pathprefix = new ConcurrentSkipListMap();
	private ConcurrentNavigableMap<String, List<Class<?>>> uriprefix = new ConcurrentSkipListMap();
	private ConcurrentMap<String, List<Class<?>>> paths = new ConcurrentHashMap();
	private ConcurrentMap<String, List<Class<?>>> uris = new ConcurrentHashMap();
	private boolean empty = true;

	public RoleMatcher clone() {
		try {
			RoleMatcher cloned = (RoleMatcher) super.clone();
			cloned.pathprefix = new ConcurrentSkipListMap(pathprefix);
			cloned.uriprefix = new ConcurrentSkipListMap(uriprefix);
			cloned.paths = new ConcurrentHashMap(paths);
			cloned.uris = new ConcurrentHashMap(uris);
			return cloned;
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
	}

	public boolean isEmpty() {
		return empty;
	}

	public void addRoles(String pattern, Class<?> role) {
		if (pattern.endsWith("*")) {
			String prefix = pattern.substring(0, pattern.length() - 1);
			if (prefix.startsWith("/")) {
				add(paths, prefix, role);
				add(pathprefix, prefix, role);
			} else {
				add(uris, prefix, role);
				add(uriprefix, prefix, role);
			}
		} else {
			if (pattern.startsWith("/")) {
				add(paths, pattern, role);
			} else {
				add(uris, pattern, role);
			}
		}
		empty = false;
	}

	public void findRoles(String uri, Collection<Class<?>> roles) {
		List<Class<?>> list = uris.get(uri);
		if (list != null) {
			roles.addAll(list);
		}
		findRoles(uriprefix, uri, roles);
		int idx = uri.indexOf("://");
		if (idx > 0) {
			String path = uri.substring(uri.indexOf('/', idx + 3));
			list = paths.get(path);
			if (list != null) {
				roles.addAll(list);
			}
			findRoles(pathprefix, path, roles);
		}
	}

	private void add(ConcurrentMap<String, List<Class<?>>> map, String pattern,
			Class<?> role) {
		List<Class<?>> list = map.get(pattern);
		if (list == null) {
			list = new CopyOnWriteArrayList<Class<?>>();
			List<Class<?>> o = map.putIfAbsent(pattern, list);
			if (o != null) {
				list = o;
			}
		}
		list.add(role);
	}

	private boolean findRoles(NavigableMap<String, List<Class<?>>> map,
			String full, Collection<Class<?>> roles) {
		String key = map.lowerKey(full);
		if (key == null) {
			return false;
		} else if (full.startsWith(key)) {
			roles.addAll(map.get(key));
			findRoles(map, key, roles);
			return true;
		} else {
			int idx = 0;
			while (idx < full.length() && idx < key.length()
					&& full.charAt(idx) == key.charAt(idx)) {
				idx++;
			}
			String prefix = full.substring(0, idx);
			if (map.containsKey(prefix)) {
				roles.addAll(map.get(prefix));
				if (idx > 1) {
					findRoles(map, prefix, roles);
				}
				return true;
			} else if (idx > 1) {
				return findRoles(map, prefix, roles);
			} else {
				return false;
			}
		}
	}
}
