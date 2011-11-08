package org.openrdf.store.blob.file;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;

import org.openrdf.store.blob.BlobStore;
import org.openrdf.store.blob.BlobStoreProvider;

public class FileBlobStoreProvider implements BlobStoreProvider {

	public BlobStore createBlobStore(String url, Map<String, String> parameters)
			throws IOException {
		URI uri = URI.create(url);
		if (uri.isAbsolute() && "file".equalsIgnoreCase(uri.getScheme())) {
			File dir = new File(uri);
			if (!dir.exists())
				return null;
			String[] list = dir.list();
			if (dir.isDirectory() && list != null && list.length == 0)
				return null;
			if (dir.isDirectory() && list != null
					&& Arrays.asList(list).contains("$versions"))
				return null;
			return new FileBlobStore(dir);
		}
		return null;
	}

}
