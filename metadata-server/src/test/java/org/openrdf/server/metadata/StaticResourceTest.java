package org.openrdf.server.metadata;

import info.aduna.io.FileUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Date;

import junit.framework.TestCase;

import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.store.StoreConfigException;
import org.openrdf.store.StoreException;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

public class StaticResourceTest extends TestCase {
	private ObjectRepository repository;
	private MetadataServer server;
	private File dataDir;
	private String host;
	private WebResource client;

	@Override
	public void setUp() throws Exception {
		repository = createRepository();
		dataDir = FileUtil.createTempDir("metadata");
		server = new MetadataServer(repository, dataDir);
		server.setPort(3453);
		server.start();
		host = "localhost:" + server.getPort();
		client = Client.create().resource("http://" + host);
	}

	@Override
	public void tearDown() throws Exception {
		server.stop();
		repository.shutDown();
		FileUtil.deltree(dataDir);
	}

	public void testGET() throws Exception {
		File dir = new File(dataDir, host);
		dir.mkdirs();
		Writer out = new FileWriter(new File(dir, "hello"));
		out.write("world");
		out.close();
		assertEquals("world", client.path("hello").get(String.class));
	}

	public void testPUT() throws Exception {
		client.path("hello").put(String.class, "world");
		assertEquals("world", client.path("hello").get(String.class));
	}

	public void testDELETE() throws Exception {
		client.path("hello").put(String.class, "world");
		client.path("hello").delete();
		try {
			client.path("hello").get(String.class);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(404, e.getResponse().getStatus());
		}
	}

	public void testGETIfModifiedSince() throws Exception {
		File dir = new File(dataDir, host);
		dir.mkdirs();
		Writer out = new FileWriter(new File(dir, "hello"));
		out.write("world");
		out.close();
		WebResource hello = client.path("hello");
		Date lastModified = hello.head().getLastModified();
		try {
			hello.header("If-Modified-Since", lastModified).get(String.class);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(304, e.getResponse().getStatus());
		}
		Thread.sleep(1000);
		out = new FileWriter(new File(dir, "hello"));
		out.write("bad world");
		out.close();
		assertEquals("bad world", hello.header("If-Modified-Since", lastModified).get(String.class));
	}

	public void testPUTIfUnmodifiedSince() throws Exception {
		WebResource hello = client.path("hello");
		hello.put(String.class, "world");
		Date lastModified = hello.head().getLastModified();
		Thread.sleep(2000);
		hello.put(String.class, "new world");
		try {
			hello.header("If-Unmodified-Since", lastModified).put(String.class, "bad world");
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(412, e.getResponse().getStatus());
		}
		assertEquals("new world", hello.get(String.class));
	}

	public void testDELETEIfUnmodifiedSince() throws Exception {
		WebResource hello = client.path("hello");
		hello.put(String.class, "world");
		Date lastModified = hello.head().getLastModified();
		Thread.sleep(2000);
		hello.put(String.class, "new world");
		try {
			hello.header("If-Unmodified-Since", lastModified).delete();
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(412, e.getResponse().getStatus());
		}
		assertEquals("new world", hello.get(String.class));
	}

	public void testPUTContentType() throws Exception {
		WebResource hello = client.path("hello");
		hello.header("Content-Type", "text/world").put(String.class, "world");
		assertEquals("text/world", hello.head().getMetadata().getFirst("Content-Type"));
	}

	private ObjectRepository createRepository() throws StoreException,
			StoreConfigException {
		SailRepository repo = new SailRepository(new MemoryStore());
		repo.initialize();
		ObjectRepositoryFactory factory = new ObjectRepositoryFactory();
		return factory.createRepository(repo);
	}
}
