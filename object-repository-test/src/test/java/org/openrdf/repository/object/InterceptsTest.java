package org.openrdf.repository.object;

import java.lang.reflect.Method;

import javax.interceptor.InvocationContext;

import junit.framework.Test;

import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.object.annotations.intercepts;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.base.ElmoManagerTestCase;

public class InterceptsTest extends ElmoManagerTestCase {
	private static final String NS = "http://www.example.com/rdf/2007/";

	public static Test suite() throws Exception {
		return ElmoManagerTestCase.suite(InterceptsTest.class);
	}

	@rdf(NS + "Role1")
	public static interface Concept1 {
		@rdf(NS + "p1")
		public String getP1();

		public void setP1(String value);

		@rdf(NS + "p1")
		public String getP2();
	}

	public static class Interceptor1 {
		@intercepts(method=".*")
		public Object intercept(InvocationContext ctx) throws Exception {
			Object result = ctx.proceed();
			if (result instanceof String)
				return result.toString() + " Interceptor1";
			return result;
		}
	}

	@rdf(NS + "Role2")
	public static interface Concept2 {
		@rdf(NS + "p1")
		public String getP1();

		public void setP1(String value);

		@rdf(NS + "p1")
		public String getP2();
	}

	public static class Interceptor2 {
		@intercepts(method = "getP1")
		public Object intercept(InvocationContext ctx) throws Exception {
			Object result = ctx.proceed();
			if (result instanceof String)
				return result.toString() + " Interceptor2";
			return result;
		}
	}

	@rdf(NS + "Role3")
	public static interface Concept3 {
		@rdf(NS + "p1")
		public Integer getInteger();

		public void setInteger(Integer value);

		@rdf(NS + "p2")
		public Number getNumber();

		public void setNumber(Number value);
	}

	public static class Interceptor3 {
		@intercepts(method=".*", parameters = { Number.class })
		public void interceptNumber(InvocationContext ctx) throws Exception {
			Number result = (Number) ctx.getParameters()[0];
			ctx.setParameters(new Object[] { new Integer(result.intValue() + 3) });
			ctx.proceed();
		}
		@intercepts(method=".*", parameters = { Integer.class })
		public void interceptInteger(InvocationContext ctx) throws Exception {
			Integer result = (Integer) ctx.getParameters()[0];
			ctx.setParameters(new Object[] { new Integer(result.intValue() + 3) });
			ctx.proceed();
		}
	}

	@rdf(NS + "Role4")
	public static interface Concept4 {
		@rdf(NS + "p1")
		public Integer getInteger();

		public void setInteger(Integer value);

		@rdf(NS + "p1")
		public Number getNumber();
	}

	public static class Interceptor4 {
		@intercepts(method=".*", returns = Integer.class)
		public Number intercept(InvocationContext ctx) throws Exception {
			Number result = (Number) ctx.proceed();
			return new Integer(result.intValue() + 3);
		}
	}

	@rdf(NS + "Role5")
	public static interface Concept5A {
		@rdf(NS + "p1")
		public String getP1();

		public void setP1(String value);
	}

	@rdf(NS + "Role5")
	public static interface Concept5B extends Concept5A {
		@rdf(NS + "p1")
		public String getP2();

		public void setP2(String value);
	}

	public static class Interceptor5 {
		@intercepts(method=".*", declaring = Concept5A.class)
		public Object intercept(InvocationContext ctx) throws Exception {
			Object result = ctx.proceed();
			if (result instanceof String)
				return result.toString() + " Interceptor5";
			return result;
		}
	}

	@rdf(NS + "Role6")
	public static interface Concept6 {
		@rdf(NS + "p1")
		public String getP1();

		public void setP1(String value);

		@rdf(NS + "p1")
		public String getP2();
	}

	public static class Interceptor6 {
		public static boolean interceptCondition(Method method) {
			return method.getName().equals("getP1");
		}

		@intercepts(method=".*", conditional = "interceptCondition")
		public Object intercept(InvocationContext ctx) throws Exception {
			Object result = ctx.proceed();
			if (result instanceof String)
				return result.toString() + " Interceptor6";
			return result;
		}
	}

	public void testAny() throws Exception {
		Concept1 bean = manager.addType(manager.getObjectFactory().createBlankObject(), Concept1.class);
		bean.setP1("hello");
		assertEquals("hello Interceptor1", bean.getP1());
		assertEquals("hello Interceptor1", bean.getP2());
	}

	public void testByName() throws Exception {
		Concept2 bean = manager.addType(manager.getObjectFactory().createBlankObject(), Concept2.class);
		bean.setP1("hello");
		assertEquals("hello Interceptor2", bean.getP1());
		assertEquals("hello", bean.getP2());
	}

	public void testByParemeters() throws Exception {
		Concept3 bean = manager.addType(manager.getObjectFactory().createBlankObject(), Concept3.class);
		bean.setInteger(new Integer(5));
		bean.setNumber(new Integer(5));
		assertEquals(new Integer(8), bean.getNumber());
		assertEquals(new Integer(8), bean.getInteger());
	}

	public void testByReturnType() throws Exception {
		Concept4 bean = manager.addType(manager.getObjectFactory().createBlankObject(), Concept4.class);
		bean.setInteger(new Integer(5));
		assertEquals(new Integer(8), bean.getNumber());
		assertEquals(new Integer(8), bean.getInteger());
	}

	public void testByDeclaredIn() throws Exception {
		Concept5B bean = manager.addType(manager.getObjectFactory().createBlankObject(), Concept5B.class);
		bean.setP1("hello");
		assertEquals("hello Interceptor5", bean.getP1());
		assertEquals("hello", bean.getP2());
	}

	public void testByConditionMethod() throws Exception {
		Concept6 bean = manager.addType(manager.getObjectFactory().createBlankObject(), Concept6.class);
		bean.setP1("hello");
		assertEquals("hello Interceptor6", bean.getP1());
		assertEquals("hello", bean.getP2());
	}

	@Override
	protected void setUp() throws Exception {
		module.addConcept(Concept1.class);
		module.addBehaviour(Interceptor1.class, new URIImpl((NS + "Role1")));
		module.addConcept(Concept2.class);
		module.addBehaviour(Interceptor2.class, new URIImpl((NS + "Role2")));
		module.addConcept(Concept3.class);
		module.addBehaviour(Interceptor3.class, new URIImpl((NS + "Role3")));
		module.addConcept(Concept4.class);
		module.addBehaviour(Interceptor4.class, new URIImpl((NS + "Role4")));
		module.addConcept(Concept5A.class);
		module.addConcept(Concept5B.class);
		module.addBehaviour(Interceptor5.class, new URIImpl((NS + "Role5")));
		module.addConcept(Concept6.class);
		module.addBehaviour(Interceptor6.class, new URIImpl((NS + "Role6")));
		super.setUp();
	}
}
