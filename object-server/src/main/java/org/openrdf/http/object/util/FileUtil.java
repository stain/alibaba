package org.openrdf.http.object.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

public class FileUtil {
	private static final Collection<File> temporary = new ArrayList<File>();

	public static void deleteOnExit(File dir) {
		synchronized (temporary) {
			if (temporary.isEmpty()) {
				Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
					public void run() {
						synchronized (temporary) {
							for (File dir : temporary) {
								deleteFileOrDir(dir, 256);
							}
						}
					}
				}, "Temporary Directory Cleanup"));
			}
			temporary.add(dir);
		}
	}

	private static void deleteFileOrDir(File dir, int max) {
		File[] listFiles = dir.listFiles();
		if (listFiles != null && max > 0) {
			for (File file : listFiles) {
				deleteFileOrDir(file, max - 1);
			}
		}
		dir.delete();
	}
}
