package org.openrdf.store.blob;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

public abstract class BlobStoreTestCase extends TestCase {
	protected BlobStore store;
	protected File dir;
	protected AssertionFailedError error;

	public abstract BlobStore createBlobStore(File dir) throws IOException;

	public void setUp() throws Exception {
		dir = File.createTempFile("store", "");
		dir.delete();
		dir.mkdirs();
		store = createBlobStore(dir);
		error = null;
	}

	public void tearDown() throws Exception {
		store.erase();
		dir.delete();
	}

	public void testEraseEmpty() throws Exception {
		store.erase();
		assertEquals(0, dir.list().length);
	}

	public void testEraseSingleBlob() throws Exception {
		BlobVersion trx1 = store.newVersion("urn:test:trx1");
		Writer file = trx1.open("urn:test:file").openWriter();
		file.append("blob store test");
		file.close();
		trx1.commit();
		store.erase();
		assertEquals(0, dir.list().length);
	}

	public void testRoundTripString() throws Exception {
		BlobVersion trx1 = store.newVersion("urn:test:trx1");
		Writer file = trx1.open("urn:test:file").openWriter();
		file.append("blob store test");
		file.close();
		trx1.commit();
		BlobVersion trx2 = store.newVersion("urn:test:trx2");
		CharSequence str = trx2.open("urn:test:file").getCharContent(true);
		assertEquals("blob store test", str.toString());
	}

	public void testAutocommit() throws Exception {
		Writer file = store.open("urn:test:file").openWriter();
		file.append("blob store test");
		file.close();
		CharSequence str = store.open("urn:test:file").getCharContent(true);
		assertEquals("blob store test", str.toString());
	}

	public void testReopenInvalid() throws Exception {
		try {
			store.openVersion("urn:test:nothing");
			fail();
		} catch (IllegalArgumentException e) {
			// pass
		}
	}

	public void testAtomicity() throws Exception {
		BlobVersion trx1 = store.newVersion("urn:test:trx1");
		Writer file1 = trx1.open("urn:test:file1").openWriter();
		file1.append("blob store test");
		file1.close();
		Writer file2 = trx1.open("urn:test:file2").openWriter();
		file2.append("blob store test");
		file2.close();
		BlobVersion trx2 = store.newVersion("urn:test:trx2");
		assertNull(trx2.open("urn:test:file1").getCharContent(true));
		assertNull(trx2.open("urn:test:file2").getCharContent(true));
		trx1.commit();
		BlobVersion trx3 = store.newVersion("urn:test:trx3");
		assertEquals("blob store test", trx3.open("urn:test:file1")
				.getCharContent(true).toString());
		assertEquals("blob store test", trx3.open("urn:test:file2")
				.getCharContent(true).toString());
	}

	public void testIsolation() throws Exception {
		BlobVersion trx1 = store.newVersion("urn:test:trx1");
		Writer file1 = trx1.open("urn:test:file1").openWriter();
		file1.append("blob store test");
		file1.close();
		final CountDownLatch latch1 = new CountDownLatch(1);
		new Thread(new Runnable() {
			public void run() {
				try {
					error = null;
					try {
						BlobVersion trx2 = store.newVersion("urn:test:trx2");
						BlobObject blob = trx2.open("urn:test:file1");
						assertNull(blob.getCharContent(true));
					} catch (Exception e) {
						e.printStackTrace();
						fail();
					} finally {
						latch1.countDown();
					}
				} catch (AssertionFailedError e) {
					error = e;
				}
			}
		}).start();
		latch1.await();
		if (error != null)
			throw error;
		trx1.prepare();
		final CountDownLatch latch2 = new CountDownLatch(1);
		final CountDownLatch latch3 = new CountDownLatch(1);
		new Thread(new Runnable() {
			public void run() {
				try {
					error = null;
					try {
						latch2.countDown();
						BlobVersion trx3 = store.newVersion("urn:test:trx3");
						BlobObject blob = trx3.open("urn:test:file1");
						CharSequence str = blob.getCharContent(true);
						assertNotNull(str);
						assertEquals("blob store test", str.toString());
					} catch (Exception e) {
						e.printStackTrace();
						fail();
					} finally {
						latch3.countDown();
					}
				} catch (AssertionFailedError e) {
					error = e;
				}
			}
		}).start();
		latch2.await();
		assertFalse(latch3.await(1, TimeUnit.SECONDS));
		trx1.commit();
		latch3.await();
		if (error != null)
			throw error;
	}

}
