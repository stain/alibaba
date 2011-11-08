package org.openrdf.store.blob;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

import org.openrdf.store.blob.disk.DiskBlobStore;

public class DiskBlobStoreTest extends BlobStoreTestCase {

	@Override
	public BlobStore createBlobStore(File dir) throws IOException {
		return new BlobStoreFactory().openBlobStore(dir);
	}

	public void testReopen() throws Exception {
		BlobVersion trx1 = store.open("urn:test:trx1");
		Writer file = trx1.open("urn:test:file").openWriter();
		file.append("test1");
		file.close();
		trx1.commit();
		BlobObject blob = store.reopen("urn:test:trx1").open("urn:test:file");
		CharSequence str = blob.getCharContent(true);
		assertEquals("test1", str.toString());
	}

	public void testStoreHistory() throws Exception {
		BlobVersion trx1 = store.open("urn:test:trx1");
		Writer file = trx1.open("urn:test:file").openWriter();
		file.append("test1");
		file.close();
		trx1.commit();
		BlobVersion trx2 = store.open("urn:test:trx2");
		file = trx2.open("urn:test:file").openWriter();
		file.append("test2");
		file.close();
		trx2.commit();
		assertEquals(Arrays.asList("urn:test:file", "urn:test:file"),
				Arrays.asList(store.getRecentModifications()));
	}

	public void testReopenPastVersion() throws Exception {
		BlobVersion trx1 = store.open("urn:test:trx1");
		Writer file = trx1.open("urn:test:file").openWriter();
		file.append("test1");
		file.close();
		trx1.commit();
		BlobVersion trx2 = store.open("urn:test:trx2");
		file = trx2.open("urn:test:file").openWriter();
		file.append("test2");
		file.close();
		trx2.commit();
		assertEquals("test1",
				store.reopen("urn:test:trx1").open("urn:test:file")
						.getCharContent(true).toString());
		assertEquals("test2",
				store.reopen("urn:test:trx2").open("urn:test:file")
						.getCharContent(true).toString());
	}

	public void testReopenPastAfterRestart() throws Exception {
		BlobVersion trx1 = store.open("urn:test:trx1");
		Writer file = trx1.open("urn:test:file").openWriter();
		file.append("test1");
		file.close();
		trx1.commit();
		BlobVersion trx2 = store.open("urn:test:trx2");
		file = trx2.open("urn:test:file").openWriter();
		file.append("test2");
		file.close();
		trx2.commit();
		store = new DiskBlobStore(dir);
		assertEquals("test1",
				store.reopen("urn:test:trx1").open("urn:test:file")
						.getCharContent(true).toString());
		assertEquals("test2",
				store.reopen("urn:test:trx2").open("urn:test:file")
						.getCharContent(true).toString());
	}

	public void testBlobHistory() throws Exception {
		BlobVersion trx1 = store.open("urn:test:trx1");
		Writer file1 = trx1.open("urn:test:file1").openWriter();
		file1.append("test1");
		file1.close();
		trx1.commit();
		BlobVersion trx2 = store.open("urn:test:trx2");
		Writer file2 = trx2.open("urn:test:file2").openWriter();
		file2.append("test2");
		file2.close();
		trx2.commit();
		BlobVersion trx3 = store.open("urn:test:trx3");
		file1 = trx3.open("urn:test:file1").openWriter();
		file1.append("test3");
		file1.close();
		trx3.commit();
		assertEquals(
				Arrays.asList("urn:test:trx3", "urn:test:trx1"),
				Arrays.asList(store.open("urn:test:trx4")
						.open("urn:test:file1").getRecentVersions()));
	}

	public void testBlobHistoryAfterRestart() throws Exception {
		BlobVersion trx1 = store.open("urn:test:trx1");
		Writer file1 = trx1.open("urn:test:file1").openWriter();
		file1.append("test1");
		file1.close();
		trx1.commit();
		BlobVersion trx2 = store.open("urn:test:trx2");
		Writer file2 = trx2.open("urn:test:file2").openWriter();
		file2.append("test2");
		file2.close();
		trx2.commit();
		BlobVersion trx3 = store.open("urn:test:trx3");
		file1 = trx3.open("urn:test:file1").openWriter();
		file1.append("test3");
		file1.close();
		trx3.commit();
		store = new DiskBlobStore(dir);
		assertEquals(
				Arrays.asList("urn:test:trx3", "urn:test:trx1"),
				Arrays.asList(store.open("urn:test:trx4")
						.open("urn:test:file1").getRecentVersions()));
	}

	public void testStoreTrimHistory() throws Exception {
		BlobVersion trx1 = store.open("urn:test:trx1");
		Writer file = trx1.open("urn:test:file").openWriter();
		file.append("test1");
		file.close();
		trx1.commit();
		BlobVersion trx2 = store.open("urn:test:trx2");
		file = trx2.open("urn:test:file").openWriter();
		file.append("test2");
		file.close();
		trx2.commit();
		store.reopen("urn:test:trx1").erase();
		assertEquals(Arrays.asList("urn:test:file"),
				Arrays.asList(store.getRecentModifications()));
		assertEquals("test2",
				store.open("urn:test:trx3").open("urn:test:file")
						.getCharContent(true).toString());
	}

}
