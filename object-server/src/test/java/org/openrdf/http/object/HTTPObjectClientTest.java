package org.openrdf.http.object;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHttpRequest;
import org.openrdf.http.object.annotations.cacheControl;
import org.openrdf.http.object.annotations.method;
import org.openrdf.http.object.base.MetadataServerTestCase;
import org.openrdf.http.object.client.HTTPObjectClient;
import org.openrdf.repository.object.annotations.matches;

public class HTTPObjectClientTest extends MetadataServerTestCase {

	@matches("/pipe")
	public static class Pipe {
		private AtomicInteger count = new AtomicInteger();

		@cacheControl("no-store")
		@method("GET")
		public int get() throws InterruptedException {
			count.incrementAndGet();
			System.out.println(count.get());
			Thread.sleep((int) (1000 * Math.random()));
			return count.decrementAndGet();
		}
	}

	public void setUp() throws Exception {
		config.addConcept(Pipe.class);
		super.setUp();
	}

	public void testNothing() {}

	public void notestHttpPipelining() throws Exception {
		HTTPObjectClient client = HTTPObjectClient.getInstance();
		InetSocketAddress addr = new InetSocketAddress("localhost", port);
		List<Future<HttpResponse>> list = new ArrayList<Future<HttpResponse>>(100);
		for (int i = 0; i < 10; i++) {
			HttpRequest request = new BasicHttpRequest("GET", "/pipe");
			request.setHeader("Host", "localhost:" + port);
			request.setHeader("Cache-Control", "no-store");
			list.add(client.submitRequest(addr, request));
		}
		int count = 0;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for (Future<HttpResponse> freq : list) {
			freq.get().getEntity().writeTo(baos);
			count += Integer.parseInt(baos.toString());
		}
		assertTrue(10 < count);
	}
}
