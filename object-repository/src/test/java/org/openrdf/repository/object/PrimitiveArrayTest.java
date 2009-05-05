package org.openrdf.repository.object;

import javax.interceptor.InvocationContext;

import junit.framework.Test;

import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.object.annotations.intercepts;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.base.ObjectRepositoryTestCase;

public class PrimitiveArrayTest extends ObjectRepositoryTestCase {

	public static Test suite() throws Exception {
		return ObjectRepositoryTestCase.suite(PrimitiveArrayTest.class);
	}

	public static class PrimitiveInterceptor1 {
		@intercepts
		public Object invoke(InvocationContext ctx) throws Exception {
			return ctx.proceed();
		}
	}

	public static class PrimitiveInterceptor2 {
		@intercepts
		public Object invoke(InvocationContext ctx) throws Exception {
			return ctx.proceed();
		}
	}

	@rdf("urn:PrimitiveBehaviour")
	public static interface PrimitiveBehaviour {
		public boolean[] getBoolean();

		public byte[] getByte();

		public char[] getChar();

		public double[] getDouble();

		public float[] getFloat();

		public int[] getInt();

		public short[] getShort();

		public String[] getString();

		public void setBoolean(boolean[] value);

		public void setByte(byte[] value);

		public void setChar(char[] value);

		public void setDouble(double[] value);

		public void setFloat(float[] value);

		public void setInt(int[] value);

		public void setShort(short[] value);

		public void setString(String[] value);
	}

	public static class PrimitiveBehaviourImpl1 implements PrimitiveBehaviour {
		private boolean[] booleanValue;

		private byte[] byteValue;

		private char[] charValue;

		private double[] doubleValue;

		private float[] floatValue;

		private int[] intValue;

		private short[] shortValue;

		private String[] stringValue;

		public boolean[] getBoolean() {
			if (count++ % 2 == 0) {
				return booleanValue;
			}
			return null;

		}

		public byte[] getByte() {
			if (count++ % 2 == 0) {
				return byteValue;
			}
			return null;
		}

		public char[] getChar() {
			if (count++ % 2 == 0) {
				return charValue;
			}
			return null;
		}

		public double[] getDouble() {
			if (count++ % 2 == 0) {
				return doubleValue;
			}
			return null;
		}

		public float[] getFloat() {
			if (count++ % 2 == 0) {
				return floatValue;
			}
			return null;
		}

		public int[] getInt() {
			if (count++ % 2 == 0) {
				return intValue;
			}
			return null;
		}

		public short[] getShort() {
			if (count++ % 2 == 0) {
				return shortValue;
			}
			return null;
		}

		public String[] getString() {
			if (count++ % 2 == 0) {
				return stringValue;
			}
			return null;
		}

		public void setBoolean(boolean[] value) {
			booleanValue = value;
		}

		public void setByte(byte[] value) {
			byteValue = value;
		}

		public void setChar(char[] value) {
			charValue = value;
		}

		public void setDouble(double[] value) {
			doubleValue = value;
		}

		public void setFloat(float[] value) {
			floatValue = value;
		}

		public void setInt(int[] value) {
			intValue = value;
		}

		public void setShort(short[] value) {
			shortValue = value;
		}

		public void setString(String[] value) {
			stringValue = value;
		}

	}

	public static class PrimitiveBehaviourImpl2 implements PrimitiveBehaviour {
		private boolean[] booleanValue;

		private byte[] byteValue;

		private char[] charValue;

		private double[] doubleValue;

		private float[] floatValue;

		private int[] intValue;

		private short[] shortValue;

		private String[] stringValue;

		public boolean[] getBoolean() {
			if (count++ % 2 == 0) {
				return booleanValue;
			}
			return null;

		}

		public byte[] getByte() {
			if (count++ % 2 == 0) {
				return byteValue;
			}
			return null;
		}

		public char[] getChar() {
			if (count++ % 2 == 0) {
				return charValue;
			}
			return null;
		}

		public double[] getDouble() {
			if (count++ % 2 == 0) {
				return doubleValue;
			}
			return null;
		}

		public float[] getFloat() {
			if (count++ % 2 == 0) {
				return floatValue;
			}
			return null;
		}

		public int[] getInt() {
			if (count++ % 2 == 0) {
				return intValue;
			}
			return null;
		}

