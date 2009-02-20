/**
 * Creates Elmo concepts and OWL ontologies. The generated source code is
 * produced from the Groovy script: /templates/concept.groovy.
 * <p>
 * As can be seen from the script it loops through the properties and check
 * for potential annotations printing the Java source code into buffers. The
 * script is run once for each concept.
 * <p>
 * Prior to running the script, however, it normalizes the ontology. This
 * includes things like replacing all rdfs:Class with owl:Class, adding
 * missing rdf:type or datatypes, and renaming anonymous or foreign classes.
 * <p>
 * The primary interface is intendent to be run from the command line.
 */
package org.openrdf.elmo.codegen;