package org.openrdf.store.blob.disk;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

import org.openrdf.store.blob.BlobStore;
import org.openrdf.store.blob.BlobStoreProvider;

public class DiskBlobStoreProvider implements BlobStoreProvider {

	public BlobStore createBlobStore(String url, Map<String, String> parameters)
			throws IOException {
		URI uri = URI.create(url);
		if (uri.isAbsolute() && "file".equalsIgnoreCase(uri.getScheme())) {
			File dir = new File(uri);
			if (!dir.exists() || dir.isDirectory())
				return new DiskBlobStore(dir);
		}
		return null;
	}

}