		public short[] getShort() {
			if (count++ % 2 == 0) {
				return shortValue;
			}
			return null;
		}

		public String[] getString() {
			if (count++ % 2 == 0) {
				return stringValue;
			}
			return null;
		}

		public void setBoolean(boolean[] value) {
			booleanValue = value;
		}

		public void setByte(byte[] value) {
			byteValue = value;
		}

		public void setChar(char[] value) {
			charValue = value;
		}

		public void setDouble(double[] value) {
			doubleValue = value;
		}

		public void setFloat(float[] value) {
			floatValue = value;
		}

		public void setInt(int[] value) {
			intValue = value;
		}

		public void setShort(short[] value) {
			shortValue = value;
		}

		public void setString(String[] value) {
			stringValue = value;
		}

	}

	@rdf("urn:Primitive")
	public static interface PrimitiveConcept {
		@rdf("urn:boolean")
		public boolean[] getBoolean();

		@rdf("urn:byte")
		public byte[] getByte();

		@rdf("urn:char")
		public char[] getChar();

		@rdf("urn:double")
		public double[] getDouble();

		@rdf("urn:float")
		public float[] getFloat();

		@rdf("urn:int")
		public int[] getInt();

		@rdf("urn:short")
		public short[] getShort();

		@rdf("urn:string")
		public String[] getString();

		public void setBoolean(boolean[] value);

		public void setByte(byte[] value);

		public void setChar(char[] value);

		public void setDouble(double[] value);

		public void setFloat(float[] value);

		public void setInt(int[] value);

		public void setShort(short[] value);

		public void setString(String[] value);
	}

	@rdf("urn:PrimitiveClass")
	public static class PrimitiveConceptClass {
		@rdf("urn:boolean")
		private boolean bool[];

		@rdf("urn:byte")
		private byte b[];

		@rdf("urn:char")
		private char chr[];

		@rdf("urn:double")
		private double doub[];

		@rdf("urn:float")
		private float flo[];

		@rdf("urn:int")
		private int in[];

		@rdf("urn:short")
		private short shor[];

		@rdf("urn:string")
		private String string[];

		public boolean[] getBool() {
			return bool;
		}

		public void setBool(boolean[] bool) {
			this.bool = bool;
		}

		public byte[] getB() {
			return b;
		}

		public void setB(byte[] b) {
			this.b = b;
		}

		public char[] getChr() {
			return chr;
		}

		public void setChr(char[] chr) {
			this.chr = chr;
		}

		public double[] getDoub() {
			return doub;
		}

		public void setDoub(double[] doub) {
			this.doub = doub;
		}

		public float[] getFlo() {
			return flo;
		}

		public void setFlo(float[] flo) {
			this.flo = flo;
		}

		public int[] getIn() {
			return in;
		}

		public void setIn(int[] in) {
			this.in = in;
		}

		public short[] getShor() {
			return shor;
		}

		public void setShor(short[] shor) {
			this.shor = shor;
		}

		public String[] getString() {
			return string;
		}

		public void setString(String[] string) {
			this.string = string;
		}
	}

	private static final boolean[] booleanValue = new boolean[] { true };

	private static final byte[] byteValue = new byte[] { 1 };

	private static final char[] charValue = new char[] { '1' };

	static int count;

	private static final double[] doubleValue = new double[] { 1 };

	private static final float[] floatValue = new float[] { 1 };

	private static final int[] intValue = new int[] { 1 };

	private static final short[] shortValue = new short[] { 1 };

	private static final String[] stringValue = new String[] { "1" };

	private PrimitiveConcept concept;

	private PrimitiveConceptClass conceptClass;

	private PrimitiveBehaviour behaviour;

	@Override
	protected void setUp() throws Exception {
		config.addBehaviour(PrimitiveInterceptor1.class, new URIImpl("urn:Primitive"));
		config.addBehaviour(PrimitiveInterceptor2.class, new URIImpl("urn:PrimitiveBehaviour"));
		config.addConcept(PrimitiveConcept.class);
		config.addConcept(PrimitiveConceptClass.class);
		config.addConcept(PrimitiveBehaviour.class);
		config.addBehaviour(PrimitiveBehaviourImpl1.class);
		config.addBehaviour(PrimitiveBehaviourImpl2.class);
		super.setUp();
		conceptClass = con.addDesignation(con.getObjectFactory().createObject(), PrimitiveConceptClass.class);
		concept = con.addDesignation(con.getObjectFactory().createObject(), PrimitiveConcept.class);
		behaviour = con.addDesignation(con.getObjectFactory().createObject(), PrimitiveBehaviour.class);
	}

