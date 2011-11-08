package org.openrdf.store.blob.disk;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;

import org.openrdf.store.blob.BlobStore;
import org.openrdf.store.blob.BlobStoreProvider;

public class DiskBlobStoreProvider implements BlobStoreProvider {

	public BlobStore createBlobStore(String url, Map<String, String> parameters)
			throws IOException {
		URI uri = URI.create(url);
		if (uri.isAbsolute() && "file".equalsIgnoreCase(uri.getScheme())) {
			File dir = new File(uri);
			if (!dir.exists())
				return new DiskBlobStore(dir);
			String[] list = dir.list();
			if (dir.isDirectory() && list != null && list.length == 0)
				return new DiskBlobStore(dir);
			if (dir.isDirectory() && list != null
					&& Arrays.asList(list).contains("$versions"))
				return new DiskBlobStore(dir);
		}
		return null;
	}

}
