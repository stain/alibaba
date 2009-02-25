package org.openrdf.repository.object;

import javax.interceptor.InvocationContext;

import junit.framework.Test;

import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.object.annotations.intercepts;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.base.ElmoManagerTestCase;

public class PrimitiveTest extends ElmoManagerTestCase {

	public static Test suite() throws Exception {
		return ElmoManagerTestCase.suite(PrimitiveTest.class);
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
		public boolean isBoolean();

		public byte getByte();

		public char getChar();

		public double getDouble();

		public float getFloat();

		public int getInt();

		public short getShort();

		public String getString();

		public void setBoolean(boolean value);

		public void setByte(byte value);

		public void setChar(char value);

		public void setDouble(double value);

		public void setFloat(float value);

		public void setInt(int value);

		public void setShort(short value);

		public void setString(String value);
	}

	public static class PrimitiveBehaviourImpl1 implements PrimitiveBehaviour {

		private boolean booleanValue;

		private byte byteValue;

		private char charValue;

		private double doubleValue;

		private float floatValue;

		private int intValue;

		private short shortValue;

		private String stringValue;

		public boolean isBoolean() {
			if (count++ % 2 == 0) {
				return booleanValue;
			}
			return false;
		}

		public byte getByte() {
			if (count++ % 2 == 0) {
				return byteValue;
			}
			return 0;
		}

		public char getChar() {
			if (count++ % 2 == 0) {
				return charValue;
			}
			return 0;
		}

		public double getDouble() {
			if (count++ % 2 == 0) {
				return doubleValue;
			}
			return 0;
		}

		public float getFloat() {
			if (count++ % 2 == 0) {
				return floatValue;
			}
			return 0;
		}

		public int getInt() {
			if (count++ % 2 == 0) {
				return intValue;
			}
			return 0;
		}

		public short getShort() {
			if (count++ % 2 == 0) {
				return shortValue;
			}
			return 0;
		}

		public String getString() {
			if (count++ % 2 == 0) {
				return stringValue;
			}
			return null;
		}

		public void setBoolean(boolean value) {
			booleanValue = value;
		}

		public void setByte(byte value) {
			byteValue = value;
		}

		public void setChar(char value) {
			charValue = value;
		}

		public void setDouble(double value) {
			doubleValue = value;
		}

		public void setFloat(float value) {
			floatValue = value;
		}

		public void setInt(int value) {
			intValue = value;
		}

		public void setShort(short value) {
			shortValue = value;
		}

		public void setString(String value) {
			stringValue = value;
		}

	}

	public static class PrimitiveBehaviourImpl2 implements PrimitiveBehaviour {

		private boolean booleanValue;

		private byte byteValue;

		private char charValue;

		private double doubleValue;

		private float floatValue;

		private int intValue;

		private short shortValue;

		private String stringValue;

		public boolean isBoolean() {
			if (count++ % 2 == 0) {
				return booleanValue;
			}
			return false;
		}

		public byte getByte() {
			if (count++ % 2 == 0) {
				return byteValue;
			}
			return 0;
		}

		public char getChar() {
			if (count++ % 2 == 0) {
				return charValue;
			}
			return 0;
		}

		public double getDouble() {
			if (count++ % 2 == 0) {
				return doubleValue;
			}
			return 0;
		}

		public float getFloat() {
			if (count++ % 2 == 0) {
				return floatValue;
			}
			return 0;
		}

		public int getInt() {
			if (count++ % 2 == 0) {
				return intValue;
			}
			return 0;
		}

		public short getShort() {
			if (count++ % 2 == 0) {
				return shortValue;
			}
			return 0;
		}

		public String getString() {
			if (count++ % 2 == 0) {
				return stringValue;
			}
			return null;
		}

		public void setBoolean(boolean value) {
			booleanValue = value;
		}

		public void setByte(byte value) {
			byteValue = value;
		}

		public void setChar(char value) {
			charValue = value;
		}

		public void setDouble(double value) {
			doubleValue = value;
		}

		public void setFloat(float value) {
			floatValue = value;
		}

		public void setInt(int value) {
			intValue = value;
		}

		public void setShort(short value) {
			shortValue = value;
		}

		public void setString(String value) {
			stringValue = value;
		}

	}

	@rdf("urn:Primitive")
	public static interface PrimitiveConcept {
		@rdf("urn:boolean")
		public boolean isBoolean();

		@rdf("urn:byte")
		public byte getByte();

		@rdf("urn:char")
		public char getChar();

		@rdf("urn:double")
		public double getDouble();

		@rdf("urn:float")
		public float getFloat();

		@rdf("urn:int")
		public int getInt();

		@rdf("urn:short")
		public short getShort();

		@rdf("urn:string")
		public String getString();

		public void setBoolean(boolean value);

		public void setByte(byte value);

		public void setChar(char value);

		public void setDouble(double value);

		public void setFloat(float value);

		public void setInt(int value);

		public void setShort(short value);

		public void setString(String value);
	}

	@rdf("urn:PrimitiveClass")
	public static class PrimitiveConceptClass {
		@rdf("urn:boolean")
		private boolean bool;

		@rdf("urn:byte")
		private byte b;

		@rdf("urn:char")
		private char chr;

		@rdf("urn:double")
		private double doub;

		@rdf("urn:float")
		private float flo;

		@rdf("urn:int")
		private int in;

		@rdf("urn:short")
		private short shor;

		@rdf("urn:string")
		private String string;

		public boolean isBool() {
			return bool;
		}

		public void setBool(boolean bool) {
			this.bool = bool;
		}

		public byte getB() {
			return b;
		}

		public void setB(byte b) {
			this.b = b;
		}

		public char getChr() {
			return chr;
		}

