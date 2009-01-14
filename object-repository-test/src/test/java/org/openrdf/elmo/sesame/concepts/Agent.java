package org.openrdf.elmo.sesame.concepts;

import java.util.Set;

import org.openrdf.repository.object.annotations.rdf;

/** An agent (eg. person, group, software or physical artifact). */
@rdf("http://xmlns.com/foaf/0.1/Agent")
public interface Agent {


	/** An AIM chat ID */
	@rdf("http://xmlns.com/foaf/0.1/aimChatID")
	public abstract Set<Object> getFoafAimChatIDs();

	/** An AIM chat ID */
	public abstract void setFoafAimChatIDs(Set<Object> value);


	/** The  birthday of this Agent, represented in mm-dd string form, eg. '12-31'. */
	@rdf("http://xmlns.com/foaf/0.1/birthday")
	public abstract Object getFoafBirthday();

	/** The  birthday of this Agent, represented in mm-dd string form, eg. '12-31'. */
	public abstract void setFoafBirthday(Object value);


	/** An organization funding a project or person. */
	@rdf("http://xmlns.com/foaf/0.1/fundedBy")
	public abstract Set<Object> getFoafFundedBy();

	/** An organization funding a project or person. */
	public abstract void setFoafFundedBy(Set<Object> value);


	/** The gender of this Agent (typically but not necessarily 'male' or 'female'). */
	@rdf("http://xmlns.com/foaf/0.1/gender")
	public abstract Object getFoafGender();

	/** The gender of this Agent (typically but not necessarily 'male' or 'female'). */
	public abstract void setFoafGender(Object value);


	/** An ICQ chat ID */
	@rdf("http://xmlns.com/foaf/0.1/icqChatID")
	public abstract Set<Object> getFoafIcqChatIDs();

	/** An ICQ chat ID */
	public abstract void setFoafIcqChatIDs(Set<Object> value);


	/** A jabber ID for something. */
	@rdf("http://xmlns.com/foaf/0.1/jabberID")
	public abstract Set<Object> getFoafJabberIDs();

	/** A jabber ID for something. */
	public abstract void setFoafJabberIDs(Set<Object> value);


	/** A logo representing some thing. */
	@rdf("http://xmlns.com/foaf/0.1/logo")
	public abstract Set<Object> getFoafLogos();

	/** A logo representing some thing. */
	public abstract void setFoafLogos(Set<Object> value);


	/** Something that was made by this agent. */
	@rdf("http://xmlns.com/foaf/0.1/made")
	public abstract Set<Object> getFoafMades();

	/** Something that was made by this agent. */
	public abstract void setFoafMades(Set<Object> value);


	/** An agent that made this thing. */
	@rdf("http://xmlns.com/foaf/0.1/maker")
	public abstract Set<Agent> getFoafMakers();

	/** An agent that made this thing. */
	public abstract void setFoafMakers(Set<Agent> value);


	/** A personal mailbox, ie. an Internet mailbox associated with exactly one owner, the first owner of this mailbox. This is a 'static inverse functional property', in that  there is (across time and change) at most one individual that ever has any particular value for foaf:mbox. */
	@rdf("http://xmlns.com/foaf/0.1/mbox")
	public abstract Set<Object> getFoafMboxes();

	/** A personal mailbox, ie. an Internet mailbox associated with exactly one owner, the first owner of this mailbox. This is a 'static inverse functional property', in that  there is (across time and change) at most one individual that ever has any particular value for foaf:mbox. */
	public abstract void setFoafMboxes(Set<Object> value);


	/** The sha1sum of the URI of an Internet mailbox associated with exactly one owner, the  first owner of the mailbox. */
	@rdf("http://xmlns.com/foaf/0.1/mbox_sha1sum")
	public abstract Set<Object> getFoafMbox_sha1sums();

	/** The sha1sum of the URI of an Internet mailbox associated with exactly one owner, the  first owner of the mailbox. */
	public abstract void setFoafMbox_sha1sums(Set<Object> value);


	/** An MSN chat ID */
	@rdf("http://xmlns.com/foaf/0.1/msnChatID")
	public abstract Set<Object> getFoafMsnChatIDs();

	/** An MSN chat ID */
	public abstract void setFoafMsnChatIDs(Set<Object> value);


	/** A name for some thing. */
	@rdf("http://xmlns.com/foaf/0.1/name")
	public abstract Set<Object> getFoafNames();

	/** A name for some thing. */
	public abstract void setFoafNames(Set<Object> value);


	/** A theme. */
	@rdf("http://xmlns.com/foaf/0.1/theme")
	public abstract Set<Object> getFoafThemes();

	/** A theme. */
	public abstract void setFoafThemes(Set<Object> value);


	/** A Yahoo chat ID */
	@rdf("http://xmlns.com/foaf/0.1/yahooChatID")
	public abstract Set<Object> getFoafYahooChatIDs();

	/** A Yahoo chat ID */
	public abstract void setFoafYahooChatIDs(Set<Object> value);

}
