package org.openrdf.repository.object.concepts;

import java.util.Set;

import org.openrdf.repository.object.annotations.iri;

/** A person. */
@iri("urn:foaf:Person")
public interface Person extends Agent {


	/** A current project this person works on. */
	@iri("urn:foaf:currentProject")
	public abstract Set<Object> getFoafCurrentProjects();

	/** A current project this person works on. */
	public abstract void setFoafCurrentProjects(Set<Object> value);


	/** The family_name of some person. */
	@iri("urn:foaf:family_name")
	public abstract Set<Object> getFoafFamily_names();

	/** The family_name of some person. */
	public abstract void setFoafFamily_names(Set<Object> value);


	/** The first name of a person. */
	@iri("urn:foaf:firstName")
	public abstract Set<Object> getFoafFirstNames();

	/** The first name of a person. */
	public abstract void setFoafFirstNames(Set<Object> value);


	/** A textual geekcode for this person, see http://www.geekcode.com/geek.html */
	@iri("urn:foaf:geekcode")
	public abstract Set<Object> getFoafGeekcodes();

	/** A textual geekcode for this person, see http://www.geekcode.com/geek.html */
	public abstract void setFoafGeekcodes(Set<Object> value);


	/** A person known by this person (indicating some level of reciprocated interaction between the parties). */
	@iri("urn:foaf:knows")
	public abstract Set<Person> getFoafKnows();

	/** A person known by this person (indicating some level of reciprocated interaction between the parties). */
	public abstract void setFoafKnows(Set<Person> value);


	/** A Myers Briggs (MBTI) personality classification. */
	@iri("urn:foaf:myersBriggs")
	public abstract Set<Object> getFoafMyersBriggs();

	/** A Myers Briggs (MBTI) personality classification. */
	public abstract void setFoafMyersBriggs(Set<Object> value);


	/** A project this person has previously worked on. */
	@iri("urn:foaf:pastProject")
	public abstract Set<Object> getFoafPastProjects();

	/** A project this person has previously worked on. */
	public abstract void setFoafPastProjects(Set<Object> value);


	/** A .plan comment, in the tradition of finger and '.plan' files. */
	@iri("urn:foaf:plan")
	public abstract Set<Object> getFoafPlans();

	/** A .plan comment, in the tradition of finger and '.plan' files. */
	public abstract void setFoafPlans(Set<Object> value);


	/** The surname of some person. */
	@iri("urn:foaf:surname")
	public abstract Set<Object> getFoafSurnames();

	/** The surname of some person. */
	public abstract void setFoafSurnames(Set<Object> value);


	/** A thing of interest to this person. */
	@iri("urn:foaf:topic_interest")
	public abstract Set<Object> getFoafTopic_interests();

	/** A thing of interest to this person. */
	public abstract void setFoafTopic_interests(Set<Object> value);

}