	public void testBoolean() {
		assertEquals(null, conceptClass.getBool());
		conceptClass.setBool(booleanValue);
		assertEquals(booleanValue[0], conceptClass.getBool()[0]);

		assertEquals(null, concept.getBoolean());
		concept.setBoolean(booleanValue);
		assertEquals(booleanValue[0], concept.getBoolean()[0]);

		assertEquals(null, behaviour.getBoolean());
		behaviour.setBoolean(booleanValue);
		assertEquals(booleanValue[0], behaviour.getBoolean()[0]);
	}

	public void testByte() {
		assertEquals(null, conceptClass.getB());
		conceptClass.setB(byteValue);
		assertEquals(byteValue[0], conceptClass.getB()[0]);

		assertEquals(null, concept.getByte());
		concept.setByte(byteValue);
		assertEquals(byteValue[0], concept.getByte()[0]);

		assertEquals(null, behaviour.getByte());
		behaviour.setByte(byteValue);
		assertEquals(byteValue[0], behaviour.getByte()[0]);
	}

	public void testChar() {
		assertEquals(null, conceptClass.getChr());
		conceptClass.setChr(charValue);
		assertEquals(charValue[0], conceptClass.getChr()[0]);

		assertEquals(null, concept.getChar());
		concept.setChar(charValue);
		assertEquals(charValue[0], concept.getChar()[0]);

		assertEquals(null, behaviour.getChar());
		behaviour.setChar(charValue);
		assertEquals(charValue[0], behaviour.getChar()[0]);
	}

	public void testDouble() {
		assertEquals(null, conceptClass.getDoub());
		conceptClass.setDoub(doubleValue);
		assertEquals(doubleValue[0], conceptClass.getDoub()[0]);

		assertEquals(null, concept.getDouble());
		concept.setDouble(doubleValue);
		assertEquals(doubleValue[0], concept.getDouble()[0]);

		assertEquals(null, behaviour.getDouble());
		behaviour.setDouble(doubleValue);
		assertEquals(doubleValue[0], behaviour.getDouble()[0]);
	}

	public void testFloat() {
		assertEquals(null, conceptClass.getFlo());
		conceptClass.setFlo(floatValue);
		assertEquals(floatValue[0], conceptClass.getFlo()[0]);

		assertEquals(null, concept.getFloat());
		concept.setFloat(floatValue);
		assertEquals(floatValue[0], concept.getFloat()[0]);

		assertEquals(null, behaviour.getFloat());
		behaviour.setFloat(floatValue);
		assertEquals(floatValue[0], behaviour.getFloat()[0]);
	}

	public void testInt() {
		assertEquals(null, conceptClass.getIn());
		conceptClass.setIn(intValue);
		assertEquals(intValue[0], conceptClass.getIn()[0]);

		assertEquals(null, concept.getInt());
		concept.setInt(intValue);
		assertEquals(intValue[0], concept.getInt()[0]);

		assertEquals(null, behaviour.getInt());
		behaviour.setInt(intValue);
		assertEquals(intValue[0], behaviour.getInt()[0]);
	}

	public void testShort() {
		assertEquals(null, conceptClass.getShor());
		conceptClass.setShor(shortValue);
		assertEquals(shortValue[0], conceptClass.getShor()[0]);

		assertEquals(null, concept.getShort());
		concept.setShort(shortValue);
		assertEquals(shortValue[0], concept.getShort()[0]);

		assertEquals(null, behaviour.getShort());
		behaviour.setShort(shortValue);
		assertEquals(shortValue[0], behaviour.getShort()[0]);
	}

	public void testString() {
		assertEquals(null, conceptClass.getString());
		conceptClass.setString(stringValue);
		assertEquals(stringValue[0], conceptClass.getString()[0]);

		assertEquals(null, concept.getString());
		concept.setString(stringValue);
		assertEquals(stringValue[0], concept.getString()[0]);

		assertEquals(null, behaviour.getString());
		behaviour.setString(stringValue);
		assertEquals(stringValue[0], behaviour.getString()[0]);
	}
}
