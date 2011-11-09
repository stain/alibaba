package org.openrdf.store.blob.file;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

import org.openrdf.store.blob.BlobStore;
import org.openrdf.store.blob.BlobStoreProvider;

public class FileBlobStoreProvider implements BlobStoreProvider {

	public BlobStore createBlobStore(String url, Map<String, String> map)
			throws IOException {
		String provider = map == null ? null : map.get("provider");
		if (provider == null || !FileBlobStoreProvider.class.getName().equals(provider))
			return null;
		URI uri = URI.create(url);
		if (uri.isAbsolute() && "file".equalsIgnoreCase(uri.getScheme())) {
			File dir = new File(uri);
			if (!dir.exists() || dir.isDirectory())
				return new FileBlobStore(dir);
		}
		return null;
	}

}
