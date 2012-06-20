package org.openrdf.sail.optimistic;

import java.util.concurrent.CountDownLatch;

import junit.framework.TestCase;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.sail.memory.MemoryStore;

public class DeadLockTest extends TestCase {
	private OptimisticRepository sail;
	private RepositoryConnection a;
	private RepositoryConnection b;
	private String NS = "http://rdf.example.org/";
	private URI PAINTER;
	private URI PICASSO;
	private URI REMBRANDT;

	@Override
	public void setUp() throws Exception {
		sail = new OptimisticRepository(new MemoryStore());
		sail.setReadSnapshot(true);
		sail.setSnapshot(false);
		sail.setSerializable(false);
		sail.initialize();
		ValueFactory uf = sail.getValueFactory();
		PAINTER = uf.createURI(NS, "Painter");
		PICASSO = uf.createURI(NS, "picasso");
		REMBRANDT = uf.createURI(NS, "rembrandt");
		a = sail.getConnection();
		b = sail.getConnection();
	}

	@Override
	public void tearDown() throws Exception {
		a.close();
		b.close();
		sail.shutDown();
	}

	public void test() throws Exception {
		final CountDownLatch start = new CountDownLatch(2);
		final CountDownLatch end = new CountDownLatch(2);
		final CountDownLatch commit = new CountDownLatch(1);
		final Exception e1 = new Exception();
		new Thread(new Runnable() {
			public void run() {
				try {
					start.countDown();
					a.setAutoCommit(false);
					a.add(PICASSO, RDF.TYPE, PAINTER);
					commit.await();
					a.setAutoCommit(true);
				} catch (Exception e) {
					e1.initCause(e);
				} finally {
					end.countDown();
				}
			}
		}).start();
		final Exception e2 = new Exception();
		new Thread(new Runnable() {
			public void run() {
				try {
					start.countDown();
					b.setAutoCommit(false);
					b.add(REMBRANDT, RDF.TYPE, PAINTER);
					commit.await();
					b.setAutoCommit(true);
				} catch (Exception e) {
					e2.initCause(e);
				} finally {
					end.countDown();
				}
			}
		}).start();
		start.await();
		synchronized (sail.getSail()) {
			commit.countDown();
			Thread.sleep(500);
		}
		end.await();
		assertNull(e1.getCause());
		assertNull(e2.getCause());
	}

}
