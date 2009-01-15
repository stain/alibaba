package org.openrdf.repository.object.concepts;

import java.util.Set;

import org.openrdf.repository.object.annotations.rdf;

/** A person. */
@rdf("http://xmlns.com/foaf/0.1/Person")
public interface Person extends Agent {


	/** A current project this person works on. */
	@rdf("http://xmlns.com/foaf/0.1/currentProject")
	public abstract Set<Object> getFoafCurrentProjects();

	/** A current project this person works on. */
	public abstract void setFoafCurrentProjects(Set<Object> value);


	/** The family_name of some person. */
	@rdf("http://xmlns.com/foaf/0.1/family_name")
	public abstract Set<Object> getFoafFamily_names();

	/** The family_name of some person. */
	public abstract void setFoafFamily_names(Set<Object> value);


	/** The first name of a person. */
	@rdf("http://xmlns.com/foaf/0.1/firstName")
	public abstract Set<Object> getFoafFirstNames();

	/** The first name of a person. */
	public abstract void setFoafFirstNames(Set<Object> value);


	/** A textual geekcode for this person, see http://www.geekcode.com/geek.html */
	@rdf("http://xmlns.com/foaf/0.1/geekcode")
	public abstract Set<Object> getFoafGeekcodes();

	/** A textual geekcode for this person, see http://www.geekcode.com/geek.html */
	public abstract void setFoafGeekcodes(Set<Object> value);


	/** A person known by this person (indicating some level of reciprocated interaction between the parties). */
	@rdf("http://xmlns.com/foaf/0.1/knows")
	public abstract Set<Person> getFoafKnows();

	/** A person known by this person (indicating some level of reciprocated interaction between the parties). */
	public abstract void setFoafKnows(Set<Person> value);


	/** A Myers Briggs (MBTI) personality classification. */
	@rdf("http://xmlns.com/foaf/0.1/myersBriggs")
	public abstract Set<Object> getFoafMyersBriggs();

	/** A Myers Briggs (MBTI) personality classification. */
	public abstract void setFoafMyersBriggs(Set<Object> value);


	/** A project this person has previously worked on. */
	@rdf("http://xmlns.com/foaf/0.1/pastProject")
	public abstract Set<Object> getFoafPastProjects();

	/** A project this person has previously worked on. */
	public abstract void setFoafPastProjects(Set<Object> value);


	/** A .plan comment, in the tradition of finger and '.plan' files. */
	@rdf("http://xmlns.com/foaf/0.1/plan")
	public abstract Set<Object> getFoafPlans();

	/** A .plan comment, in the tradition of finger and '.plan' files. */
	public abstract void setFoafPlans(Set<Object> value);


	/** The surname of some person. */
	@rdf("http://xmlns.com/foaf/0.1/surname")
	public abstract Set<Object> getFoafSurnames();

	/** The surname of some person. */
	public abstract void setFoafSurnames(Set<Object> value);


	/** A thing of interest to this person. */
	@rdf("http://xmlns.com/foaf/0.1/topic_interest")
	public abstract Set<Object> getFoafTopic_interests();

	/** A thing of interest to this person. */
	public abstract void setFoafTopic_interests(Set<Object> value);

}
