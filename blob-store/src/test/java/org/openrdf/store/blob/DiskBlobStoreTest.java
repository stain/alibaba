package org.openrdf.store.blob;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.util.Arrays;

import org.openrdf.store.blob.disk.DiskBlobStore;

public class DiskBlobStoreTest extends BlobStoreTestCase {

	@Override
	public BlobStore createBlobStore(File dir) throws IOException {
		return new BlobStoreFactory().openDiskStore(dir);
	}

	public void testReopen() throws Exception {
		BlobTransaction trx1 = store.open("urn:test:trx1");
		Writer file = trx1.open(URI.create("urn:test:file")).openWriter();
		file.append("test1");
		file.close();
		trx1.commit();
		BlobObject blob = store.reopen("urn:test:trx1").open(
				URI.create("urn:test:file"));
		CharSequence str = blob.getCharContent(true);
		assertEquals("test1", str.toString());
	}

	public void testStoreHistory() throws Exception {
		BlobTransaction trx1 = store.open("urn:test:trx1");
		Writer file = trx1.open(URI.create("urn:test:file")).openWriter();
		file.append("test1");
		file.close();
		trx1.commit();
		BlobTransaction trx2 = store.open("urn:test:trx2");
		file = trx2.open(URI.create("urn:test:file")).openWriter();
		file.append("test2");
		file.close();
		trx2.commit();
		assertEquals(Arrays.asList("urn:test:trx1", "urn:test:trx2"),
				Arrays.asList(store.getHistory()));
		assertEquals(Arrays.asList(),
				Arrays.asList(store.reopen("urn:test:trx1").getHistory()));
		assertEquals(Arrays.asList("urn:test:trx1"),
				Arrays.asList(store.reopen("urn:test:trx2").getHistory()));
		assertEquals(Arrays.asList("urn:test:trx1", "urn:test:trx2"),
				Arrays.asList(store.open("urn:test:trx3").getHistory()));
	}

	public void testReopenHistory() throws Exception {
		BlobTransaction trx1 = store.open("urn:test:trx1");
		Writer file = trx1.open(URI.create("urn:test:file")).openWriter();
		file.append("test1");
		file.close();
		trx1.commit();
		BlobTransaction trx2 = store.open("urn:test:trx2");
		file = trx2.open(URI.create("urn:test:file")).openWriter();
		file.append("test2");
		file.close();
		trx2.commit();
		assertEquals("test1",
				store.reopen("urn:test:trx1").open(URI.create("urn:test:file"))
						.getCharContent(true).toString());
		assertEquals("test2",
				store.reopen("urn:test:trx2").open(URI.create("urn:test:file"))
						.getCharContent(true).toString());
	}

	public void testReopenHistoryAfterRestart() throws Exception {
		BlobTransaction trx1 = store.open("urn:test:trx1");
		Writer file = trx1.open(URI.create("urn:test:file")).openWriter();
		file.append("test1");
		file.close();
		trx1.commit();
		BlobTransaction trx2 = store.open("urn:test:trx2");
		file = trx2.open(URI.create("urn:test:file")).openWriter();
		file.append("test2");
		file.close();
		trx2.commit();
		store = new DiskBlobStore(dir);
		assertEquals("test1",
				store.reopen("urn:test:trx1").open(URI.create("urn:test:file"))
						.getCharContent(true).toString());
		assertEquals("test2",
				store.reopen("urn:test:trx2").open(URI.create("urn:test:file"))
						.getCharContent(true).toString());
	}

	public void testBlobHistory() throws Exception {
		BlobTransaction trx1 = store.open("urn:test:trx1");
		Writer file1 = trx1.open(URI.create("urn:test:file1")).openWriter();
		file1.append("test1");
		file1.close();
		trx1.commit();
		BlobTransaction trx2 = store.open("urn:test:trx2");
		Writer file2 = trx2.open(URI.create("urn:test:file2")).openWriter();
		file2.append("test2");
		file2.close();
		trx2.commit();
		BlobTransaction trx3 = store.open("urn:test:trx3");
		file1 = trx3.open(URI.create("urn:test:file1")).openWriter();
		file1.append("test3");
		file1.close();
		trx3.commit();
		assertEquals(
				Arrays.asList(),
				Arrays.asList(store.reopen("urn:test:trx1")
						.open(URI.create("urn:test:file1")).getHistory()));
		assertEquals(
				Arrays.asList("urn:test:trx1"),
				Arrays.asList(store.reopen("urn:test:trx2")
						.open(URI.create("urn:test:file1")).getHistory()));
		assertEquals(
				Arrays.asList("urn:test:trx1"),
				Arrays.asList(store.reopen("urn:test:trx3")
						.open(URI.create("urn:test:file1")).getHistory()));
		assertEquals(
				Arrays.asList("urn:test:trx1", "urn:test:trx3"),
				Arrays.asList(store.open("urn:test:trx4")
						.open(URI.create("urn:test:file1")).getHistory()));
	}

	public void testBlobHistoryAfterRestart() throws Exception {
		BlobTransaction trx1 = store.open("urn:test:trx1");
		Writer file1 = trx1.open(URI.create("urn:test:file1")).openWriter();
		file1.append("test1");
		file1.close();
		trx1.commit();
		BlobTransaction trx2 = store.open("urn:test:trx2");
		Writer file2 = trx2.open(URI.create("urn:test:file2")).openWriter();
		file2.append("test2");
		file2.close();
		trx2.commit();
		BlobTransaction trx3 = store.open("urn:test:trx3");
		file1 = trx3.open(URI.create("urn:test:file1")).openWriter();
		file1.append("test3");
		file1.close();
		trx3.commit();
		store = new DiskBlobStore(dir);
		assertEquals(
				Arrays.asList(),
				Arrays.asList(store.reopen("urn:test:trx1")
						.open(URI.create("urn:test:file1")).getHistory()));
		assertEquals(
				Arrays.asList("urn:test:trx1"),
				Arrays.asList(store.reopen("urn:test:trx2")
						.open(URI.create("urn:test:file1")).getHistory()));
		assertEquals(
				Arrays.asList("urn:test:trx1"),
				Arrays.asList(store.reopen("urn:test:trx3")
						.open(URI.create("urn:test:file1")).getHistory()));
		assertEquals(
				Arrays.asList("urn:test:trx1", "urn:test:trx3"),
				Arrays.asList(store.open("urn:test:trx4")
						.open(URI.create("urn:test:file1")).getHistory()));
	}

}