		public void setChr(char chr) {
			this.chr = chr;
		}

		public double getDoub() {
			return doub;
		}

		public void setDoub(double doub) {
			this.doub = doub;
		}

		public float getFlo() {
			return flo;
		}

		public void setFlo(float flo) {
			this.flo = flo;
		}

		public int getIn() {
			return in;
		}

		public void setIn(int in) {
			this.in = in;
		}

		public short getShor() {
			return shor;
		}

		public void setShor(short shor) {
			this.shor = shor;
		}

		public String getString() {
			return string;
		}

		public void setString(String string) {
			this.string = string;
		}
	}

	private static final boolean booleanValue = true;

	private static final byte byteValue = 1;

	private static final char charValue = '1';

	static int count;

	private static final double doubleValue = 1;

	private static final float floatValue = 1;

	private static final int intValue = 1;

	private static final short shortValue = 1;

	private static final String stringValue = "1";

	private PrimitiveConcept concept;

	private PrimitiveConceptClass conceptClass;

	private PrimitiveBehaviour behaviour;

	@Override
	protected void setUp() throws Exception {
		module.addBehaviour(PrimitiveInterceptor1.class, new URIImpl("urn:Primitive"));
		module.addBehaviour(PrimitiveInterceptor2.class, new URIImpl("urn:PrimitiveBehaviour"));
		module.addConcept(PrimitiveConcept.class);
		module.addConcept(PrimitiveConceptClass.class);
		module.addConcept(PrimitiveBehaviour.class);
		module.addBehaviour(PrimitiveBehaviourImpl1.class);
		module.addBehaviour(PrimitiveBehaviourImpl2.class);
		super.setUp();
		conceptClass = manager.addType(manager.getObjectFactory().createBlankObject(), PrimitiveConceptClass.class);
		concept = manager.addType(manager.getObjectFactory().createBlankObject(), PrimitiveConcept.class);
		behaviour = manager.addType(manager.getObjectFactory().createBlankObject(), PrimitiveBehaviour.class);
	}

	public void testBoolean() {
		assertEquals(false, conceptClass.isBool());
		conceptClass.setBool(booleanValue);
		assertEquals(booleanValue, conceptClass.isBool());

		assertEquals(false, concept.isBoolean());
		concept.setBoolean(booleanValue);
		assertEquals(booleanValue, concept.isBoolean());

		assertEquals(false, behaviour.isBoolean());
		behaviour.setBoolean(booleanValue);
		assertEquals(booleanValue, behaviour.isBoolean());
	}

	public void testByte() {
		assertEquals(0, conceptClass.getB());
		conceptClass.setB(byteValue);
		assertEquals(byteValue, conceptClass.getB());

		assertEquals(0, concept.getByte());
		concept.setByte(byteValue);
		assertEquals(byteValue, concept.getByte());

		assertEquals(0, behaviour.getByte());
		behaviour.setByte(byteValue);
		assertEquals(byteValue, behaviour.getByte());
	}

	public void testChar() {
		assertEquals(0, conceptClass.getChr());
		conceptClass.setChr(charValue);
		assertEquals(charValue, conceptClass.getChr());

		assertEquals(0, concept.getChar());
		concept.setChar(charValue);
		assertEquals(charValue, concept.getChar());

		assertEquals(0, behaviour.getChar());
		behaviour.setChar(charValue);
		assertEquals(charValue, behaviour.getChar());
	}

	public void testDouble() {
		assertEquals(0.0, conceptClass.getDoub());
		conceptClass.setDoub(doubleValue);
		assertEquals(doubleValue, conceptClass.getDoub());

		assertEquals(0.0, concept.getDouble());
		concept.setDouble(doubleValue);
		assertEquals(doubleValue, concept.getDouble());

		assertEquals(0.0, behaviour.getDouble());
		behaviour.setDouble(doubleValue);
		assertEquals(doubleValue, behaviour.getDouble());
	}

	public void testFloat() {
		assertEquals(0.0f, conceptClass.getFlo());
		conceptClass.setFlo(floatValue);
		assertEquals(floatValue, conceptClass.getFlo());

		assertEquals(0.0f, concept.getFloat());
		concept.setFloat(floatValue);
		assertEquals(floatValue, concept.getFloat());

		assertEquals(0.0f, behaviour.getFloat());
		behaviour.setFloat(floatValue);
		assertEquals(floatValue, behaviour.getFloat());
	}

	public void testInt() {
		assertEquals(0, conceptClass.getIn());
		conceptClass.setIn(intValue);
		assertEquals(intValue, conceptClass.getIn());

		assertEquals(0, concept.getInt());
		concept.setInt(intValue);
		assertEquals(intValue, concept.getInt());

		assertEquals(0, behaviour.getInt());
		behaviour.setInt(intValue);
		assertEquals(intValue, behaviour.getInt());
	}

	public void testShort() {
		assertEquals(0, conceptClass.getShor());
		conceptClass.setShor(shortValue);
		assertEquals(shortValue, conceptClass.getShor());

		assertEquals(0, concept.getShort());
		concept.setShort(shortValue);
		assertEquals(shortValue, concept.getShort());

		assertEquals(0, behaviour.getShort());
		behaviour.setShort(shortValue);
		assertEquals(shortValue, behaviour.getShort());
	}

	public void testString() {
		assertEquals(null, conceptClass.getString());
		conceptClass.setString(stringValue);
		assertEquals(stringValue, conceptClass.getString());

		assertEquals(null, concept.getString());
		concept.setString(stringValue);
		assertEquals(stringValue, concept.getString());

		assertEquals(null, behaviour.getString());
		behaviour.setString(stringValue);
		assertEquals(stringValue, behaviour.getString());
	}
}
