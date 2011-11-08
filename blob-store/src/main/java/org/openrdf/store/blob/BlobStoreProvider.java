package org.openrdf.store.blob;

import java.util.Map;

public interface BlobStoreProvider {
	BlobStore createBlobStore(String url, Map<String, String> parameters) throws Exception;
}
