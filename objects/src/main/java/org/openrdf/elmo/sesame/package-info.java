/**
 * Implements the Elmo API for the Sesame RDF Repository.
 * 
 * The Class SesameManagerFactory assembles the services found in this
 * package and in org.openrdf.elmo. It creates the ContextAwareConnection
 * used by SesameManager and intializes it services.
 * <p>
 * SesameManager creates the new JavaBean instances, using
 * SesameResourceManager to read and write the subject types to the
 * repository. Then using RoleMapperImpl to map them to Java roles, which
 * are composed together by ElmoClassCompositor.
 * <p>
 * The MapperClassFactory scans the roles before composition looking for rdf
 * annotations and generates the byte-code for triple mapping. These
 * generated classes will use a SesamePropertyFactory for each Bean property
 * to create a new instance of SesameProperty for every bean instance.
 * <p>
 * The SesameProperty reads the property values using SesameManager to
 * convert the rdf value into a literal via SesameLiteralManager or creates
 * a Bean. The value is written to the repository by PropertyChanger.
 * 
 */
package org.openrdf.elmo.sesame;