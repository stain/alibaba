package org.openrdf.alibaba.decor.helper;

import java.io.PrintStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;

public class RepositoryRecorder implements InvocationHandler {
	public static Repository wrap(Repository repository) {
		InvocationHandler handler = new RepositoryRecorder(repository);
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		Class<?>[] faces = new Class<?>[]{Repository.class};
		return (Repository) Proxy.newProxyInstance(cl, faces, handler);
	}

	private Repository repository;

	private PrintStream out = System.out;

	public RepositoryRecorder(Repository repository) {
		super();
		this.repository = repository;
	}

	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		out.print("repository.");
		out.print(method.getName());
		out.print("(");
		if (args != null) {
		for (int i=0; i<args.length; i++) {
			if (i > 0) {
				out.print(",");
			}
			out.print(args[i]);
		}
		}
		out.println(");");
		Object invoke = method.invoke(repository, args);
		if (invoke instanceof RepositoryConnection)
			return ConnectionRecorder.wrap((RepositoryConnection) invoke);
		return invoke;
	}

}
