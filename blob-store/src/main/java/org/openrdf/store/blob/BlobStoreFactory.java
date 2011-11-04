/*
 * Copyright (c) 2011, 3 Round Stones Inc. Some rights reserved.
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
package org.openrdf.store.blob;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;

import org.openrdf.store.blob.disk.DiskBlobStore;
import org.openrdf.store.blob.file.FileBlobStore;

/**
 * Creates new {@link BlobStore}s, storing blobs to the given directories.
 * 
 * @author James Leigh
 * 
 */
public class BlobStoreFactory {
	private final Map<String, DiskBlobStore> stores = new WeakHashMap<String, DiskBlobStore>();

	/**
	 * Create or retrieve a BlobStore at this directory.
	 */
	public BlobStore openDiskStore(File dir) throws IOException {
		File d = dir.getCanonicalFile();
		String[] list = d.list();
		if (list == null || list.length == 0
				|| Arrays.asList(list).contains("$transactions")) {
			DiskBlobStore store = stores.get(d.getAbsolutePath());
			if (store == null) {
				stores.put(d.getAbsolutePath(), store = new DiskBlobStore(d));
			}
			return store;
		} else {
			// AliBaba's old blob store format
			return new FileBlobStore(d);
		}
	}
}
