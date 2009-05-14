package org.openrdf.repository.object.config;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;

public class ObjectRepositorySchema {

	/**
	 * The ObjectRepository schema namespace (
	 * <tt>http://www.openrdf.org/config/repository/object#</tt>).
	 */
	public static final String NAMESPACE = "http://www.openrdf.org/config/repository/object#";

	/** <tt>http://www.openrdf.org/config/repository/object#baseClass</tt> */
	public final static URI BASE_CLASS;

	/** <tt>http://www.openrdf.org/config/repository/object#packgaePrefix</tt> */
	public final static URI PACKAGE_PREFIX;

	/** <tt>http://www.openrdf.org/config/repository/object#memberPrefix</tt> */
	public final static URI MEMBER_PREFIX;

	/** <tt>http://www.openrdf.org/config/repository/object#datatype</tt> */
	public final static URI DATATYPE;

	/** <tt>http://www.openrdf.org/config/repository/object#concept</tt> */
	public final static URI CONCEPT;

	/** <tt>http://www.openrdf.org/config/repository/object#behaviour</tt> */
	public final static URI BEHAVIOUR;

	/** <tt>http://www.openrdf.org/config/repository/object#knownAs</tt> */
	public final static URI KNOWN_AS;

	/** <tt>http://www.openrdf.org/config/repository/object#conceptJar</tt> */
	public final static URI CONCEPT_JAR;

	/** <tt>http://www.openrdf.org/config/repository/object#behaviourJar</tt> */
	public final static URI BEHAVIOUR_JAR;

	/** <tt>http://www.openrdf.org/config/repository/object#importJars</tt> */
	public final static URI IMPORT_JARS;

	/** <tt>http://www.openrdf.org/config/repository/object#imports</tt> */
	public final static URI IMPORTS;

	/** <tt>http://www.openrdf.org/config/repository/object#followImports</tt> */
	public final static URI FOLLOW_IMPORTS;

	static {
		ValueFactory vf = ValueFactoryImpl.getInstance();
		BASE_CLASS = vf.createURI(NAMESPACE, "baseClass");
		PACKAGE_PREFIX = vf.createURI(NAMESPACE, "packgaePrefix");
		MEMBER_PREFIX = vf.createURI(NAMESPACE, "memberPrefix");
		DATATYPE = vf.createURI(NAMESPACE, "datatype");
		CONCEPT = vf.createURI(NAMESPACE, "concept");
		BEHAVIOUR = vf.createURI(NAMESPACE, "behaviour");
		KNOWN_AS = vf.createURI(NAMESPACE, "knownAs");
		CONCEPT_JAR = vf.createURI(NAMESPACE, "conceptJar");
		BEHAVIOUR_JAR = vf.createURI(NAMESPACE, "behaviourJar");
		IMPORT_JARS = vf.createURI(NAMESPACE, "importJarOntologies");
		IMPORTS = vf.createURI(NAMESPACE, "imports");
		FOLLOW_IMPORTS = vf.createURI(NAMESPACE, "followImports");
	}
}
